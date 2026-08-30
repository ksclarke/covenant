
package info.freelibrary.ark.verticles;

import info.freelibrary.ark.NoidMinter;
import info.freelibrary.ark.util.MessageCodes;
import info.freelibrary.ark.util.Op;
import info.freelibrary.ark.util.SerializableCodec;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.Stopwatch;
import info.freelibrary.util.warnings.JDK;
import info.freelibrary.util.warnings.PMD;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.VerticleBase;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBException;
import org.mapdb.Serializer;
import org.mapdb.serializer.GroupSerializer;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A verticle that mints new NOIDs, ARKs, and their namespaces.
 */
public class NamespaceMintingVerticle extends VerticleBase {

    /** The property that determines what type of action is needed. */
    public static final String ACTION = "covenant.minting.action";

    /** The logger for the minting verticle. */
    private static final Logger LOGGER = LoggerFactory.getLogger(NamespaceMintingVerticle.class, MessageCodes.BUNDLE);

    /** The map name in which minters are stored. */
    private static final String MINTERS = "minters";

    /** The database for storing minters. */
    private final DB myDatabase;

    /** The number of requests currently being processed. */
    private final AtomicInteger myInFlightCounter = new AtomicInteger();

    /** The map of minters. */
    private BTreeMap<String, NoidMinter> myMinters;

    /**
     * Creates a new minting verticle.
     *
     * @param aDatabase The database for storing minters
     */
    public NamespaceMintingVerticle(final DB aDatabase) {
        myDatabase = aDatabase;
    }

    @Override
    public Future<Void> start() {
        final Promise<Void> promise = Promise.promise();
        final EventBus eventBus;

        @SuppressWarnings(JDK.UNCHECKED)
        final GroupSerializer<NoidMinter> serializer = (GroupSerializer<NoidMinter>) Serializer.JAVA;

        try {
            super.start();

            // Open the map of minters
            myMinters = myDatabase.treeMap(MINTERS, Serializer.STRING, serializer).createOrOpen();

            // Get the Vert.x event bus and register the codec we'll use for sharing minters
            eventBus = vertx.eventBus();
            eventBus.registerDefaultCodec(NoidMinter.class, new SerializableCodec<>(NoidMinter.class));

            // Receive minting messages
            eventBus.<NoidMinter>consumer(getClass().getName(), request -> {
                myInFlightCounter.incrementAndGet();

                try (NoidMinter minter = request.body()) {
                    final String action = request.headers().get(ACTION);

                    // Add minter to the database if it's new
                    if (myMinters.putIfAbsent(minter.getNamespace(), minter) == null) {
                        myDatabase.commit();
                    }

                    if (Op.MINT_NOID_NAMESPACE.equals(action)) {
                        mintNoidNamespace(minter, request);
                    } else {
                        request.reply(new JsonObject());
                    }
                } catch (final DBException | IOException details) {
                    LOGGER.error(details, details.getMessage());
                    request.fail(500, details.getMessage());
                } finally {
                    myInFlightCounter.decrementAndGet();
                }
            });

            promise.complete();
        } catch (@SuppressWarnings(PMD.AVOID_CATCHING_GENERIC_EXCEPTION) final Exception details) {
            promise.fail(details);
        }

        return promise.future();
    }

    /**
     * Waits for any in-flight requests to finish before undeploying, so the parent verticle doesn't close the database
     * while a request is still using it.
     *
     * @return A future that completes once all in-flight requests have finished
     */
    @Override
    public Future<Void> stop() {
        final Promise<Void> promise = Promise.promise();

        if (myInFlightCounter.get() == 0) {
            promise.complete();
        } else {
            vertx.setPeriodic(100, id -> {
                if (myInFlightCounter.get() == 0) {
                    vertx.cancelTimer(id);
                    promise.tryComplete();
                }
            });
        }

        return promise.future();
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

        if (!myDatabase.exists(collection)) {
            final Stopwatch timer = new Stopwatch().start();

            try (BTreeMap<String, String> map =
                    myDatabase.treeMap(collection, Serializer.STRING, Serializer.STRING).createOrOpen()) {
                myDatabase.commit();
                LOGGER.debug(MessageCodes.ARK_032, map.sizeLong(), timer.stop().getSeconds().trim());

                LOGGER.debug("DB exists: " + myDatabase.exists(collection));
            }

            aRequest.reply(aMinter);
        } else {
            aRequest.fail(500, LOGGER.getMessage(MessageCodes.ARK_031, collection));
        }
    }
}
