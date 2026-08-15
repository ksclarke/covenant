
package info.freelibrary.ark;

import info.freelibrary.ark.verticles.MainVerticle;
import info.freelibrary.util.Logger;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * An abstract base class for tests.
 */
@RunWith(VertxUnitRunner.class)
public abstract class AbstractTest {

    /** The host used for testing. */
    protected static final String HOST = "0.0.0.0";

    /** A view into the test that's being run. */
    @Rule
    public TestName myTestName = new TestName();

    /** The test context, from which the Vert.x instance can be retrieved. */
    @Rule
    public RunTestOnContext myTestContext = new RunTestOnContext();

    /**
     * Set up our testing environment.
     *
     * @param aContext A test context
     */
    @Before
    public void setUp(@NotNull final TestContext aContext) throws IOException {
        final DeploymentOptions options = new DeploymentOptions();
        final Async asyncTask = aContext.async();
        final int port = getAvailablePort();

        aContext.put(Config.HTTP_PORT, port);
        options.setConfig(new JsonObject().put(Config.HTTP_PORT, port));

        myTestContext.vertx().deployVerticle(MainVerticle.class.getName(), options).onFailure(aContext::fail)
                .onSuccess(_ -> {
                    try {
                        InetAddress.getByName(HOST).isReachable(port);
                    } catch (final IOException details) {
                        getLogger().warn(MessageCodes.ARK_016, port);
                    }

                    asyncTask.complete();
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

        myTestContext.vertx().close().onSuccess(_ -> {
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
    protected void complete(@NotNull final Async aAsyncTask) {
        if (!aAsyncTask.isCompleted()) {
            aAsyncTask.complete();
        }
    }

    /**
     * Gets an available port.
     *
     * @return An available port
     * @throws IOException If there isn't an available port
     */
    private int getAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
