
package info.freelibrary.ark.handlers;

import java.nio.charset.StandardCharsets;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;

import info.freelibrary.ark.ContentType;
import info.freelibrary.ark.HTTP;
import info.freelibrary.ark.MessageCodes;
import info.freelibrary.ark.Namespace;
import info.freelibrary.ark.NoidType;
import info.freelibrary.ark.Op;
import info.freelibrary.ark.UnexpectedNoidTypeException;
import info.freelibrary.ark.utils.NoidMinter;
import info.freelibrary.ark.verticles.NamespaceMintingVerticle;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

/**
 * A handler for requests to mint NOID namespaces.
 */
public class MintNoidNamespaceHandler implements Handler<RoutingContext> {

    /**
     * The handler's logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MintNoidNamespaceHandler.class, MessageCodes.BUNDLE);

    /**
     * The handler's copy of the Vert.x instance.
     */
    private final Vertx myVertx;

    /**
     * Creates a mint NOID namespace handler.
     *
     * @param aVertx A Vert.x instance
     */
    public MintNoidNamespaceHandler(final Vertx aVertx) {
        myVertx = aVertx;
    }

    @Override
    public void handle(final RoutingContext aContext) {
        final HttpServerResponse httpResponse = aContext.response();
        final MultiMap params = aContext.request().params();
        final boolean checksumsRequired = getChecksumsRequirement(params.get(Namespace.CHECKSUMS));
        final String namespace = StringUtils.trimToNull(params.get(Namespace.NAME));
        final String shoulder = params.get(Namespace.SHOULDER);

        LOGGER.debug(MessageCodes.ARK_017, System.lineSeparator() + params.toString());

        try {
            final NoidType noidType = NoidType.fromString(params.get(Namespace.NOID_TYPE));
            final int noidLength = getLength(params.get(Namespace.LENGTH));
            final NoidMinter minter = new NoidMinter(namespace, noidType, shoulder, noidLength, checksumsRequired);
            final DeliveryOptions options = new DeliveryOptions().setSendTimeout(Long.MAX_VALUE);

            options.addHeader(NamespaceMintingVerticle.ACTION, Op.MINT_NOID_NAMESPACE);

            myVertx.eventBus().request(NamespaceMintingVerticle.class.getName(), minter, options, response -> {
                if (response.succeeded()) {
                    LOGGER.debug("message successful");
                    httpResponse.setStatusCode(HTTP.CREATED);
                    httpResponse.end();
                } else {
                    final String errorMessage = response.cause().getMessage();

                    LOGGER.error(response.cause(), errorMessage);

                    httpResponse.setStatusCode(HTTP.INTERNAL_SERVER_ERROR);
                    httpResponse.setStatusMessage(errorMessage);
                    httpResponse.putHeader(HTTP.Response.CONTENT_TYPE, ContentType.TEXT);
                    httpResponse.end(errorMessage, StandardCharsets.UTF_8.displayName());
                }
            });
        } catch (final UnexpectedNoidTypeException | IllegalArgumentException details) {
            final String errorMessage = details.getMessage();

            LOGGER.error(details, errorMessage);

            httpResponse.setStatusCode(HTTP.INTERNAL_SERVER_ERROR);
            httpResponse.setStatusMessage(errorMessage);
            httpResponse.putHeader(HTTP.Response.CONTENT_TYPE, ContentType.TEXT);
            httpResponse.end(errorMessage, StandardCharsets.UTF_8.displayName());
        }
    }

    /**
     * Gets the length of the identifier (minus shoulder and checksum character).
     *
     * @param aLength An identifier length
     * @return The length of the identifier
     * @throws NumberFormatException If the supplied string value isn't a valid length
     */
    private int getLength(final String aLength) throws NumberFormatException {
        final int length;

        if (StringUtils.trimToNull(aLength) == null || (length = Integer.parseInt(aLength)) == 0) {
            throw new NumberFormatException(LOGGER.getMessage(MessageCodes.ARK_013, aLength));
        }

        return length;
    }

    /**
     * Gets whether checksums are required or not. The default is yes.
     *
     * @param aChecksumsReqFlag Whether checksums are required
     * @return True if checksums are required; else, false
     */
    private boolean getChecksumsRequirement(final String aChecksumsReqFlag) {
        final String checksumsRequired = StringUtils.trimToNull(aChecksumsReqFlag);

        if ("true".equalsIgnoreCase(checksumsRequired)) {
            return true;
        }

        return false;
    }
}
