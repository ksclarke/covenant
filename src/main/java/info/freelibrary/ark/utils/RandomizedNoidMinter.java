
package info.freelibrary.ark.utils;

import info.freelibrary.ark.Config;
import info.freelibrary.ark.MessageCodes;
import info.freelibrary.ark.NoidType;
import info.freelibrary.util.I18nRuntimeException;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.Stopwatch;
import info.freelibrary.util.warnings.PMD;
import io.netty.util.internal.ThreadLocalRandom;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serial;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * A NOID minter that randomizes its NOIDs.
 * <p>
 * To randomize, the minter first writes all possible NOIDs to an array file (which might take some time, depending on
 * NOID type and length) and then uses a modified <a href="https://stackoverflow.com/a/29158495/171452">LCG
 * algorithm</a> to iterate over all possible indices. For each index position, the minter reads the NOID associated
 * with that index position from the file. If a shoulder and checksum are desired, these are added to the NOID before it
 * is returned by the minter.
 * </p>
 */
public class RandomizedNoidMinter extends NoidMinter implements Serializable, AutoCloseable {

    /** The logger for RandomizedNoidMinter. */
    private static final Logger LOGGER = LoggerFactory.getLogger(RandomizedNoidMinter.class, MessageCodes.BUNDLE);

    /** The <code>serialVersionUID</code> for RandomizedNoidMinter. */
    @Serial
    private static final long serialVersionUID = 2_138_657_523_643_562_090L;

    /** The linear congruential generator (LCG) increment (often referred to as 'c'). */
    private static final int DEFAULT_INCREMENT = 1_013_904_223;

    /** The LCG multiplier (often referred to as 'a'). */
    private static final int DEFAULT_MULTIPLIER = 1_664_525;

    /** The byte buffer into which NOIDs are read. */
    private final ByteBuffer myByteBuffer;

    /** The total number of possible NOIDs for this minter. */
    private final long myTotalNoidCount;

    /** The length of NOIDs in the NAF. */
    private final int myNoidLength;

    /** The key file associated with this minter type. */
    private final Path myKeyFile;

    /** The LCG modulus (often referred to as 'm'). */
    private final long myModulus;

    /** The LCG seed. */
    private final long mySeed;

    /** The NOID array file. */
    private AsynchronousFileChannel myNAF;

    /** If the minter has a next NOID. */
    private boolean hasNextNOID = true;

    /** The next value in the LCG sequence. */
    private long myIndex;

    /**
     * Creates a new randomized NOID minter.
     *
     * @param aNamespace A namespace for the newly created minter
     * @param aNoidType A type of NOID to be minted
     * @param aNoidLength The length of NOIDs to be minted (minus shoulder and checksum character)
     * @throws IOException If there is trouble reading or writing NOIDs from a random access file
     */
    public RandomizedNoidMinter(final String aNamespace, final NoidType aNoidType, final int aNoidLength)
            throws IOException {
        this(aNamespace, aNoidType, null, aNoidLength);
    }

    /**
     * Creates a new randomized NOID minter.
     *
     * @param aNamespace A namespace for the newly created minter
     * @param aNoidType A type of NOID to be minted
     * @param aNoidLength The length of NOIDs to be minted (minus shoulder and checksum character)
     * @param aChecksumRequired True if a checksum should be generated; else, false
     * @throws IOException If there is trouble reading or writing NOIDs from a random access file
     */
    public RandomizedNoidMinter(final String aNamespace, final NoidType aNoidType, final int aNoidLength,
            final boolean aChecksumRequired) throws IOException {
        this(aNamespace, aNoidType, null, aNoidLength, aChecksumRequired);
    }

    /**
     * Creates a new randomized NOID minter.
     *
     * @param aNamespace A namespace for the newly created minter
     * @param aNoidType A type of NOID to be minted
     * @param aShoulder A shoulder for the minted NOID
     * @param aNoidLength The length of NOIDs to be minted (minus shoulder and checksum character)
     * @throws IOException If there is trouble reading or writing NOIDs from a random access file
     */
    public RandomizedNoidMinter(final String aNamespace, final NoidType aNoidType, final String aShoulder,
            final int aNoidLength) throws IOException {
        this(aNamespace, aNoidType, aShoulder, aNoidLength, true);
    }

