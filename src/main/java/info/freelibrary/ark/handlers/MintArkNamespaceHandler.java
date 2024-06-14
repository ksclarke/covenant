
package info.freelibrary.ark.handlers;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

/**
 * A mint ARK namespace handler.
 */
public class MintArkNamespaceHandler implements Handler<RoutingContext> {

    /**
     * The handler's copy of the Vert.x instance.
     */
    private final Vertx myVertx;

    /**
     * Creates a new mint ARK namespace handler.
     *
     * @param aVertx A Vert.x instance
     */
    public MintArkNamespaceHandler(final Vertx aVertx) {
        myVertx = aVertx;
    }

    @Override
    public void handle(final RoutingContext aContext) {
        final MultiMap params = aContext.queryParams();

    }

}
