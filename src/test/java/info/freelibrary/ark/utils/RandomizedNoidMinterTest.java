/**
 *
 */

package info.freelibrary.ark.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import info.freelibrary.ark.MessageCodes;
import info.freelibrary.ark.NoidType;

/**
 * Tests the randomized NOID minter.
 */
public class RandomizedNoidMinterTest {

    /* The logger used by these tests. */
    private static final Logger LOGGER = LoggerFactory.getLogger(RandomizedNoidMinterTest.class, MessageCodes.BUNDLE);

    /* The location of the JVM's temporary directory. */
    private static final File TMP_DIR = new File(System.getProperty("java.io.tmpdir"));

    /* A convenient hook into the test that's running. */
    @Rule
    public TestName myTestName = new TestName();

    /**
     * The namespace of the test minter.
     */
    private String myNamespace;

    /**
     * Sets up the tests.
     */
    @Before
    public void setUp() {
        myNamespace = UUID.randomUUID().toString();
    }

    /**
     * Clean up after tests.
     */
    @AfterClass
    public static void tearDown() {
        final File alphanumericFive = new File(TMP_DIR, NoidType.ALPHANUMERIC.toString() + "-5.naf");

        // Clean this up after all this class' tests
        if (alphanumericFive.exists()) {
            assertTrue(alphanumericFive.delete());
        }
    }

    /**
     * Test method for {@link RandomizedNoidMinter#next()}.
     */
    @Test
    public void testNextNoid() throws Exception {
        LOGGER.debug(MessageCodes.ARK_027, myTestName.getMethodName());

        try (RandomizedNoidMinter minter = new RandomizedNoidMinter(myNamespace, NoidType.ALPHANUMERIC, 5, false)) {
            for (int index = 0; index < 10; index++) {
                final String noid = minter.next();
                assertTrue(noid, noid.matches("[0-9a-z]{5}"));
            }
        }
    }

    /**
     * Test method for {@link RandomizedNoidMinter#next(int)}.
     */
    @Test
    public void testNextIntNoid() throws Exception {
        LOGGER.debug(MessageCodes.ARK_027, myTestName.getMethodName());

        try (RandomizedNoidMinter minter =
                new RandomizedNoidMinter(myNamespace, NoidType.ALPHANUMERIC, "k3", 5, false)) {
            final List<String> noids = minter.next(10);
            final Iterator<String> iterator = noids.iterator();

            assertEquals(10, noids.size());

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
        LOGGER.debug(MessageCodes.ARK_027, myTestName.getMethodName());

        try (RandomizedNoidMinter minter = new RandomizedNoidMinter(myNamespace, NoidType.ALPHANUMERIC, 5)) {
            for (int index = 0; index < 10; index++) {
                final String noid = minter.next();

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
        LOGGER.debug(MessageCodes.ARK_027, myTestName.getMethodName());

        try (RandomizedNoidMinter minter = new RandomizedNoidMinter(myNamespace, NoidType.ALPHANUMERIC, "f5", 5)) {
            for (int index = 0; index < 10; index++) {
                final String noid = minter.next();

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
        LOGGER.debug(MessageCodes.ARK_027, myTestName.getMethodName());

        try (RandomizedNoidMinter minter = new RandomizedNoidMinter(myNamespace, NoidType.ALPHANUMERIC, "f7", 5)) {
            assertTrue(minter.toString().matches(
                    RandomizedNoidMinter.class.getSimpleName() + "=\\[#[0-9]+, " + TMP_DIR + "/ALPHANUMERIC-5.naf\\]"));
        }
    }

    /**
     * Test method for {@link RandomizedNoidMinter#getNafNOID()}.
     */
    @Test
    public void testGetNOID() throws Exception {
        try (RandomizedNoidMinter minter = new RandomizedNoidMinter(myNamespace, NoidType.ALPHANUMERIC, 5, false)) {
            assertEquals("00000", minter.getNOID(0L));
        }
    }
}