    /**
     * Creates a new randomized NOID minter.
     *
     * @param aNamespace A namespace for the newly created minter
     * @param aNoidType A type of NOID to be minted
     * @param aShoulder A shoulder for the minted NOID
     * @param aNoidLength The length of NOIDs to be minted (minus shoulder and checksum character)
     * @param aChecksumRequired Whether the NOID should have a checksum character at the end
     * @throws IOException If there is trouble reading or writing NOIDs from a random access file
     * @throws IllegalArgumentException If there was a request for more NOIDs than are possible
     */
    public RandomizedNoidMinter(final String aNamespace, final NoidType aNoidType, final String aShoulder,
            final int aNoidLength, final boolean aChecksumRequired) throws IOException {
        super(aNamespace, aNoidType, null, aNoidLength, false);

        // The unique name for this minter
        final String minterName = aNoidType.toString() + '-' + aNoidLength;

        // The length of NOIDs generated by this minter
        myNoidLength = aNoidLength;

        // Get the total number of NOIDs that this minter can mint
        myTotalNoidCount = aNoidType.getNoidCount(aNoidLength);

        // Set a max on the number of NOIDs we can randomize
        if (myTotalNoidCount <= 0 || myTotalNoidCount > Math.pow(2, 62)) {
            throw new IllegalArgumentException(LOGGER.getMessage(MessageCodes.ARK_026, myTotalNoidCount));
        }

        // The .naf extension is for our "NOID array file" format; it's just a fixed size array of bare NOIDs
        myKeyFile = Paths.get(getDbFilesDir(), minterName + ".naf");

        // If key file does not already exist, create it; this may take a while, depending on NOID type/length
        if (!myKeyFile.toFile().exists()) {
            createKeyFile(aNoidLength);
        }

        // Create a byte buffer for NOIDs we read from the NAF
        myByteBuffer = ByteBuffer.allocate(myNoidLength);

        // Initialize the linear congruential generator (LCG)
        myModulus = (long) Math.pow(2, Math.ceil(Math.log(myTotalNoidCount) / Math.log(2)));
        myIndex = mySeed = ThreadLocalRandom.current().nextInt((int) Math.min(myTotalNoidCount, Integer.MAX_VALUE));

        // We can set these now that we've used the underlying minter to give us the bare NOIDs
        hasChecksums = aChecksumRequired;
        myShoulder = aShoulder;
    }

    @Override
    public long getIndex() {
        return myIndex;
    }

    @Override
    public boolean hasNext() {
        return hasNextNOID;
    }

    @Override
    public String next() {
        if (!hasNextNOID) {
            throw new NoSuchElementException();
        }

        myIndex = (DEFAULT_MULTIPLIER * myIndex + DEFAULT_INCREMENT) % myModulus;

        while (myIndex >= myTotalNoidCount) {
            myIndex = (DEFAULT_MULTIPLIER * myIndex + DEFAULT_INCREMENT) % myModulus;
        }

        if (myIndex == mySeed) {
            hasNextNOID = false;
        }

        return getNoidFromNAF(myIndex);
    }

    /**
     * Gets the bare NOID at the supplied index position from the underlying sequential NAF. This index is not the index
     * of the randomized minter, but a sequential index position of the NOID in the NOID array file.
     *
     * @param aIndex An index position to use when retrieving from the NAF
     * @return The requested NOID
     */
    public String getNOID(final long aIndex) {
        return getNoidFromNAF(aIndex);
    }

    @Override
    public String toString() {
        return LOGGER.getMessage(MessageCodes.ARK_028, RandomizedNoidMinter.class.getSimpleName(), myIndex, myKeyFile);
    }

    @Override
    public void close() throws Exception {
        if (myNAF != null && myNAF.isOpen()) {
            myNAF.close();
        }
    }

