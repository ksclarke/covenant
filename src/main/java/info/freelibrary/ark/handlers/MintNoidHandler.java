
package info.freelibrary.ark.handlers;

import info.freelibrary.ark.MessageCodes;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import org.jetbrains.annotations.NotNull;

/**
 * A handler that accepts requests to mint a new NOID.
 */
public class MintNoidHandler implements Handler<RoutingContext> {

    /** The logger for the MintNoidHandler. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MintNoidHandler.class, MessageCodes.BUNDLE);

    /** The handler's copy of the Vert.x instance. */
    private final Vertx myVertx;

    /**
     * Creates a new handler for requests to mint NOIDs.
     *
     * @param aVertx A Vert.x instance
     */
    public MintNoidHandler(final Vertx aVertx) {
        myVertx = aVertx;
    }

    @Override
    public void handle(@NotNull final RoutingContext aContext) {
        final MultiMap params = aContext.queryParams();

        LOGGER.debug(params.get("noid"));
        myVertx.eventBus().send("placeholder", params);
    }

}
