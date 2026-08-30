
package info.freelibrary.ark.util;

/**
 * Covenant API operation IDs.
 */
public final class Op {

    /** Gets an array of ARK namespaces. */
    public static final String GET_ARK_NAMESPACES = "getArkNamespaces";

    /** Gets an array of NOID namespaces. */
    public static final String GET_NOID_NAMESPACES = "getNoidNamespaces";

    /** Mints a new ARK namespace. */
    public static final String MINT_ARK_NAMESPACE = "mintArkNamespace";

    /** Mints a new NOID namespace. */
    public static final String MINT_NOID_NAMESPACE = "mintNoidNamespace";

    /** Mints a new ARK identifier. */
    public static final String MINT_ARK = "mintARK";

    /** Mints a new NOID identifier. */
    public static final String MINT_NOID = "mintNOID";

    /**
     * Creates a new collection of Op constants.
     */
    private Op() {
        // This is intentionally left empty
    }

}
