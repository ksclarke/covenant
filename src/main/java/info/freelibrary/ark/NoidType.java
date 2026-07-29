
package info.freelibrary.ark;

import static info.freelibrary.util.Constants.DASH;
import static info.freelibrary.util.Constants.UNDERSCORE;

import info.freelibrary.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * An enumeration of the possible NOID types.
 */
public enum NoidType {

    /** A numeric character type. */
    NUMERIC(Integer.toString(10)),

    /** An alpha character type. */
    ALPHA(Integer.toString(25)),

    /** An all-alpha character type. */
    ALPHA_ALL(Integer.toString(51)),

    /** An alphanumeric character type. */
    ALPHANUMERIC(Integer.toString(35)),

    /** An all-alphanumeric character type. */
    ALPHANUMERIC_ALL(Integer.toString(61)),

    /** A regex character type. */
    REGEX_PATTERN(Integer.toString(35)),

    /** An all-regex character type. */
    REGEX_PATTERN_ALL(Integer.toString(61));

    /* ChecksumUtils depends on the order of these arrays remaining constant; changing them will break checksums. */

    /** Lowercase alpha characters. */
    private static final Character[] ALPHA_LC_CHARS = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'm', 'n',
        'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' }; // 25 chars; it's missing the lower case L

    /** Uppercase alpha characters. */
    private static final Character[] ALPHA_UC_CHARS = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
        'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z' }; // 26 chars

    /** Numeric characters. */
    private static final Character[] NUMERIC_CHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' }; // 10 chars

    /** A character value. */
    public final String myValue;

    /**
     * Creates a new NOID type.
     *
     * @param aValue A character value
     */
    NoidType(final String aValue) {
        myValue = aValue;
    }

    /**
     * Acts like valueOf but is a bit more forgiving of variation in input.
     *
     * @param aValue A NoidType value in string form
     * @return A NoidType that corresponds to the string input
     * @throws UnexpectedNoidTypeException If the supplied string isn't a supported NOID type
     */
    @NotNull
    public static NoidType fromString(@NotNull final String aValue) {
        try {
            return valueOf(Objects.requireNonNull(aValue).toUpperCase(Locale.US).replace(DASH, UNDERSCORE));
        } catch (final IllegalArgumentException details) {
            throw new UnexpectedNoidTypeException(StringUtils.trimToNull(aValue) == null ? "(null)" : aValue, details);
        }
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
        return switch (this) {
            // 10 characters
            case NUMERIC -> NUMERIC_CHARS;
            // 25 characters
            case ALPHA -> ALPHA_LC_CHARS;
            // 51 characters
            case ALPHA_ALL -> merge(ALPHA_UC_CHARS, ALPHA_LC_CHARS);
            // 35 characters
            case ALPHANUMERIC, REGEX_PATTERN -> merge(NUMERIC_CHARS, ALPHA_LC_CHARS);
            // 61 characters
            case ALPHANUMERIC_ALL, REGEX_PATTERN_ALL -> merge(NUMERIC_CHARS, ALPHA_UC_CHARS, ALPHA_LC_CHARS);
        };
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
     * Conveniently joins smaller arrays of acceptable characters into a single array.
     *
     * @param aArrayOfCharArrays An array of acceptable character arrays
     * @return A merged array of character arrays
     */
    @NotNull
    private Character[] merge(final Character[]... aArrayOfCharArrays) {
        return Stream.of(aArrayOfCharArrays).<Character>flatMap(Stream::of).toArray(Character[]::new);
    }
}
