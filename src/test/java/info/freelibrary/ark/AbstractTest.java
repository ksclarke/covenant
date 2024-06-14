
package info.freelibrary.ark;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import info.freelibrary.util.Logger;

import info.freelibrary.ark.verticles.MainVerticle;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 * An abstract base class for tests.
 */
@RunWith(VertxUnitRunner.class)
public abstract class AbstractTest {

    /**
     * The host used for testing.
     */
    protected static final String HOST = "0.0.0.0";

    /**
     * A view into the test that's being run.
     */
    @Rule
    public TestName myTestName = new TestName();

    /**
     * The test context, from which the Vert.x instance can be retrieved
     */
    @Rule
    public RunTestOnContext myTestContext = new RunTestOnContext();

    /**
     * Set up our testing environment.
     *
     * @param aContext A test context
     */
    @Before
    public void setUp(final TestContext aContext) throws IOException {
        final DeploymentOptions options = new DeploymentOptions();
        final Async asyncTask = aContext.async();
        final int port = getAvailablePort();

        aContext.put(Config.HTTP_PORT, port);
        options.setConfig(new JsonObject().put(Config.HTTP_PORT, port));

        myTestContext.vertx().deployVerticle(MainVerticle.class.getName(), options, deployment -> {
            if (deployment.succeeded()) {
                try (Socket socket = new Socket(HOST, port)) {
                    // This is expected... do nothing
                } catch (final IOException details) {
                    // Not necessarily an error, but a higher probability of one
                    getLogger().warn(MessageCodes.ARK_016, port);
                }

                asyncTask.complete();
            } else {
                aContext.fail(deployment.cause());
            }
        });
    }

    /**
     * Cleans up after the test.
     *
     * @param aContext A test context
     */
    @After
    public void tearDown(final TestContext aContext) {
        final Async asyncTask = aContext.async();
        final int port = aContext.get(Config.HTTP_PORT);

        myTestContext.vertx().close(shutdown -> {
            getLogger().debug(MessageCodes.ARK_015, port);
            asyncTask.complete();
        });
    }

    /**
     * The logger used in testing.
     *
     * @return A test logger
     */
    protected abstract Logger getLogger();

    /**
     * Completes an Async task, if needed.
     *
     * @param aAsyncTask An asynchronous task
     */
    protected void complete(final Async aAsyncTask) {
        if (!aAsyncTask.isCompleted()) {
            aAsyncTask.complete();
        }
    }

    /**
     * Gets an available port.
     *
     * @return An available port
     */
    private int getAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
