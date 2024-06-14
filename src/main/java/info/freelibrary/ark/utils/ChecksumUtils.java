
package info.freelibrary.ark.utils;

import java.util.stream.IntStream;

import info.freelibrary.ark.NoidType;

/**
 * Utilities for creating and validating NOID checksums. NOID checksums that are created by this library use the (Luhn
 * mod N algorithm)[https://en.wikipedia.org/wiki/Luhn_mod_N_algorithm]. They are intended to catch typos, not protect
 * against malicious intents.
 */
public final class ChecksumUtils {

    // Utility classes should not have public constructors
    private ChecksumUtils() {
    }

    /**
     * Gets a checksum character for the supplied NOID.
     *
     * @param aBareNOID A NOID for which to create a checksum
     * @param aNoidType The type of NOID supplied
     * @return A checksum character
     */
    public static Character getChecksum(final String aBareNOID, final NoidType aNoidType) {
        final Character[] characters = aNoidType.getCharacters();

        int factor = 2;
        int sum = 0;

        for (int index = aBareNOID.length() - 1; index >= 0; index--) {
            int addend = factor * getCodePoint(aBareNOID.charAt(index), characters);

            factor = factor == 2 ? 1 : 2;
            addend = addend / characters.length + addend % characters.length;
            sum += addend;
        }

        return characters[(characters.length - sum % characters.length) % characters.length];
    }

    /**
     * Appends a checksum to the end of the supplied NOID.
     *
     * @param aBareNOID A NOID without a checksum already appended
     * @param aNoidType The type of NOID supplied
     * @return A NOID with a checksum
     */
    public static String appendChecksum(final String aBareNOID, final NoidType aNoidType) {
        return new StringBuilder(aBareNOID).append(getChecksum(aBareNOID, aNoidType)).toString();
    }

    /**
     * Validate a NOID with a checksum at the end of it.
     *
     * @param aNOID A NOID to validate
     * @param aNoidType The type of NOID supplied
     * @return True if the NOID's checksum is valid; else, false
     */
    public static boolean validate(final String aNOID, final NoidType aNoidType) {
        final int charCount = aNoidType.getCharacterCount();

        int factor = 1;
        int sum = 0;

        for (int index = aNOID.length() - 1; index >= 0; index--) {
            int addend = factor * getCodePoint(aNOID.charAt(index), aNoidType.getCharacters());

            factor = factor == 2 ? 1 : 2;
            addend = addend / charCount + addend % charCount;
            sum += addend;
        }

        return sum % charCount == 0;
    }

    /**
     * Get the code point for the supplied character.
     *
     * @param aChar A character for which to get the code point
     * @param aCharArray A character array
     * @return A code point
     */
    private static int getCodePoint(final char aChar, final Character[] aCharArray) {
        return IntStream.range(0, aCharArray.length).filter(index -> aChar == aCharArray[index]).findFirst().orElse(-1);
    }
}
