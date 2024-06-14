
package info.freelibrary.ark.verticles;

import org.junit.Test;
import org.junit.runner.RunWith;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import info.freelibrary.ark.AbstractTest;
import info.freelibrary.ark.Config;
import info.freelibrary.ark.Constants;
import info.freelibrary.ark.HTTP;
import info.freelibrary.ark.MessageCodes;

import io.vertx.core.buffer.Buffer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

/**
 * Tests the main verticle of the covenant application.
 */
@RunWith(VertxUnitRunner.class)
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
    public void testThatTheServerIsStarted(final TestContext aContext) {
        final WebClient client = WebClient.create(myTestContext.vertx());
        final int port = aContext.get(Config.HTTP_PORT);
        final Async asyncTask = aContext.async();

        client.get(port, HOST, Constants.SLASH).send(get -> {
            if (get.succeeded()) {
                final HttpResponse<Buffer> response = get.result();

                aContext.assertEquals(HTTP.OK, response.statusCode());
                complete(asyncTask);
            } else {
                aContext.fail(get.cause());
            }
        });
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
