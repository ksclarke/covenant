
package info.freelibrary.ark.verticles;

import static info.freelibrary.util.Constants.INADDR_ANY;
import static info.freelibrary.util.Constants.SLASH;
import static org.junit.jupiter.api.Assertions.assertEquals;

import info.freelibrary.ark.AbstractTest;
import info.freelibrary.ark.HTTP;
import info.freelibrary.ark.MessageCodes;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxTestContext;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

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
    public void testThatTheServerIsStarted(final @NotNull VertxTestContext aContext) {
        final WebClient client = WebClient.create(myVertx);

        client.get(myPort, INADDR_ANY, SLASH).send().onFailure(aContext::failNow).onSuccess(response -> {
            aContext.verify(() -> {
                assertEquals(HTTP.OK, response.statusCode());
                complete(aContext);
            });
        });
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
