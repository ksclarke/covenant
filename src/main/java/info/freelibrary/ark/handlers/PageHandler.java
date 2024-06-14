
package info.freelibrary.ark.handlers;

import info.freelibrary.ark.pages.AdminPage;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

/**
 * A handler that returns pages.
 */
public class PageHandler implements Handler<RoutingContext> {

    @Override
    public void handle(final RoutingContext aContext) {
        final HttpServerRequest request = aContext.request();
        final HttpServerResponse response = aContext.response();
        final String language = request.getHeader("Accept-Language");

        response.putHeader("content-type", "text/html");
        response.end(AdminPage.render("Hello!", language));
    }

}
