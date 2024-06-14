
package info.freelibrary.ark;

/**
 * Covenant API operation IDs.
 */
public final class Op {

    /**
     * Mints a new ARK namespace.
     */
    public static final String MINT_ARK_NAMESPACE = "mintArkNamespace";

    /**
     * Mints a new NOID namespace.
     */
    public static final String MINT_NOID_NAMESPACE = "mintNoidNamespace";

    /**
     * Mints a new ARK identifier.
     */
    public static final String MINT_ARK = "mintARK";

    /**
     * Mints a new NOID identifier.
     */
    public static final String MINT_NOID = "mintNOID";

    /**
     * Constant class constructors should be private.
     */
    private Op() {
    }

}
