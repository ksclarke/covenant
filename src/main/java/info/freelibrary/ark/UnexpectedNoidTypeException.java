
package info.freelibrary.ark;

import info.freelibrary.util.I18nRuntimeException;

/**
 * An exception thrown when the requested NoidType is not known.
 */
public class UnexpectedNoidTypeException extends I18nRuntimeException {

    /**
     * The <code>serialVersionUID</code> of UnexpectedNotTypeException.
     */
    private static final long serialVersionUID = 5086512635321685665L;

    /**
     * Creates a new unexpected NoidType exception.
     *
     * @param aNoidType The NOID type that isn't supported or known
     */
    public UnexpectedNoidTypeException(final NoidType aNoidType) {
        super(MessageCodes.BUNDLE, MessageCodes.ARK_005, aNoidType.name());
    }

    /**
     * Creates a new unexpected NoidType exception.
     *
     * @param aNoidTypeValue A NOID type value that isn't supported or known
     */
    public UnexpectedNoidTypeException(final String aNoidTypeValue) {
        super(MessageCodes.BUNDLE, MessageCodes.ARK_005, aNoidTypeValue);
    }
}
