
package info.freelibrary.ark;

import java.util.Locale;
import java.util.stream.Stream;

import info.freelibrary.util.StringUtils;

/**
 * A enumeration of the possible NOID types.
 */
public enum NoidType {

    NUMERIC("10"), ALPHA("25"), ALPHA_ALL("51"), @SuppressWarnings("MultipleStringLiterals")
    ALPHANUMERIC("35"), @SuppressWarnings("MultipleStringLiterals")
    ALPHANUMERIC_ALL("61"), @SuppressWarnings("MultipleStringLiterals")
    REGEX_PATTERN("35"), @SuppressWarnings("MultipleStringLiterals")
    REGEX_PATTERN_ALL("61");

    /* ChecksumUtils depends on the order of these arrays remaining constant; changing them will break checksums. */

    private static final Character[] ALPHA_LC_CHARS = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'm', 'n',
        'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' }; // 25 chars; it's missing the lower case L

    private static final Character[] ALPHA_UC_CHARS = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
        'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z' }; // 26 chars

    private static final Character[] NUMERIC_CHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' }; // 10 chars

    public final String myValue;

    NoidType(final String aValue) {
        myValue = aValue;
    }

    /**
     * Gets the total number of possible NOIDs given the supplied length.
     *
     * @param aLength A maximum character length of minted NOIDs.
     * @return The total number of possible NOIDS given the supplied length
     */
    public long getNoidCount(final int aLength) {
        return (long) Math.pow(getCharacters().length, aLength);
    }

    /**
     * Gets the character array for the supplied NOID type.
     *
     * @return An array of characters represented by the supplied NOID type
     */
    public Character[] getCharacters() {
        switch (this) {
            case NUMERIC:
                return NUMERIC_CHARS; // 10 characters
            case ALPHA:
                return ALPHA_LC_CHARS; // 25 characters
            case ALPHA_ALL:
                return merge(ALPHA_UC_CHARS, ALPHA_LC_CHARS); // 51 characters
            case ALPHANUMERIC:
            case REGEX_PATTERN:
                return merge(NUMERIC_CHARS, ALPHA_LC_CHARS); // 35 characters
            case ALPHANUMERIC_ALL:
            case REGEX_PATTERN_ALL:
                return merge(NUMERIC_CHARS, ALPHA_UC_CHARS, ALPHA_LC_CHARS); // 61 characters
            default:
                throw new UnexpectedNoidTypeException(this);
        }
    }

    /**
     * Gets the number of possible characters for a supplied NOID type.
     *
     * @return The number of possible characters for a supplied NOID type
     */
    public int getCharacterCount() {
        return getCharacters().length;
    }

    /**
     * Acts like valueOf but is a bit more forgiving of variation in input.
     *
     * @param aValue A NoidType value in string form
     * @return A NoidType that corresponds to the string input
     * @throws UnexpectedNoidTypeException If the supplied string isn't a supported NOID type
     */
    public static NoidType fromString(final String aValue) {
        try {
            return valueOf(aValue.toUpperCase(Locale.US).replace("-", "_"));
        } catch (final NullPointerException | IllegalArgumentException details) {
            throw new UnexpectedNoidTypeException(StringUtils.trimToNull(aValue) == null ? "(null)" : aValue);
        }
    }

    /**
     * Conveniently joins smaller arrays of acceptable characters into a single array.
     *
     * @param aArrayOfCharArrays An array of acceptable character arrays
     * @return A merged array of character arrays
     */
    private Character[] merge(final Character[]... aArrayOfCharArrays) {
        return Stream.of(aArrayOfCharArrays).<Character>flatMap(Stream::<Character>of).toArray(Character[]::new);
    }
}
