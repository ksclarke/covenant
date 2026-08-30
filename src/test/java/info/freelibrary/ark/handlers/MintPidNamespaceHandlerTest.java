
package info.freelibrary.ark.handlers;

import static info.freelibrary.util.Constants.INADDR_ANY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import info.freelibrary.ark.AbstractTest;
import info.freelibrary.ark.Namespace;
import info.freelibrary.ark.NoidType;
import info.freelibrary.ark.util.MessageCodes;
import info.freelibrary.util.HTTP;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;

/**
 * A test of the mint NOID namespace handler.
 */
public class MintPidNamespaceHandlerTest extends AbstractTest {

    /** The logger for the test. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MintNamespaceHandler.class, MessageCodes.BUNDLE);

    /** The endpoint for minting a new NOID namespace. */
    private static final String MIND_NOID_NS_PATH = "/mint/noid/namespace";

    /**
     * Tests minting a new NOID namespace.
     *
     * @param aContext A test context
     */
    @Test
    public void testMintingNoidNamespace(final VertxTestContext aContext) {
        final WebClient client = WebClient.create(myVertx);
        final MultiMap form = MultiMap.caseInsensitiveMultiMap();

        form.set(Namespace.NAME, "test-namespace");
        form.set(Namespace.LENGTH, "5");
        form.set(Namespace.SHOULDER, "f3");
        form.set(Namespace.CHECKSUMS, "true");
        form.set(Namespace.NOID_TYPE, NoidType.ALPHANUMERIC.name());

        client.post(myPort, INADDR_ANY, MIND_NOID_NS_PATH).sendForm(form).onFailure(aContext::failNow)
                .onSuccess(response -> {
                    aContext.verify(() -> {
                        assertEquals(HTTP.CREATED, response.statusCode());
                        aContext.completeNow();
                    });
                });
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
