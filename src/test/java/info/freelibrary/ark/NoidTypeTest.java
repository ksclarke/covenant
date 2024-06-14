
package info.freelibrary.ark;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

/**
 * Tests of the NoidType enumeration.
 */
public class NoidTypeTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoidTypeTest.class, MessageCodes.BUNDLE);

    private static final String ALPHA_STRING_LC = "alpha";

    private static final String ALPHA_STRING_UC = "ALPHA";

    private static final String ALPHA_ALL_STRING_LC = "alpha-all";

    private static final String ALPHA_ALL_STRING_UC = "ALPHA-all";

    /**
     * Tests NoidType's from method.
     */
    @Test
    public void testFromString() {
        assertEquals(NoidType.ALPHA_ALL, NoidType.fromString(ALPHA_ALL_STRING_UC));
        assertEquals(NoidType.ALPHA_ALL, NoidType.fromString(ALPHA_ALL_STRING_LC));
        assertEquals(NoidType.ALPHA, NoidType.fromString(ALPHA_STRING_UC));
        assertEquals(NoidType.ALPHA, NoidType.fromString(ALPHA_STRING_LC));
    }

    /**
     * Tests whether fromString method throws an exception on receiving an empty string value.
     */
    @Test(expected = UnexpectedNoidTypeException.class)
    public void testFromStringNullValue() {
        NoidType.fromString(null);
        fail(LOGGER.getMessage(MessageCodes.ARK_012, UnexpectedNoidTypeException.class));
    }

    /**
     * Tests whether fromString method throws an exception on receiving an empty string value.
     */
    @Test(expected = UnexpectedNoidTypeException.class)
    public void testFromStringEmptyValue() {
        NoidType.fromString("");
        fail(LOGGER.getMessage(MessageCodes.ARK_012, UnexpectedNoidTypeException.class));
    }

    /**
     * Tests whether fromString method throws an exception on receiving an invalid NOID type value.
     */
    @Test(expected = UnexpectedNoidTypeException.class)
    public void testFromStringInvalidValue() {
        NoidType.fromString("orange");
        fail(LOGGER.getMessage(MessageCodes.ARK_012, UnexpectedNoidTypeException.class));
    }

    /**
     * Tests the valueOf method.
     */
    @Test
    public void testValueOf() {
        assertEquals(NoidType.ALPHA, NoidType.valueOf(ALPHA_STRING_UC));
    }

    /**
     * Tests what happens when an illegal argument is passed to NoidType's valueOf method.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testIllegalValueOf() {
        NoidType.valueOf(ALPHA_STRING_LC);
        fail(LOGGER.getMessage(MessageCodes.ARK_012, IllegalArgumentException.class));
    }

    /**
     * Tests getting the numeric characters.
     */
    @Test
    public void testGetCharactersNumeric() {
        assertEquals(10, NoidType.NUMERIC.getCharacters().length);
    }

    /**
     * Tests getting the character count for the numeric characters.
     */
    @Test
    public void testGetCharacterCountNumeric() {
        assertEquals(NoidType.NUMERIC.getCharacterCount(), NoidType.NUMERIC.getCharacters().length);
    }

    /**
     * Tests getting the alpha characters.
     */
    @Test
    public void testGetCharactersAlpha() {
        assertEquals(25, NoidType.ALPHA.getCharacters().length);
    }

    /**
     * Tests getting the character count for the alpha characters.
     */
    @Test
    public void testGetCharacterCountAlpha() {
        assertEquals(NoidType.ALPHA.getCharacterCount(), NoidType.ALPHA.getCharacters().length);
    }

    /**
     * Tests getting all the alpha characters.
     */
    @Test
    public void testGetCharactersAlphaAll() {
        assertEquals(51, NoidType.ALPHA_ALL.getCharacters().length);
    }

    /**
     * Tests getting the character count for all the alpha characters.
     */
    @Test
    public void testGetCharacterCountAlphaAll() {
        assertEquals(NoidType.ALPHA_ALL.getCharacterCount(), NoidType.ALPHA_ALL.getCharacters().length);
    }

    /**
     * Tests getting the alphanumeric characters.
     */
    @Test
    public void testGetCharactersAlphaNumeric() {
        assertEquals(35, NoidType.ALPHANUMERIC.getCharacters().length);
    }

    /**
     * Tests getting the character count for the alphanumeric characters.
     */
    @Test
    public void testGetCharacterCountAlphaNumeric() {
        assertEquals(NoidType.ALPHANUMERIC.getCharacterCount(), NoidType.ALPHANUMERIC.getCharacters().length);
    }

    /**
     * Tests getting all the alphanumeric characters.
     */
    @Test
    public void testGetCharactersAlphaNumericAll() {
        assertEquals(61, NoidType.ALPHANUMERIC_ALL.getCharacters().length);
    }

    /**
     * Tests getting the character count for all the alphanumeric characters.
     */
    @Test
    public void testGetCharacterCountAlphaNumericAll() {
        assertEquals(NoidType.ALPHANUMERIC_ALL.getCharacterCount(), NoidType.ALPHANUMERIC_ALL.getCharacters().length);
    }

    /**
     * Tests the maximum number of NOIDs that the minter can mint.
     */
    @Test
    public void testgetNoidCount() {
        assertEquals(6765201L, NoidType.ALPHA_ALL.getNoidCount(4), 0);
        assertEquals(51520374361L, NoidType.ALPHANUMERIC_ALL.getNoidCount(6), 0);
        assertEquals(2251875390625L, NoidType.ALPHANUMERIC.getNoidCount(8), 0);
    }

    /**
     * Tests converting the NoidType to a string.
     */
    @Test
    public void testToString() {
        System.out.println("String: " + NoidType.ALPHA.toString()); // FIXME
    }

    /**
     * Tests getting the name of the NoidType.
     */
    @Test
    public void testName() {
        System.out.println("Name: " + NoidType.ALPHA.name()); // FIXME
    }
}
