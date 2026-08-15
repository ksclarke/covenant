
package info.freelibrary.ark.verticles;

import static info.freelibrary.util.Constants.SLASH;

import info.freelibrary.ark.AbstractTest;
import info.freelibrary.ark.Config;
import info.freelibrary.ark.HTTP;
import info.freelibrary.ark.MessageCodes;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.client.WebClient;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

/**
 * Tests the main verticle of the covenant application.
 */
public class MainVerticleTest extends AbstractTest {

    /**
     * The logger used in testing.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticleTest.class, MessageCodes.BUNDLE);

    /**
     * Tests the server can start successfully.
     *
     * @param aContext A test context
     */
    @Test
    public void testThatTheServerIsStarted(@NotNull final TestContext aContext) {
        final WebClient client = WebClient.create(myTestContext.vertx());
        final int port = aContext.get(Config.HTTP_PORT);
        final Async asyncTask = aContext.async();

        client.get(port, HOST, SLASH).send().onFailure(aContext::fail).onSuccess(response -> {
            aContext.assertEquals(HTTP.OK, response.statusCode());
            complete(asyncTask);
        });
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
