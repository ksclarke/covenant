
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

import java.util.concurrent.TimeUnit;

/**
 * Main verticle that starts the application.
 */
public class MainVerticle extends VerticleBase {

    /** The API specification for the application. */
    private static final String API_SPEC = "src/main/resources/covenant.yaml";

    /** The logger for the main verticle. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class, MessageCodes.BUNDLE);

    /** The application's Web server. */
    private HttpServer myServer;

    @Override
    public Future<Void> start() {
        final ConfigRetriever configRetriever = ConfigRetriever.create(vertx);
        final Promise<Void> promise = Promise.promise();

        // We pull our application's configuration before configuring the server
        configRetriever.getConfig().onSuccess(config -> configureServer(config.mergeIn(config()), promise))
                .onFailure(promise::fail);

        return promise.future();
    }

    @Override
    public Future<?> stop() {
        return myServer.close();
    }

    /**
     * Configure the Covenant server.
     *
     * @param aConfig A JSON configuration
     * @param aPromise A startup promise
     */
    private void configureServer(@NotNull final JsonObject aConfig, @NotNull final Promise<Void> aPromise) {
        final int port = aConfig.getInteger(Config.HTTP_PORT);

        OpenAPIContract.from(vertx, API_SPEC).compose(contract -> {
            final RequestExtractor requestExtractor = RequestExtractor.withBodyHandler();
            final RouterBuilder routerBuilder = RouterBuilder.create(vertx, contract, requestExtractor);
            final Router router;

            // Set a body handler so form POSTs will work; this also requires the request extractor above
            routerBuilder.rootHandler(BodyHandler.create());

            // Associate handlers with OpenAPI operation IDs
            routerBuilder.getRoute(Op.MINT_ARK_NAMESPACE)
                    .addHandler(new MintPidNamespaceHandler(vertx, Op.MINT_ARK_NAMESPACE));
            routerBuilder.getRoute(Op.MINT_ARK).addHandler(new MintPidHandler(vertx, Op.MINT_ARK));

            routerBuilder.getRoute(Op.MINT_NOID_NAMESPACE)
                    .addHandler(new MintPidNamespaceHandler(vertx, Op.MINT_NOID_NAMESPACE));
            routerBuilder.getRoute(Op.MINT_NOID).addHandler(new MintPidHandler(vertx, Op.MINT_NOID));

            routerBuilder.getRoute(Op.GET_ARK_NAMESPACES)
                    .addHandler(new GetPidNamespaceHandler(vertx, Op.GET_ARK_NAMESPACES));
            routerBuilder.getRoute(Op.GET_NOID_NAMESPACES)
                    .addHandler(new GetPidNamespaceHandler(vertx, Op.GET_NOID_NAMESPACES));

            // Build the router from the OpenAPI contract
            router = routerBuilder.createRouter();

            // Set up page handlers
            router.get("/admin").handler(new PageHandler());
            router.get("/").handler(new PageHandler());

            return Future.succeededFuture(router);
        }).onSuccess(router -> {
            myServer = vertx.createHttpServer().requestHandler(router);
            myServer.listen(port).onComplete(new StartupHandler(port, aPromise));
        }).onFailure(aPromise::fail);
    }

    /**
     * A handler for a newly started server.
     */
    private final class StartupHandler implements Handler<AsyncResult<HttpServer>> {

        /** The port at which the server should be started. */
        private final int myPort;

        /** A promise that the application startup will happen. */
        private final Promise<Void> myPromise;

        /**
         * Creates a new startup handler.
         *
         * @param aPort A port
         * @param aPromise A startup promise
         */
        private StartupHandler(final int aPort, final Promise<Void> aPromise) {
            myPromise = aPromise;
            myPort = aPort;
        }

        @Override
        public void handle(@NotNull final AsyncResult<HttpServer> aStartup) {
            if (aStartup.succeeded()) {
                final DeploymentOptions nsMintingOpts = new DeploymentOptions().setConfig(config());
                final String nsMintingVerticleName = NamespaceMintingVerticle.class.getName();

                nsMintingOpts.setThreadingModel(ThreadingModel.WORKER).setWorkerPoolName(nsMintingVerticleName)
                        .setWorkerPoolSize(1).setMaxWorkerExecuteTime(10).setMaxWorkerExecuteTimeUnit(TimeUnit.MINUTES);

                LOGGER.info(MessageCodes.ARK_007, myPort);

                // If the server startup succeeds, deploy verticles into the server
                vertx.deployVerticle(nsMintingVerticleName, nsMintingOpts).onSuccess(_ -> myPromise.complete())
                        .onFailure(myPromise::fail);
            } else {
                myPromise.fail(aStartup.cause());
            }
        }
    }
}
