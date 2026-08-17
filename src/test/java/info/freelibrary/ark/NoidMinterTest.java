
package info.freelibrary.ark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.Stopwatch;
import info.freelibrary.util.StringUtils;
import io.vertx.core.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * A test of the NOID minter.
 */
public class NoidMinterTest {

    /** The logger used in testing. */
    private static final Logger LOGGER = LoggerFactory.getLogger(NoidMinterTest.class, MessageCodes.BUNDLE);

    /** The file extension for the NOID database. */
    private static final String NAF_EXT = ".naf";

    /** A test shoulder. */
    private static final String TEST_SHOULDER = "f3";

    /** A minter namespace to be used in the test. */
    private String myNamespace;

    /**
     * Sets up test.
     */
    @BeforeEach
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
        final File dbFile = Files.createTempFile(UUID.randomUUID().toString(), NAF_EXT).toFile();
        final Stopwatch stopwatch = new Stopwatch().start();

        dbFile.deleteOnExit();

        try (FileWriter writer = new FileWriter(dbFile);
                NoidMinter minter = new NoidMinter(myNamespace, NoidType.ALPHA, 6)) {
            final long expectedCount = minter.getSize();
            long count = 0;

            while (minter.hasNext()) {
                writer.write(minter.next().result());
                writer.write(System.lineSeparator());
                count++;
            }

            assertEquals(expectedCount, count);
            LOGGER.info(MessageCodes.ARK_009, "244,140,625", stopwatch.stop().getSeconds());
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
        final File serFile = Files.createTempFile(UUID.randomUUID().toString(), NAF_EXT).toFile();
        final NoidMinter minter = new NoidMinter(myNamespace, NoidType.NUMERIC, 5);
        final String minterState;

        serFile.deleteOnExit();

        // Increment the minter so it's minted some NOIDs
        for (int index = 0; index < 26; index++) {
            assertTrue(minter.next().succeeded()); // Always succeeds in NoidMinter
        }

        minterState = minter.toString();

        // Serialize the minter to disk
        try (ObjectOutputStream outStream = new ObjectOutputStream(new FileOutputStream(serFile))) {
            outStream.writeObject(minter);
        }

        try (ObjectInputStream inStream = new ObjectInputStream(new FileInputStream(serFile))) {
            assertEquals(minterState, ((NoidMinter) inStream.readObject()).toString());
        }
    }

    /**
     * Tests {@link NoidMinter#hasNext() hasNext}.
     *
     * @throws IOException If there is an error
     */
    @Test
    public final void testHasNextTrue() throws IOException {
        try (NoidMinter minter = new NoidMinter(myNamespace, NoidType.NUMERIC, 2)) {
            for (int index = 0; index < 100; index++) {
                assertTrue(minter.hasNext());
                assertTrue(minter.next().succeeded());
            }

            assertFalse(minter.hasNext());
        }
    }

    /**
     * Tests {@link NoidMinter#hasNext() hasNext}.
     *
     * @throws IOException If there is an error
     */
    @Test
    public final void testHasNextFalse() throws IOException {
        try (NoidMinter minter = new NoidMinter(myNamespace, NoidType.NUMERIC, 2)) {
            final Future<String> exhausted;

            while (minter.hasNext()) {
                assertTrue(minter.next().succeeded());
            }

            assertFalse(minter.hasNext());
            exhausted = minter.next();
            assertTrue(exhausted.failed());
            assertInstanceOf(NoSuchElementException.class, exhausted.cause());
        }
    }

    /**
     * Tests {@link NoidMinter#next() next}.
     *
     * @throws IOException If there is an error
     */
    @Test
    public final void testNext() throws IOException {
        try (NoidMinter minter = new NoidMinter(myNamespace, NoidType.NUMERIC, 2)) {
            final String previousNoid = minter.next().result();
            assertNotEquals(previousNoid, minter.next().result());
        }
    }

    /**
     * Tests {@link NoidMinter#next() next}.
     *
     * @throws IOException If there is an error
     */
    @Test
    public final void testShoulder() throws IOException {
        try (NoidMinter minter = new NoidMinter(myNamespace, NoidType.NUMERIC, TEST_SHOULDER, 2)) {
            assertTrue(minter.next().result().startsWith(TEST_SHOULDER));
        }
    }

    /**
     * Tests {@link NoidMinter#toString() toString}.
     *
     * @throws IOException If there is an error
     */
    @Test
    public final void testToString() throws IOException {
        try (NoidMinter minter = new NoidMinter(myNamespace, NoidType.NUMERIC, 3)) {
            final int charInt = Integer.parseInt(minter.next().await().substring(0, 1));
            final String array = StringUtils.format("[{}, {}, {}]", charInt, charInt, charInt);
            final String expected = LOGGER.getMessage(MessageCodes.ARK_008, NoidMinter.class.getSimpleName(),
                    myNamespace, NoidType.NUMERIC, 3, "<null>", false, 1, minter.getSize(), true, array, "10000");

            assertEquals(expected, minter.toString());
        }
    }
}
