
package info.freelibrary.ark.verticles;

import info.freelibrary.ark.Config;
import info.freelibrary.ark.MessageCodes;
import info.freelibrary.ark.Op;
import info.freelibrary.ark.handlers.GetPidNamespaceHandler;
import info.freelibrary.ark.handlers.MintPidHandler;
import info.freelibrary.ark.handlers.MintPidNamespaceHandler;
import info.freelibrary.ark.handlers.PageHandler;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.warnings.PMD;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.ThreadingModel;
import io.vertx.core.VerticleBase;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.openapi.router.RequestExtractor;
import io.vertx.ext.web.openapi.router.RouterBuilder;
import io.vertx.openapi.contract.OpenAPIContract;
import org.jetbrains.annotations.NotNull;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Main verticle that starts the application.
 */
@SuppressWarnings({ PMD.EXCESSIVE_IMPORTS })
public class MainVerticle extends VerticleBase {

    /** The API specification for the application. */
    private static final String API_SPEC = "covenant.yaml";

    /** The name of the database file. */
    private static final String DB_FILE_NAME = "covenant.db";

    /** The default port for the server. */
    private static final int DEFAULT_PORT = 8888;

    /** The logger for the main verticle. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class, MessageCodes.BUNDLE);

    /** The application's Web server. */
    private HttpServer myServer;

    /** The application's database. */
    private DB myDB;

    /** The application's temporary database directory. */
    private String myTempDbDir;

    @Override
    @NotNull
    public Future<Void> start() {
        final Promise<Void> promise = Promise.promise();

        // We pull our application's configuration before configuring the server
        ConfigRetriever.create(vertx).getConfig()
                .onSuccess(config -> configureServer(config.mergeIn(config()), promise)).onFailure(promise::fail);

        return promise.future();
    }

    @Override
    @NotNull
    public Future<Void> stop() {
        final List<Future<?>> futures = new ArrayList<>();

        if (myDB != null) {
            futures.add(vertx.<Void>executeBlocking(() -> {
                try {
                    myDB.close();
                } catch (@SuppressWarnings({ PMD.AVOID_CATCHING_GENERIC_EXCEPTION }) final Exception details) {
                    LOGGER.error(MessageCodes.ARK_035, details);
                }

                // We are just closing the DB, no result needed
                return null;
            }));
        }

        if (myServer != null) {
            futures.add(myServer.close());
        }

        // Always attempt temporary directory cleanup, even if a shutdown step failed
        return futures.isEmpty() ? Future.succeededFuture()
                : Future.all(futures).mapEmpty().otherwiseEmpty().compose(_ -> cleanUp());
    }

    /**
     * Deletes the temporary database's directory if one was created.
     *
     * @return A future that completes when the temporary directory has been deleted
     */
    private Future<Void> cleanUp() {
        if (myTempDbDir == null) {
            return Future.succeededFuture();
        }

        return vertx.fileSystem().deleteRecursive(myTempDbDir)
                .onSuccess(_ -> LOGGER.warn(MessageCodes.ARK_034, myTempDbDir)).otherwiseEmpty();
    }

    /**
     * Checks if a resource exists.
     *
     * @param aResource The resource to check
     * @return True if the resource exists, false otherwise
     */
    private boolean resourceExists(final @NotNull String aResource) {
        return Thread.currentThread().getContextClassLoader().getResource(aResource) != null;
    }

    /**
     * Gets the API specification.
     *
     * @return The API specification
     */
    @NotNull
    private String getApiSpec() {
        return resourceExists(API_SPEC) ? API_SPEC : Path.of("src/main/resources", API_SPEC).toString();
    }

