
package info.freelibrary.ark.verticles;

import java.util.UUID;

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

    /**
     * The property that determines what type of action is needed.
     */
    public static final String ACTION = "covenant.minting.action";

    /**
     * The logger for the minting verticle.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(NamespaceMintingVerticle.class, MessageCodes.BUNDLE);

    private final DB myDb;

    /**
     * Creates a new minting verticle.
     */
    public NamespaceMintingVerticle() {
        myDb = DBMaker.fileDB("/tmp/test-" + UUID.randomUUID().toString() + "-.db").transactionEnable().make();
    }

    @Override
    public void start(final Promise<Void> aPromise) {
        final EventBus eventBus;

        try {
            super.start();

            // Get the Vert.x event bus
            eventBus = vertx.eventBus();

            // Register the codec we'll use for sharing minters
            eventBus.registerDefaultCodec(NoidMinter.class, new SerializableCodec<>(NoidMinter.class));

            // Receive minting messages
            eventBus.<NoidMinter>consumer(NamespaceMintingVerticle.class.getName(), request -> {
                final NoidMinter minter = request.body();

                try {
                    switch (request.headers().get(NamespaceMintingVerticle.ACTION)) {
                        case Op.MINT_NOID_NAMESPACE:
                            mintNoidNamespace(minter, request);
                            break;
                        default:
                            request.reply(new JsonObject());
                    }
                } catch (final DBException details) {
                    LOGGER.error(details, details.getMessage());
                    request.fail(500, details.getMessage());
                }

                System.out.println("received " + minter.getNamespace());
            });

            aPromise.complete();
        } catch (final Exception details) {
            aPromise.fail(details);
        }
    }

    private void mintNoidNamespace(final NoidMinter aMinter, final Message<NoidMinter> aRequest) throws DBException {
        final String collection = aMinter.getNamespace();

        if (!myDb.exists(collection)) {
            final Stopwatch timer = new Stopwatch().start();
            final DB.TreeMapSink<String, String> dbSink =
                    myDb.treeMap(collection, Serializer.STRING_ASCII, Serializer.STRING).createFromSink();

            while (aMinter.hasNext()) {
                final String noid = aMinter.next();
                // LOGGER.debug(noid);
                dbSink.put(noid, "");
            }

            final BTreeMap<String, String> btree = dbSink.create();

            LOGGER.debug("Db size: {} [{}]", btree.sizeLong(), timer.stop().getSeconds());
            aRequest.reply("SUCCESS");
        } else {
            LOGGER.debug("db already exists");
            aRequest.fail(500, "Noid namespace already exists");
        }

    }
}
