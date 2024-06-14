
package info.freelibrary.ark.handlers;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

/**
 * A handler that accepts requests to mint a new NOID.
 */
public class MintNoidHandler implements Handler<RoutingContext> {

    /* The handler's copy of the Vert.x instance */
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
    public void handle(final RoutingContext aContext) {
        final MultiMap params = aContext.queryParams();

    }

}
