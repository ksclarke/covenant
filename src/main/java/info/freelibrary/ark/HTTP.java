
package info.freelibrary.ark;

/**
 * A set of HTTP related constants.
 */
public final class HTTP {

    /** Success response */
    public static final int OK = 200;

    /** Created response */
    public static final int CREATED = 201;

    /** Success, no content */
    public static final int NO_CONTENT = 204;

    /** Temporary redirect */
    public static final int TEMP_REDIRECT = 307;

    /** Too many requests */
    public static final int TOO_MANY_REQUESTS = 429;

    /** Not found response */
    public static final int NOT_FOUND = 404;

    /** Method not allowed */
    public static final int METHOD_NOT_ALLOWED = 405;

    /** An empty or other unsupported media type */
    public static final int UNSUPPORTED_MEDIA_TYPE = 415;

    /** Generic internal server error */
    public static final int INTERNAL_SERVER_ERROR = 500;

    /** Bad request */
    public static final int BAD_REQUEST = 400;

    /**
     * A private constructor for the constants class.
     */
    private HTTP() {
    }

    /**
     * A class of HTTP response constants.
     */
    public final class Response {

        /**
         * The content-type setting for an HTTP response.
         */
        public static final String CONTENT_TYPE = "content-type";

        /**
         * Constructors for constant classes should be private.
         */
        private Response() {
        }
    }
}
