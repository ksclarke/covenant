
package info.freelibrary.ark.verticles;

import java.util.UUID;

import info.freelibrary.util.warnings.PMD;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBException;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.Stopwatch;

import info.freelibrary.ark.MessageCodes;
import info.freelibrary.ark.Op;
import info.freelibrary.ark.utils.NoidMinter;
import info.freelibrary.ark.utils.SerializableCodec;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

/**
 * A verticle that mints new NOIDs, ARKs, and their namespaces.
 */
public class NamespaceMintingVerticle extends AbstractVerticle {

    /** The property that determines what type of action is needed. */
    public static final String ACTION = "covenant.minting.action";

    /** The logger for the minting verticle. */
    private static final Logger LOGGER = LoggerFactory.getLogger(NamespaceMintingVerticle.class, MessageCodes.BUNDLE);

    /** The database for storing minters. */
    private final DB myDb;

    /**
     * Creates a new minting verticle.
     */
    public NamespaceMintingVerticle() {
        myDb = DBMaker.fileDB("/tmp/test-" + UUID.randomUUID().toString() + "-.db").transactionEnable().make();
    }

    @Override
    @SuppressWarnings(PMD.AVOID_CATCHING_GENERIC_EXCEPTION) // That's what Vert.x throws
    public void start(final Promise<Void> aPromise) {
        final EventBus eventBus;

        try {
            super.start();

            // Get the Vert.x event bus and register the codec we'll use for sharing minters
            eventBus = vertx.eventBus();
            eventBus.registerDefaultCodec(NoidMinter.class, new SerializableCodec<>(NoidMinter.class));

            // Receive minting messages
            eventBus.<NoidMinter>consumer(getClass().getName(), request -> {
                final NoidMinter minter = request.body();

                try {
                    final String action = request.headers().get(ACTION);

                    if (Op.MINT_NOID_NAMESPACE.equals(action)) {
                        mintNoidNamespace(minter, request);
                    } else {
                        request.reply(new JsonObject());
                    }
                } catch (final DBException details) {
                    LOGGER.error(details, details.getMessage());
                    request.fail(500, details.getMessage());
                }

                LOGGER.info("Received minter namespace: " + minter.getNamespace());
            });

            aPromise.complete();
        } catch (final Exception details) {
            aPromise.fail(details);
        }
    }

    /**
     * Mints a Noid namespace.
     *
     * @param aMinter The Noid minter
     * @param aRequest The request
     * @throws DBException If there's a problem with the database
     */
    private void mintNoidNamespace(final NoidMinter aMinter, final Message<NoidMinter> aRequest) {
        final String collection = aMinter.getNamespace();

        if (!myDb.exists(collection)) {
            final Stopwatch timer = new Stopwatch().start();
            final DB.TreeMapSink<String, String> dbSink =
                    myDb.treeMap(collection, Serializer.STRING_ASCII, Serializer.STRING).createFromSink();

            while (aMinter.hasNext()) {
                final String noid = aMinter.next();
                dbSink.put(noid, "");
            }

            try (BTreeMap<String, String> btree = dbSink.create()) {
                LOGGER.debug("Db size: {} [{}]", btree.sizeLong(), timer.stop().getSeconds());
            }

            aRequest.reply("SUCCESS");
        } else {
            LOGGER.debug("db already exists");
            aRequest.fail(500, "Noid namespace already exists");
        }

    }
}
