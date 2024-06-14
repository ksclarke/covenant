
package info.freelibrary.ark.verticles;

import java.util.concurrent.TimeUnit;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import info.freelibrary.ark.Config;
import info.freelibrary.ark.MessageCodes;
import info.freelibrary.ark.Op;
import info.freelibrary.ark.handlers.MintArkHandler;
import info.freelibrary.ark.handlers.MintArkNamespaceHandler;
import info.freelibrary.ark.handlers.MintNoidHandler;
import info.freelibrary.ark.handlers.MintNoidNamespaceHandler;
import info.freelibrary.ark.handlers.PageHandler;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;

/**
 * Main verticle that starts the application.
 */
public class MainVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class, MessageCodes.BUNDLE);

    private static final String API_SPEC = "src/main/resources/covenant.yaml";

    private HttpServer myServer;

    /**
     * Starts a Web server.
     */
    @Override
    public void start(final Promise<Void> aPromise) {
        final ConfigRetriever configRetriever = ConfigRetriever.create(vertx);

        // We pull our application's configuration before configuring the server
        configRetriever.getConfig(configuration -> {
            if (configuration.failed()) {
                aPromise.fail(configuration.cause());
            } else {
                // We merge in any config properties that have been set for testing
                configureServer(configuration.result().mergeIn(config()), aPromise);
            }
        });
    }

    @Override
    public void stop(final Promise<Void> aPromise) {
        myServer.close(close -> {
            if (close.succeeded()) {
                aPromise.complete();
            } else {
                aPromise.fail(close.cause());
            }
        });
    }

    /**
     * Configure the endpoints that are defined in the OpenAPI specification.
     *
     * @param aRouterFactory A factory for router creation
     * @return A router configured with the API endpoints
     */
    private Router configureApiEndpoints(final OpenAPI3RouterFactory aRouterFactory) {
        aRouterFactory.addHandlerByOperationId(Op.MINT_ARK_NAMESPACE, new MintArkNamespaceHandler(vertx));
        aRouterFactory.addHandlerByOperationId(Op.MINT_NOID_NAMESPACE, new MintNoidNamespaceHandler(vertx));
        aRouterFactory.addHandlerByOperationId(Op.MINT_NOID, new MintNoidHandler(vertx));
        aRouterFactory.addHandlerByOperationId(Op.MINT_ARK, new MintArkHandler(vertx));

        return aRouterFactory.getRouter();
    }

    /**
     * Configure the Covenant server.
     *
     * @param aConfig A JSON configuration
     * @param aPromise A startup promise
     */
    private void configureServer(final JsonObject aConfig, final Promise<Void> aPromise) {
        final int port = aConfig.getInteger(Config.HTTP_PORT);

        // Create the endpoints defined in the OpenAPI specification file
        OpenAPI3RouterFactory.create(vertx, API_SPEC, creation -> {
            if (creation.succeeded()) {
                final Router router = configureApiEndpoints(creation.result());

                // Set up page handlers
                router.get("/admin").handler(new PageHandler());
                router.get("/").handler(new PageHandler());

                // Start the Covenant server
                myServer = vertx.createHttpServer().requestHandler(router);
                myServer.listen(port, new StartupHandler(port, aPromise));
            } else {
                aPromise.fail(creation.cause());
            }
        });
    }

    /**
     * A handler for a newly started server.
     */
    private final class StartupHandler implements Handler<AsyncResult<HttpServer>> {

        /**
         * A promise that the application startup will happen.
         */
        private final Promise<Void> myPromise;

        /**
         * The port at which the server should be started.
         */
        private final int myPort;

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
        public void handle(final AsyncResult<HttpServer> aStartup) {
            if (aStartup.succeeded()) {
                final DeploymentOptions nsMintingOpts = new DeploymentOptions().setConfig(config());
                final String nsMintingVerticleName = NamespaceMintingVerticle.class.getName();

                nsMintingOpts.setWorker(true).setWorkerPoolName(nsMintingVerticleName).setWorkerPoolSize(1);
                nsMintingOpts.setMaxWorkerExecuteTime(10).setMaxWorkerExecuteTimeUnit(TimeUnit.MINUTES);

                LOGGER.info(MessageCodes.ARK_007, myPort);

                // If the server startup succeeds, deploy verticles into the server
                vertx.deployVerticle(nsMintingVerticleName, nsMintingOpts, deployment -> {
                    if (deployment.succeeded()) {
                        myPromise.complete();
                    } else {
                        myPromise.fail(deployment.cause());
                    }
                });
            } else {
                myPromise.fail(aStartup.cause());
            }
        }

    }
}
