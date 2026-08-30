
package info.freelibrary.ark.minters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import info.freelibrary.ark.NoidMinter;
import info.freelibrary.ark.NoidType;
import info.freelibrary.ark.util.MessageCodes;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.Stopwatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

/**
 * An integration test for the NOID minter.
 */
public class NoidMinterIT {

    /** The logger used in testing. */
    private static final Logger LOGGER = LoggerFactory.getLogger(NoidMinterIT.class, MessageCodes.BUNDLE);

    /** The file extension for the NOID database. */
    private static final String NAF_EXT = ".naf";

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
}
