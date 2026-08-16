
package info.freelibrary.ark;

import static info.freelibrary.util.Constants.INADDR_ANY;
import static org.junit.jupiter.api.Assertions.assertTrue;

import info.freelibrary.ark.verticles.MainVerticle;
import info.freelibrary.util.Logger;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * An abstract base class for tests.
 */
@ExtendWith(VertxExtension.class)
public abstract class AbstractTest {

    /** A view into the test that's being run. */
    protected String myTestName;

    /** The test context, from which the Vert.x instance can be retrieved. */
    protected Vertx myVertx;

    /** The port used for testing. */
    protected int myPort;

    /**
     * Set up our testing environment.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     * @param aTestInfo Information about the test being run
     * @throws IOException If there is trouble getting an available port
     */
    @BeforeEach
    public void setUp(final @NotNull Vertx aVertx, final @NotNull VertxTestContext aContext,
            final @NotNull TestInfo aTestInfo) throws IOException {
        final DeploymentOptions options = new DeploymentOptions();

        myTestName = aTestInfo.getTestMethod().orElseThrow().getName();
        myPort = getAvailablePort();
        myVertx = aVertx;

        options.setConfig(new JsonObject().put(Config.HTTP_PORT, myPort));

        aVertx.deployVerticle(MainVerticle.class.getName(), options).onFailure(aContext::failNow).onSuccess(id -> {
            try {
                assertTrue(InetAddress.getByName(INADDR_ANY).isReachable(myPort));
            } catch (final IOException details) {
                getLogger().warn(MessageCodes.ARK_016, myPort);
            }

            aContext.completeNow();
        });
    }

    /**
     * Cleans up after the test.
     *
     * @param aContext A test context
     */
    @AfterEach
    public void tearDown(final @NotNull VertxTestContext aContext) {
        myVertx.close().onSuccess(_ -> {
            getLogger().debug(MessageCodes.ARK_015, myPort);
            aContext.completeNow();
        }).onFailure(aContext::failNow);
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
     * @param aContext A test context
     */
    protected void complete(@NotNull final VertxTestContext aContext) {
        if (!aContext.completed()) {
            aContext.completeNow();
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
