
package info.freelibrary.ark.handlers;

import info.freelibrary.ark.AbstractTest;
import info.freelibrary.ark.Config;
import info.freelibrary.ark.HTTP;
import info.freelibrary.ark.MessageCodes;
import info.freelibrary.ark.Namespace;
import info.freelibrary.ark.NoidType;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import io.vertx.core.MultiMap;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.client.WebClient;
import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.Test;

/**
 * A test of the mint NOID namespace handler.
 */
public class MintPidNamespaceHandlerTest extends AbstractTest {

    /** The logger for the test. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MintPidNamespaceHandler.class, MessageCodes.BUNDLE);

    /** The endpoint for minting a new NOID namespace. */
    private static final String MIND_NOID_NS_PATH = "/mint/noid/namespace";

    /**
     * Tests minting a new NOID namespace.
     *
     * @param aContext A test context
     */
    @Test(timeout = Long.MAX_VALUE)
    @Ignore
    public void testMintingNoidNamespace(@NotNull final TestContext aContext) {
        final WebClient client = WebClient.create(myTestContext.vertx());
        final MultiMap form = MultiMap.caseInsensitiveMultiMap();
        final int port = aContext.get(Config.HTTP_PORT);
        final Async asyncTask = aContext.async();

        form.set(Namespace.NAME, "test-namespace");
        form.set(Namespace.LENGTH, "5");
        form.set(Namespace.SHOULDER, "f3");
        form.set(Namespace.CHECKSUMS, "true");
        form.set(Namespace.NOID_TYPE, NoidType.ALPHANUMERIC.name());

        client.post(port, HOST, MIND_NOID_NS_PATH).sendForm(form).onFailure(aContext::fail).onSuccess(response -> {
            aContext.assertEquals(HTTP.CREATED, response.statusCode());
            complete(asyncTask);
        });
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
