
package info.freelibrary.ark.handlers;

import org.junit.Test;
import org.junit.runner.RunWith;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import info.freelibrary.ark.AbstractTest;
import info.freelibrary.ark.Config;
import info.freelibrary.ark.HTTP;
import info.freelibrary.ark.MessageCodes;
import info.freelibrary.ark.Namespace;
import info.freelibrary.ark.NoidType;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

/**
 * A test of the mint NOID namespace handler.
 */
@RunWith(VertxUnitRunner.class)
public class MintNoidNamespaceHandlerTest extends AbstractTest {

    /**
     * The logger for the test.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MintNoidNamespaceHandler.class, MessageCodes.BUNDLE);

    /**
     * The endpoint for minting a new NOID namespace.
     */
    private static final String PATH = "/mint/noid/namespace";

    /**
     * Tests minting a new NOID namespace.
     *
     * @param aContext A test context
     */
    @Test(timeout = Long.MAX_VALUE)
    public void testMintingNoidNamespace(final TestContext aContext) {
        final WebClient client = WebClient.create(myTestContext.vertx());
        final MultiMap form = MultiMap.caseInsensitiveMultiMap();
        final int port = aContext.get(Config.HTTP_PORT);
        final Async asyncTask = aContext.async();

        form.set(Namespace.NAME, "test-namespace");
        form.set(Namespace.LENGTH, "5");
        form.set(Namespace.SHOULDER, "f3");
        form.set(Namespace.CHECKSUMS, "true");
        form.set(Namespace.NOID_TYPE, NoidType.ALPHANUMERIC.name());

        client.post(port, HOST, PATH).sendForm(form, submission -> {
            if (submission.succeeded()) {
                final HttpResponse<Buffer> response = submission.result();

                aContext.assertEquals(HTTP.CREATED, response.statusCode());
                complete(asyncTask);
            } else {
                aContext.fail(submission.cause());
            }
        });
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
