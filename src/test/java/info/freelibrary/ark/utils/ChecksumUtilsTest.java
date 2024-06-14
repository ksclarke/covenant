
package info.freelibrary.ark.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import info.freelibrary.ark.NoidType;

/**
 * Tests of the checksum utilities.
 */
public class ChecksumUtilsTest {

    // Our test data.
    private final List<TestData> myTestData = Arrays.asList( //
            new TestData(NoidType.NUMERIC, "9485900", '6'), //
            new TestData(NoidType.ALPHA, "awjdkjo", 'c'), //
            new TestData(NoidType.ALPHA_ALL, "aWjDkJo", 'w'), //
            new TestData(NoidType.ALPHANUMERIC, "e9f0rt9", '9'), //
            new TestData(NoidType.ALPHANUMERIC_ALL, "e9F0rt9", '8'));

    /**
     * Tests getting the checksum of a supplied NOID.
     */
    @Test
    public void testGetChecksum() {
        for (final TestData testData : myTestData) {
            assertEquals(testData.myChecksum, ChecksumUtils.getChecksum(testData.myBareNOID, testData.myNoidType));
        }
    }

    /**
     * Tests validating a NOID that has a checksum appended to it.
     */
    @Test
    public void testValidate() {
        for (final TestData testData : myTestData) {
            assertTrue(ChecksumUtils.validate(testData.myBareNOID + testData.myChecksum, testData.myNoidType));
        }
    }

    /**
     * Tests appending a checksum onto a NOID string.
     */
    @Test
    public void testAppendChecksum() {
        for (final TestData testData : myTestData) {
            assertEquals(testData.myNOID, ChecksumUtils.appendChecksum(testData.myBareNOID, testData.myNoidType));
        }
    }

    /**
     * Data used for testing the checksum utilities.
     */
    private final class TestData {

        /**
         * A NOID type.
         */
        private final NoidType myNoidType;

        /**
         * A NOID string.
         */
        private final String myBareNOID;

        /**
         * A checksum character.
         */
        private final Character myChecksum;

        /**
         * A NOID with an appended checksum.
         */
        private final String myNOID;

        /**
         * Creates a new TestData object.
         *
         * @param aNoidType A NOID type
         * @param aNOID A NOID string
         * @param aChecksum A checksum character
         */
        private TestData(final NoidType aNoidType, final String aNOID, final char aChecksum) {
            myNoidType = aNoidType;
            myBareNOID = aNOID;
            myChecksum = aChecksum;
            myNOID = aNOID + aChecksum;
        }
    }
}
