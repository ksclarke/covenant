
package info.freelibrary.ark.handlers;

import info.freelibrary.ark.util.MessageCodes;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler that responds to requests to list PID namespaces.
 */
public class GetPidNamespaceHandler implements Handler<RoutingContext> {

    /** The logger for the GetPidNamespaceHandler. */
    private static final Logger LOGGER = LoggerFactory.getLogger(GetPidNamespaceHandler.class, MessageCodes.BUNDLE);

    /** The handler's copy of the Vert.x instance. */
    private final Vertx myVertx;

    /** The OpenAPI operation ID. */
    private final String myOpID;

    /**
     * Creates a new GetPidNamespaceHandler.
     *
     * @param aVertx The Vert.x instance
     * @param anOpID The OpenAPI operation ID
     */
    public GetPidNamespaceHandler(final Vertx aVertx, final String anOpID) {
        myVertx = aVertx;
        myOpID = anOpID;
    }

    @Override
    public void handle(final RoutingContext aContext) {
        LOGGER.debug(myVertx.toString(), myOpID);
    }
}
