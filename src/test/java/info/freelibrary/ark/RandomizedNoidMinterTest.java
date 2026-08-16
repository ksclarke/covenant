
package info.freelibrary.ark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import info.freelibrary.ark.utils.ChecksumUtils;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Tests the randomized NOID minter.
 */
public class RandomizedNoidMinterTest {

    /** The logger used by these tests. */
    private static final Logger LOGGER = LoggerFactory.getLogger(RandomizedNoidMinterTest.class, MessageCodes.BUNDLE);

    /** The location of the JVM's temporary directory. */
    private static final File TMP_DIR = new File(System.getProperty("java.io.tmpdir"));

    /** A convenient hook into the test method that's running. */
    private String myTestMethodName;

    /** The namespace of the test minter. */
    private String myNamespace;

    /**
     * Clean up after tests.
     */
    @AfterAll
    public static void tearDown() {
        final File alphanumericFive = new File(TMP_DIR, NoidType.ALPHANUMERIC.toString() + "-5.naf");

        // Clean this up after all this class' tests
        if (alphanumericFive.exists()) {
            assertTrue(alphanumericFive.delete());
        }
    }

    /**
     * Sets up the tests.
     *
     * @param aTestInfo Information about the test being run
     */
    @BeforeEach
    public void setUp(final TestInfo aTestInfo) {
        myNamespace = UUID.randomUUID().toString();
        myTestMethodName = aTestInfo.getTestMethod().get().getName();
    }

    /**
     * Test method for {@link info.freelibrary.ark.RandomizedNoidMinter#next()}.
     */
    @Test
    public void testNextNoid() throws Exception {
        LOGGER.debug(MessageCodes.ARK_027, myTestMethodName);

        try (RandomizedNoidMinter minter = new RandomizedNoidMinter(myNamespace, NoidType.ALPHANUMERIC, 5, false)) {
            for (int index = 0; index < 10; index++) {
                final String noid = minter.next().await();
                assertTrue(noid.matches("[0-9a-z]{5}"), noid);
            }
        }
    }

    /**
     * Test method for {@link RandomizedNoidMinter#next(int)}.
     */
    @Test
    public void testNextIntNoid() throws Exception {
        LOGGER.debug(MessageCodes.ARK_027, myTestMethodName);

        try (Minter minter = new RandomizedNoidMinter(myNamespace, NoidType.ALPHANUMERIC, "k3", 5, false)) {
            final List<String> noidList = minter.next(10).await();
            final Iterator<String> iterator = noidList.iterator();

            assertEquals(10, noidList.size());

            while (iterator.hasNext()) {
                assertTrue(iterator.next().matches("k3[a-z0-9]{5}"));
            }
        }
    }

    /**
     * Test method for {@link RandomizedNoidMinter#next()}.
     */
    @Test
    public void testNextChecksumNoid() throws Exception {
        LOGGER.debug(MessageCodes.ARK_027, myTestMethodName);

        try (RandomizedNoidMinter minter = new RandomizedNoidMinter(myNamespace, NoidType.ALPHANUMERIC, 5)) {
            for (int index = 0; index < 10; index++) {
                final String noid = minter.next().await();

                // Checksums are produced by default
                assertTrue(noid.matches("[a-z0-9]{6}"));
                assertTrue(ChecksumUtils.validate(noid, NoidType.ALPHANUMERIC));
            }
        }
    }

    /**
     * Test method for {@link RandomizedNoidMinter#next()}.
     */
    @Test
    public void testNextShoulderChecksumNoid() throws Exception {
        LOGGER.debug(MessageCodes.ARK_027, myTestMethodName);

        try (RandomizedNoidMinter minter = new RandomizedNoidMinter(myNamespace, NoidType.ALPHANUMERIC, "f5", 5)) {
            for (int index = 0; index < 10; index++) {
                final String noid = minter.next().await();

                // Checksums are produced by default
                assertTrue(noid.matches("f5[a-z0-9]{6}"));
                assertTrue(ChecksumUtils.validate(noid, NoidType.ALPHANUMERIC));
            }
        }
    }

    /**
     * Test method for {@link RandomizedNoidMinter#toString()}.
     */
    @Test
    public void testToString() throws Exception {
        LOGGER.debug(MessageCodes.ARK_027, myTestMethodName);

        try (RandomizedNoidMinter minter = new RandomizedNoidMinter(myNamespace, NoidType.ALPHANUMERIC, "f7", 5)) {
            assertTrue(minter.toString().matches(
                    RandomizedNoidMinter.class.getSimpleName() + "=\\[#[0-9]+, " + TMP_DIR + "/ALPHANUMERIC-5.naf\\]"));
        }
    }

    /**
     * Test method for {@link RandomizedNoidMinter#getNOID(long)} with a negative index.
     */
    @Test
    public void testGetNOIDNegativeIndex() throws Exception {
        try (RandomizedNoidMinter minter = new RandomizedNoidMinter(myNamespace, NoidType.ALPHANUMERIC, 5, false)) {
            assertThrows(IndexOutOfBoundsException.class, () -> minter.getNOID(-1L).await());
        }
    }

    /**
     * Test method for {@link RandomizedNoidMinter#getNOID(long)} with an index beyond the available NOIDs.
     */
    @Test
    public void testGetNOIDOutOfRangeIndex() throws Exception {
        try (RandomizedNoidMinter minter = new RandomizedNoidMinter(myNamespace, NoidType.ALPHANUMERIC, 5, false)) {
            assertThrows(IndexOutOfBoundsException.class,
                    () -> minter.getNOID(NoidType.ALPHANUMERIC.getNoidCount(5)).await());
        }
    }

    /**
     * Test method for {@link RandomizedNoidMinter#getNOID(long)}.
     */
    @Test
    public void testGetNOID() throws Exception {
        try (RandomizedNoidMinter minter = new RandomizedNoidMinter(myNamespace, NoidType.ALPHANUMERIC, 5, false)) {
            assertEquals("00000", minter.getNOID(0L).await());
        }
    }
}
