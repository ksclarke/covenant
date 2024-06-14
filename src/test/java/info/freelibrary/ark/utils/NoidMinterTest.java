
package info.freelibrary.ark.utils;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.Stopwatch;
import info.freelibrary.util.StringUtils;

import info.freelibrary.ark.MessageCodes;
import info.freelibrary.ark.NoidType;

/**
 * A test of the NOID minter.
 */
public class NoidMinterTest {

    /**
     * The logger used in testing.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(NoidMinterTest.class, MessageCodes.BUNDLE);

    /**
     * A test shoulder.
     */
    private static final String TEST_SHOULDER = "f3";

    /**
     * A minter namespace to be used in the test.
     */
    private String myNamespace;

    /**
     * Sets up test.
     */
    @Before
    public final void setUp() {
        myNamespace = UUID.randomUUID().toString();
    }

    /**
     * Tests how long the minting process takes.
     *
     * @throws IOException If there is trouble writing the minted NOIDs.
     */
    @Test
    public final void testTime() throws IOException {
        final NoidMinter minter = new NoidMinter(myNamespace, NoidType.ALPHA, 6);
        final Stopwatch stopwatch = new Stopwatch().start();

        try (FileWriter writer = new FileWriter("/tmp/alpha-6.ser")) {
            long count = 0;

            while (minter.hasNext()) {
                writer.write(minter.next());
                writer.write(System.lineSeparator());
                count++;

                if (count % 1000000 == 0) {
                    LOGGER.trace(MessageCodes.ARK_008, count);
                }
            }

            LOGGER.debug(MessageCodes.ARK_009, "244,140,625", stopwatch.stop().getSeconds());
        }
    }

    /**
     * Tests NoidMinter serialization.
     *
     * @throws IOException If the serialized object can't be written or read from disk
     * @throws ClassNotFoundException If the NoidMinter class cannot be found
     */
    @Test
    public final void testSerialization() throws IOException, ClassNotFoundException {
        final NoidMinter minter = new NoidMinter(myNamespace, NoidType.NUMERIC, 5);
        final File serializedObj = new File("/tmp", UUID.randomUUID().toString() + ".ser");
        final String minterState;

        // Increment the minter so it's minted some NOIDs
        for (int index = 0; index < 26; index++) {
            minter.next();
        }

        minterState = minter.toString();

        // Serialize the minter to disk
        try (ObjectOutputStream outStream = new ObjectOutputStream(new FileOutputStream(serializedObj))) {
            outStream.writeObject(minter);
        }

        try (ObjectInputStream inStream = new ObjectInputStream(new FileInputStream(serializedObj))) {
            assertEquals(minterState, ((NoidMinter) inStream.readObject()).toString());
        }
    }

    /**
     * Tests {@link NoidMinter#hasNext() hasNext}.
     */
    @Test
    public final void testHasNextTrue() {
        final NoidMinter minter = new NoidMinter(myNamespace, NoidType.NUMERIC, 2);

        for (int index = 0; index < 100; index++) {
            assertTrue(minter.hasNext());
            minter.next();
        }

        assertFalse(minter.hasNext());
    }

    /**
     * Tests {@link NoidMinter#hasNext() hasNext}.
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public final void testHasNextFalse() {
        final NoidMinter minter = new NoidMinter(myNamespace, NoidType.NUMERIC, 2);

        for (int index = 0; index < 125; index++) {
            minter.next();
        }

        assertFalse(minter.hasNext());
        minter.next();
    }

    /**
     * Tests {@link NoidMinter#next() next}.
     */
    @Test
    public final void testNext() {
        final NoidMinter minter = new NoidMinter(myNamespace, NoidType.NUMERIC, 2);
        final String previousNoid = minter.next();

        assertNotEquals(previousNoid, minter.next());
    }

    /**
     * Tests {@link NoidMinter#next() next}.
     */
    @Test
    public final void testShoulder() {
        final NoidMinter minter = new NoidMinter(myNamespace, NoidType.NUMERIC, TEST_SHOULDER, 2);
        assertTrue(minter.next().startsWith(TEST_SHOULDER));
    }

    /**
     * Tests {@link NoidMinter#remove() remove}.
     */
    @Test(expected = UnsupportedOperationException.class)
    public final void testRemove() {
        new NoidMinter(myNamespace, NoidType.NUMERIC, 1).remove();
    }

    /**
     * Tests {@link NoidMinter#toString() toString}.
     */
    @Test
    public final void testToString() {
        final NoidMinter minter = new NoidMinter(myNamespace, NoidType.NUMERIC, 3);
        final int charInt = Integer.parseInt(minter.next().substring(0, 1));
        final String array = StringUtils.format("[{}, {}, {}]", charInt, charInt, charInt);

        assertEquals(StringUtils.format("NoidMinter=[#1, {}, bits: 10000]", array), minter.toString());
    }

}
