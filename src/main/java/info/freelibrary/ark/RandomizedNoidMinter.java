
package info.freelibrary.ark;

import static info.freelibrary.util.Constants.SINGLE_INSTANCE;

import info.freelibrary.ark.util.Config;
import info.freelibrary.ark.util.MessageCodes;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import info.freelibrary.util.warnings.PMD;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.jspecify.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A NOID minter that randomizes its NOIDs.
 *
 * <p>
 * To randomize, the minter first writes all possible NOIDs to an array file (which might take some time, depending on
 * NOID type and length) and then uses a modified <a href="https://stackoverflow.com/a/29158495/171452">LCG
 * algorithm</a> to iterate over all possible indices. For each index position, the minter reads the NOID associated
 * with that index position from the file. If a shoulder and checksum are desired, these are added to the NOID before it
 * is returned by the minter.
 * </p>
 */
@SuppressWarnings({ PMD.GOD_CLASS, PMD.EXCESSIVE_IMPORTS })
public class RandomizedNoidMinter extends NoidMinter implements Serializable {

    /** The logger for RandomizedNoidMinter. */
    private static final Logger LOGGER = LoggerFactory.getLogger(RandomizedNoidMinter.class, MessageCodes.BUNDLE);

    /** The maximum number of NOIDs that can be randomized. */
    private static final long MAX_RANDOMIZABLE_NOID_COUNT = 1L << 62;

    /** The <code>serialVersionUID</code> for RandomizedNoidMinter. */
    @Serial
    private static final long serialVersionUID = 2_138_657_523_643_562_090L;

    /** The linear congruential generator (LCG) increment (often referred to as 'c'). */
    private static final int DEFAULT_INCREMENT = 1_013_904_223;

    /** The LCG multiplier (often referred to as 'a'). */
    private static final int DEFAULT_MULTIPLIER = 1_664_525;

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
    @Nullable
    private transient AsynchronousFileChannel myNAF;

    /** If the minter has a next NOID. */
    private volatile boolean hasNextNOID = true;