    /**
     * Gets a NOID from the NAF.
     *
     * @param aIndex An index position to use when retrieving a NOID from the NAF
     * @return The requested NOID
     * @throws NoSuchElementException If the requested NOID isn't available
     * @throws I18nRuntimeException If there is trouble executing the retrieval
     */
    @SuppressWarnings({ PMD.CYCLOMATIC_COMPLEXITY })
    private String getNoidFromNAF(final long aIndex) {
        try {
            final int bytesRead;

            if (myNAF == null || !myNAF.isOpen()) {
                myNAF = AsynchronousFileChannel.open(myKeyFile, StandardOpenOption.READ);
            }

            bytesRead = myNAF.read(myByteBuffer.clear(), aIndex * myNoidLength).get();

            if (bytesRead == myNoidLength) {
                return mint(new String(myByteBuffer.array(), StandardCharsets.UTF_8));
            } else {
                final IOException cause = new IOException(LOGGER.getMessage(MessageCodes.ARK_024, aIndex));
                throw new NoSuchElementException(LOGGER.getMessage(MessageCodes.ARK_023), cause);
            }
        } catch (final InterruptedException details) {
            Thread.currentThread().interrupt();
            throw new I18nRuntimeException(details);
        } catch (final ExecutionException details) {
            throw new I18nRuntimeException(details);
        } catch (final IOException details) {
            throw new NoSuchElementException(details.getMessage(), details);
        }
    }

    /**
     * Get the directory for database files from: 1) a <code>DB_FILES_DIR</code> environmental property, or 2) a
     * <code>db.files.dir</code> system property, or 3) the <code>java.io.tmpdir</code> system property.
     *
     * @return The location of the directory used for database files
     */
    private String getDbFilesDir() {
        final String dbDirSysProperty = System.getProperty(Config.DB_FILES_DIR);
        final String dbDirEnvProperty = System.getenv(Config.DB_FILES_DIR.toUpperCase(Locale.US).replace(".", "_"));

        if (dbDirEnvProperty == null && dbDirSysProperty == null) {
            return System.getProperty("java.io.tmpdir");
        } else {
            return Objects.requireNonNullElse(dbDirEnvProperty, dbDirSysProperty);
        }
    }

    /**
     * Creates a key file for our randomized minter. Keeping billions of NOIDs in memory isn't great, but, to randomize
     * them without doing that, we'll need something to iterate over. This method writes a NAF (NOID array file) to disk
     * so that we can use that to iterate over base NOID values, adding a requested shoulder and/or checksum as needed.
     *
     * @param aNoidLength A length of NOIDs in this key file
     * @throws FileNotFoundException If the path to the supplied file cannot be found
     * @throws IOException If there is trouble reading or writing to the key file
     */
    private void createKeyFile(final int aNoidLength) throws FileNotFoundException, IOException {
        try (RandomAccessFile file = new RandomAccessFile(myKeyFile.toFile(), "rw");
                FileChannel fileChannel = file.getChannel()) {
            final NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
            final String totalNoidCount = formatter.format(myTotalNoidCount);
            final long totalBytes = aNoidLength * myTotalNoidCount;
            final int iterationByteCount;
            final int iterationCount;
            final Stopwatch timer;

            long startIndex = 0;
            MappedByteBuffer byteBuffer;

            // Find out if we're using the max integer size or something smaller for our batch size
            if (totalBytes < Integer.MAX_VALUE) {
                iterationByteCount = (int) totalBytes;
                iterationCount = 1;
            } else {
                iterationByteCount = Integer.MAX_VALUE - 1;
                iterationCount = (int) Math.ceil(totalBytes / (double) iterationByteCount);
            }

            fileChannel.truncate(0);
            timer = new Stopwatch().start();

            for (int index = 0; index < iterationCount; index++) {
                LOGGER.debug(MessageCodes.ARK_021, startIndex, iterationByteCount);
                byteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, startIndex, iterationByteCount);

                for (int byteCount = iterationByteCount; hasMoreBytes(byteCount); byteCount -= aNoidLength) {
                    startIndex += aNoidLength;
                    byteBuffer.put(StandardCharsets.UTF_8.encode(super.next()));
                }
            }

            fileChannel.truncate(startIndex);
            LOGGER.info(MessageCodes.ARK_020, totalNoidCount, myKeyFile, timer.stop().getSeconds());
        }
    }

    /**
     * Determines whether there are more bytes to be read by checking the iterator and the byte count.
     *
     * @param aByteCount A number of bytes to be read
     * @return True if there are more bytes to be read; else, false
     */
    private boolean hasMoreBytes(final int aByteCount) {
        return super.hasNext() && aByteCount > 0;
    }
}