    /**
     * Configure the Covenant server.
     *
     * @param aConfig A JSON configuration
     * @param aPromise A startup promise
     */
    private void configureServer(@NotNull final JsonObject aConfig, @NotNull final Promise<Void> aPromise) {
        OpenAPIContract.from(vertx, getApiSpec()).compose(contract -> {
            final RouterBuilder builder = RouterBuilder.create(vertx, contract, RequestExtractor.withBodyHandler());
            final Router router;

            // Set a body handler so form POSTs will work; this also requires the request extractor above
            builder.rootHandler(BodyHandler.create());

            // Associate handlers with OpenAPI operation IDs
            builder.getRoute(Op.MINT_ARK_NAMESPACE)
                    .addHandler(new MintPidNamespaceHandler(vertx, Op.MINT_ARK_NAMESPACE));
            builder.getRoute(Op.MINT_ARK).addHandler(new MintPidHandler(vertx, Op.MINT_ARK));

            builder.getRoute(Op.MINT_NOID_NAMESPACE)
                    .addHandler(new MintPidNamespaceHandler(vertx, Op.MINT_NOID_NAMESPACE));
            builder.getRoute(Op.MINT_NOID).addHandler(new MintPidHandler(vertx, Op.MINT_NOID));

            builder.getRoute(Op.GET_ARK_NAMESPACES)
                    .addHandler(new GetPidNamespaceHandler(vertx, Op.GET_ARK_NAMESPACES));
            builder.getRoute(Op.GET_NOID_NAMESPACES)
                    .addHandler(new GetPidNamespaceHandler(vertx, Op.GET_NOID_NAMESPACES));

            // Build the router from the OpenAPI contract
            router = builder.createRouter();

            // Set up page handlers
            router.get("/admin").handler(new PageHandler());
            router.get("/").handler(new PageHandler());

            return Future.succeededFuture(router);
        }).onSuccess(router -> {
            final Integer port = aConfig.getInteger(Config.HTTP_PORT, DEFAULT_PORT);

            if (!aConfig.containsKey(Config.HTTP_PORT)) {
                LOGGER.warn(MessageCodes.ARK_036, port);
            }

            myServer = vertx.createHttpServer().requestHandler(router);
            myServer.listen(port).onComplete(new StartupHandler(aConfig, aPromise));
        }).onFailure(aPromise::fail);
    }

    /**
     * A handler for a newly started server.
     */
    private final class StartupHandler implements Handler<AsyncResult<HttpServer>> {

        /** A promise that the application startup will happen. */
        private final Promise<Void> myPromise;

        /** The merged application configuration. */
        private final JsonObject myConfig;

        /**
         * Creates a new startup handler.
         *
         * @param aConfig The merged application configuration
         * @param aPromise A startup promise
         */
        private StartupHandler(final @NotNull JsonObject aConfig, final @NotNull Promise<Void> aPromise) {
            myPromise = aPromise;
            myConfig = aConfig;
        }

        @Override
        public void handle(@NotNull final AsyncResult<HttpServer> aStartup) {
            if (aStartup.succeeded()) {
                vertx.<DB>executeBlocking(this::initializeDB).compose(db -> {
                    final DeploymentOptions nsMintingOpts = new DeploymentOptions().setConfig(myConfig);
                    final NamespaceMintingVerticle nsMintingVerticle = new NamespaceMintingVerticle(db);

                    nsMintingOpts.setThreadingModel(ThreadingModel.WORKER)
                            .setWorkerPoolName(NamespaceMintingVerticle.class.getName()).setWorkerPoolSize(1)
                            .setMaxWorkerExecuteTime(10).setMaxWorkerExecuteTimeUnit(TimeUnit.MINUTES);

                    LOGGER.info(MessageCodes.ARK_007, myServer.actualPort());
                    return vertx.deployVerticle(nsMintingVerticle, nsMintingOpts);
                }).onSuccess(_ -> myPromise.complete()).onFailure(myPromise::fail);
            } else {
                myPromise.fail(aStartup.cause());
            }
        }

        /**
         * Initializes the embedded database. If a `Config.DB_FILES_DIR` is configured, we use that directory;
         * otherwise, we use a randomized temporary directory.
         *
         * <p>
         * This method is blocking, so should only be run inside Vert.x's `executeBlocking()` function or a worker
         * verticle.
         * </p>
         *
         * @return A database of ID relationships
         * @throws java.io.IOException If the database cannot be initialized
         */
        @NotNull
        private DB initializeDB() throws IOException {
            final String dbDirConfig = myConfig.getString(Config.DB_FILES_DIR);
            final String dbDir = dbDirConfig == null ? Files.createTempDirectory("covenant-").toString() : dbDirConfig;

            if (dbDirConfig == null) {
                myTempDbDir = dbDir;
                LOGGER.warn(MessageCodes.ARK_033, myTempDbDir);
            }

            myDB = DBMaker.fileDB(Path.of(dbDir, DB_FILE_NAME).toString()).transactionEnable().make();
            return myDB;
        }
    }
}
