
package info.freelibrary.ark.handlers;

import info.freelibrary.ark.util.MessageCodes;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

/**
 * A handler that accepts requests to mint a new NOID.
 */
public class MintPidHandler implements Handler<RoutingContext> {

    /** The logger for the MintPidHandler. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MintPidHandler.class, MessageCodes.BUNDLE);

    /** The handler's copy of the Vert.x instance. */
    private final Vertx myVertx;

    /** The OpenAPI operation ID. */
    private final String myOpID;

    /**
     * Creates a new handler for requests to mint NOIDs.
     *
     * @param aVertx A Vert.x instance
     * @param anOpID The OpenAPI operation ID
     */
    public MintPidHandler(final Vertx aVertx, final String anOpID) {
        myVertx = aVertx;
        myOpID = anOpID;
    }

    @Override
    public void handle(final RoutingContext aContext) {
        final MultiMap params = aContext.queryParams();

        LOGGER.debug(params.get("noid") + " " + myOpID);
        myVertx.eventBus().send("placeholder", params);
    }

}
