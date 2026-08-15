
package info.freelibrary.ark.handlers;

import info.freelibrary.ark.ContentType;
import info.freelibrary.ark.HTTP;
import info.freelibrary.ark.MessageCodes;
import info.freelibrary.ark.Namespace;
import info.freelibrary.ark.NoidType;
import info.freelibrary.ark.Op;
import info.freelibrary.ark.UnexpectedNoidTypeException;
import info.freelibrary.ark.NoidMinter;
import info.freelibrary.ark.verticles.NamespaceMintingVerticle;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.StringUtils;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * A handler for requests to mint NOID namespaces.
 */
public class MintPidNamespaceHandler implements Handler<RoutingContext> {

    /** The handler's logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MintPidNamespaceHandler.class, MessageCodes.BUNDLE);

    /** The handler's copy of the Vert.x instance. */
    private final Vertx myVertx;

    /** The OpenAPI operation ID. */
    private final String myOpID;

    /**
     * Creates a mint NOID namespace handler.
     *
     * @param aVertx A Vert.x instance
     * @param anOpID The OpenAPI operation ID
     */
    public MintPidNamespaceHandler(final Vertx aVertx, final String anOpID) {
        myVertx = aVertx;
        myOpID = anOpID;
    }

    @Override
    public void handle(@NotNull final RoutingContext aContext) {
        final HttpServerResponse httpResponse = aContext.response();
        final MultiMap params = aContext.request().formAttributes();
        final boolean checksumsRequired = getChecksumsRequirement(params.get(Namespace.CHECKSUMS));
        final String namespace = StringUtils.trimToNull(params.get(Namespace.NAME));
        final String shoulder = params.get(Namespace.SHOULDER);
        final NoidType noidType = NoidType.fromString(params.get(Namespace.NOID_TYPE));
        final int noidLength = getLength(params.get(Namespace.LENGTH));

        LOGGER.debug(MessageCodes.ARK_017, mapToJSON(params));

        if (myOpID != null) {
            LOGGER.debug(myOpID);
        }

        try (NoidMinter minter = new NoidMinter(namespace, noidType, shoulder, noidLength, checksumsRequired)) {
            final DeliveryOptions options = new DeliveryOptions().setSendTimeout(Long.MAX_VALUE);

            options.addHeader(NamespaceMintingVerticle.ACTION, Op.MINT_NOID_NAMESPACE);

            myVertx.eventBus().request(NamespaceMintingVerticle.class.getName(), minter, options)
                    .onSuccess(response -> {
                        httpResponse.setStatusCode(HTTP.CREATED);
                        httpResponse.end();
                    }).onFailure(cause -> returnError(cause, httpResponse));
        } catch (final UnexpectedNoidTypeException | IllegalArgumentException | IOException details) {
            returnError(details, httpResponse);
        }
    }

    /**
     * Returns a string representation of the given MultiMap.
     *
     * @param aParamMap The MultiMap to convert to a string
     * @return A string representation of the given MultiMap
     */
    @NotNull
    private String mapToJSON(@NotNull final MultiMap aParamMap) {
        final JsonObject json = new JsonObject();

        aParamMap.forEach(entry -> {
            json.put(entry.getKey(), entry.getValue());
        });

        return json.encode();
    }

    /**
     * Returns an Internal Server Error response.
     *
     * @param aCause The cause of the error
     * @param aResponse The HTTP response
     */
    private void returnError(@NotNull final Throwable aCause, @NotNull final HttpServerResponse aResponse) {
        final String errorMessage = aCause.getMessage();
        final JsonObject error = new JsonObject().put("code", HTTP.INTERNAL_SERVER_ERROR).put("message",
                LOGGER.getMessage(MessageCodes.ARK_029)); // Don't use the exception message here; log it instead

        LOGGER.error(aCause, errorMessage);

        aResponse.setStatusCode(HTTP.INTERNAL_SERVER_ERROR);
        aResponse.putHeader(HTTP.Response.CONTENT_TYPE, ContentType.TEXT);
        aResponse.end(error.encode(), StandardCharsets.UTF_8.displayName());
    }

    /**
     * Gets the length of the identifier (minus shoulder and checksum character).
     *
     * @param aLength An identifier length
     * @return The length of the identifier
     * @throws NumberFormatException If the supplied string value isn't a valid length
     */
    private int getLength(final String aLength) {
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
        return Boolean.TRUE.toString().equalsIgnoreCase(checksumsRequired);
    }
}
