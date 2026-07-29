
package info.freelibrary.ark.handlers;

import info.freelibrary.ark.MessageCodes;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

/**
 * A handler that accepts requests to mint ARKs.
 */
public class MintArkHandler implements Handler<RoutingContext> {

    /** The logger for the MintArkHandler. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MintArkHandler.class, MessageCodes.BUNDLE);

    /** The handler's copy of the Vert.x instance. */
    private final Vertx myVertx;

    /**
     * Creates a handler that mints ARKs.
     *
     * @param aVertx A Vert.x instance
     */
    public MintArkHandler(final Vertx aVertx) {
        myVertx = aVertx;
    }

    @Override
    public void handle(final RoutingContext aContext) {
        final MultiMap params = aContext.queryParams();

        LOGGER.debug(params.get("noid"));
        myVertx.eventBus().send("placeholder", params);
    }

}
