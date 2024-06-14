
package info.freelibrary.ark;

import info.freelibrary.util.I18nRuntimeException;

/**
 * An exception thrown if something goes wrong with the minting process. This usually means something about the
 * application, or the system running it, is catastrophically broken.
 */
public class NoidGenerationException extends I18nRuntimeException {

    /**
     * The <code>serialVersionUID</code> of NoidGenerationException.
     */
    private static final long serialVersionUID = 1715863014923173350L;

    /**
     * Creates a new NoidGenerationException.
     *
     * @param aNOID The NOID that failed to be minted
     */
    public NoidGenerationException(final String aNOID) {
        super(MessageCodes.BUNDLE, MessageCodes.ARK_003, aNOID);
    }

    /**
     * Creates a new NoidGenerationException from the supplied exception.
     *
     * @param aRootCause A parent exception
     * @param aNOID The NOID that failed to be minted
     */
    public NoidGenerationException(final Throwable aRootCause, final String aNOID) {
        super(aRootCause, MessageCodes.BUNDLE, MessageCodes.ARK_003, aNOID);
    }

}