    /** The next value in the LCG sequence. */
    private volatile long myIndex;

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
    public RandomizedNoidMinter(final String aNamespace, final NoidType aNoidType, @Nullable final String aShoulder,
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
    public RandomizedNoidMinter(final String aNamespace, final NoidType aNoidType, @Nullable final String aShoulder,
            final int aNoidLength, final boolean aChecksumRequired) throws IOException {
        super(aNamespace, aNoidType, null, aNoidLength, false); // The minter requires bare NOIDs for NAF construction

        // The unique name for this minter
        final String minterName = aNoidType.toString() + '-' + aNoidLength;
        final long expectedNafSize;

        // The length of NOIDs generated by this minter
        myNoidLength = aNoidLength;

        // Get the total number of NOIDs that this minter can mint
        myTotalNoidCount = aNoidType.getNoidCount(aNoidLength);

        // Set a max on the number of NOIDs we can randomize
        if (myTotalNoidCount <= 0 || myTotalNoidCount > MAX_RANDOMIZABLE_NOID_COUNT) {
            throw new IllegalArgumentException(LOGGER.getMessage(MessageCodes.ARK_026, myTotalNoidCount));
        }

        expectedNafSize = Math.multiplyExact(myNoidLength, myTotalNoidCount);

        // The .naf extension is for our "NOID array file" format; it's just a fixed size array of bare NOIDs
        myKeyFile = Paths.get(getDbFilesDir(), minterName + ".naf");

        // If key file does not already exist, create it; this may take a while, depending on NOID type/length
        if (!isValidKeyFile(expectedNafSize)) {
            createKeyFileAtomically(aNoidLength);
        }

        myModulus = nextPowerOfTwo(myTotalNoidCount);
        myIndex = mySeed = ThreadLocalRandom.current().nextInt((int) Math.min(myTotalNoidCount, Integer.MAX_VALUE));

        // We can set these now that we've used the underlying minter to give us the bare NOIDs
        hasChecksums = aChecksumRequired;
        myShoulder = aShoulder;
    }

    /**
     * Gets the next power of two.
     *
     * @param aValue The value from which to get the next power of two
     * @return The next power of two
     */
    private static long nextPowerOfTwo(final long aValue) {
        if (aValue <= SINGLE_INSTANCE) {
            return 1L;
        }

        return Long.highestOneBit(aValue - 1L) << 1;
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
    public Future<String> next() {
        return hasNext() ? getNOID(nextIndex())
                : Future.failedFuture(new NoSuchElementException(LOGGER.getMessage(MessageCodes.ARK_010)));
    }

    @Override
    public Future<List<String>> next(final int aCount) {
        final List<Long> indexes = nextIndexes(aCount);
        final List<Future<String>> futures;

        if (aCount <= 0) {
            throw new IndexOutOfBoundsException(aCount);
        }

        if (indexes.isEmpty()) {
            return Future.succeededFuture(Collections.emptyList());
        }

        futures = indexes.stream().map(this::getNOID).toList();

        return Future.all(new ArrayList<>(futures)).map(_ -> futures.stream().map(Future::result).toList());
    }

    /**
     * Gets the bare NOID at the supplied index position from the underlying sequential NAF. This index is not the index
     * of the randomized minter, but a sequential index position of the NOID in the NOID array file.
     *
     * @param aIndex An index position to use when retrieving from the NAF
     * @return A future containing the requested NOID
     */
    public Future<String> getNOID(final long aIndex) {
        return getNoidFromNAF(aIndex);
    }

    @Override
    public String toString() {
        return LOGGER.getMessage(MessageCodes.ARK_028, RandomizedNoidMinter.class.getSimpleName(), myIndex, myKeyFile);
    }

    @Override
    public void close() throws IOException {
        if (myNAF != null && myNAF.isOpen()) {
            myNAF.close();
        }
    }

    /**
     * Gets the next randomized NAF index.
     *
     * @return The next randomized NAF index, or null if no more NOIDs are available
     * @throws NoSuchElementException If there aren't any more NOIDs available
     */
    private synchronized Long nextIndex() {
        if (!hasNextNOID) {
            throw new NoSuchElementException(LOGGER.getMessage(MessageCodes.ARK_010));
        }

        do {
            myIndex = (DEFAULT_MULTIPLIER * myIndex + DEFAULT_INCREMENT) % myModulus;
        } while (myIndex >= myTotalNoidCount);

        if (myIndex == mySeed) {
            hasNextNOID = false;
            return mySeed;
        }

        return myIndex;
    }

    /**
     * Gets the next randomized NAF indices.
     *
     * @param aCount The requested number of indices
     * @return The next randomized NAF indices
     */
    private List<Long> nextIndexes(final int aCount) {
        final List<Long> indices = new ArrayList<>();

        for (int count = aCount; count > 0; count--) {
            if (hasNextNOID) {
                indices.add(nextIndex());
            }
        }

        return indices;
    }

    /**
     * Checks whether the existing key file appears to contain the expected number of bare NOIDs.
     *
     * @param aExpectedSize The expected size of the key file in bytes
     * @return True if the key file exists and has the expected size; else, false
     * @throws IOException If there is trouble checking the key file
     */
    private boolean isValidKeyFile(final long aExpectedSize) throws IOException {
        return Files.exists(myKeyFile) && Files.size(myKeyFile) == aExpectedSize;
    }

    /**
     * Gets a NOID from the NAF.
     *
     * @param aIndex An index position to use when retrieving a NOID from the NAF
     * @return A future containing the requested NOID
     */
    private Future<String> getNoidFromNAF(final long aIndex) {
        final Promise<String> promise = Promise.promise();
        final ByteBuffer byteBuffer = ByteBuffer.allocate(myNoidLength);

        if (aIndex < 0 || aIndex >= myTotalNoidCount) {
            return Future.failedFuture(new IndexOutOfBoundsException(aIndex));
        }

        try {
            final long position = Math.multiplyExact(aIndex, myNoidLength);
            getNAF().read(byteBuffer, position, byteBuffer, new CompletionHandler<>() {

                @Override
                public void completed(final Integer aBytesRead, final ByteBuffer aByteBuffer) {
                    if (aBytesRead == myNoidLength) {
                        promise.complete(mint(new String(aByteBuffer.array(), StandardCharsets.UTF_8)));
                    } else {
                        final IOException cause = new IOException(LOGGER.getMessage(MessageCodes.ARK_024, aIndex));
                        promise.fail(new NoSuchElementException(LOGGER.getMessage(MessageCodes.ARK_023), cause));
                    }
                }

                @Override
                public void failed(final Throwable aCause, final ByteBuffer aByteBuffer) {
                    promise.fail(aCause);
                }
            });
        } catch (final ArithmeticException details) {
            return Future.failedFuture(new IndexOutOfBoundsException(aIndex));
        } catch (final IOException details) {
            promise.fail(details);
        }

        return promise.future();
    }

    /**
     * Gets the open NAF channel.
     *
     * @return The open NAF channel
     * @throws IOException If there is trouble opening the NAF channel
     */
    private synchronized AsynchronousFileChannel getNAF() throws IOException {
        if (myNAF == null || !myNAF.isOpen()) {
            myNAF = AsynchronousFileChannel.open(myKeyFile, StandardOpenOption.READ);
        }

        return myNAF;
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
     * Creates a key file by writing to a temporary file and atomically moving it into place. Because every writer
     * produces identical content for a given NOID type and length, concurrent creators can each publish their own
     * complete file, and the last completed move wins without the shared file ever being observed partially written.
     *
     * @param aNoidLength A length of NOIDs in this key file
     * @throws IOException If there is trouble writing or moving the key file
     */
    private void createKeyFileAtomically(final int aNoidLength) throws IOException {
        final Path dir = myKeyFile.toAbsolutePath().getParent();
        final Path tmpFile = Files.createTempFile(dir, myKeyFile.getFileName().toString(), ".tmp");

        try {
            createKeyFile(aNoidLength, tmpFile);

            try {
                Files.move(tmpFile, myKeyFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (final AtomicMoveNotSupportedException details) {
                // Filesystem can't guarantee an atomic move; a same-directory rename is still effectively atomic
                Files.move(tmpFile, myKeyFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmpFile);
        }
    }

    /**
     * Creates a key file for our randomized minter. Keeping billions of NOIDs in memory isn't great, but, to randomize
     * them without doing that, we'll need something to iterate over. This method writes a NAF (NOID array file) to disk
     * so that we can use that to iterate over base NOID values, adding a requested shoulder and/or checksum as needed.
     *
     * @param aNoidLength A length of NOIDs in this key file
     * @param aKeyFile The file into which the NOIDs are written
     * @throws FileNotFoundException If the path to the supplied file cannot be found
     * @throws IOException If there is trouble reading or writing to the key file
     */
    private void createKeyFile(final int aNoidLength, final Path aKeyFile) throws IOException {
        try (FileChannel fileChannel = FileChannel.open(aKeyFile, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            final long totalBytes = Math.multiplyExact(aNoidLength, myTotalNoidCount);
            final int bufferSize = (int) Math.min(totalBytes, 64 * 1024);
            final ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize + aNoidLength);

            long bytesWritten = 0;

            while (bytesWritten < totalBytes && super.hasNext()) {
                buffer.clear();

                while (buffer.remaining() >= aNoidLength && super.hasNext()) {
                    final String noid = super.next().toCompletionStage().toCompletableFuture().join();
                    final byte[] noidBytes = noid.getBytes(StandardCharsets.UTF_8);

                    buffer.put(noidBytes);
                }

                buffer.flip();

                while (buffer.hasRemaining()) {
                    bytesWritten += fileChannel.write(buffer);
                }
            }

            fileChannel.truncate(bytesWritten);
        }
    }
}
