
package info.freelibrary.ark;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import info.freelibrary.ark.utils.ChecksumUtils;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;
import io.vertx.core.Future;
import org.jetbrains.annotations.NotNull;

/**
 * A serializable minter of sequential NOIDs.
 */
public class NoidMinter implements Minter, Serializable {

    /** The logger for NoidMinter. */
    private static final Logger LOGGER = LoggerFactory.getLogger(NoidMinter.class, MessageCodes.BUNDLE);

    /** We warn if a shoulder doesn't adhere to the "First Digit" convention. */
    private static final Pattern FIRST_DIGIT_PATTERN = Pattern.compile("^[a-zA-Z]+[0-9]$");

    /** The <code>serialVersionUID</code> for NoidMinter. */
    @Serial
    private static final long serialVersionUID = -3059924955130497273L;

    /** Whether the NOIDs minted have checksums. */
    protected boolean hasChecksums;

    /** The shoulder for the NOIDs minted. */
    protected String myShoulder;

    /** The characters available for constructing NOIDs of this minter's configured type. */
    private final List<Character> myCharacters;

    /** The character values that make up the current bare NOID before shoulder or checksum processing. */
    private final List<Character> myCurrentNoidChars;

    /** The length of the bare NOIDs minted, excluding any shoulder or checksum character. */
    private final int myNoidLength;

    /** The internal counter-array used to track the current position in the NOID character space. */
    private final int[] myBitArray;

    /** The configured NOID type that determines the characters used by this minter. */
    private final NoidType myNoidType;

    /** The namespace associated with this minter. */
    private final String myNamespace;

    /** The number of NOIDs minted by this minter. */
    private long myIndex;

    /**
     * Creates a new NOID minter.
     *
     * @param aNamespace A namespace name for the IDs minted
     * @param aNoidType A type of NOID to be minted
     * @param aNoidLength A maximum NOID length
     */
    public NoidMinter(final String aNamespace, final NoidType aNoidType, final int aNoidLength) {
        this(aNamespace, aNoidType, null, aNoidLength);
    }

    /**
     * Creates a new NOID minter.
     *
     * @param aNamespace A namespace name for the IDs minted
     * @param aNoidType A type of NOID to be minted
     * @param aShoulder A shoulder to add to the NOID
     * @param aNoidLength A maximum NOID length
     */
    public NoidMinter(final String aNamespace, final NoidType aNoidType, final String aShoulder,
            final int aNoidLength) {
        this(aNamespace, aNoidType, aShoulder, aNoidLength, false);
    }

    /**
     * Creates a new NOID minter.
     *
     * @param aNamespace A namespace name for the IDs minted
     * @param aNoidType A type of NOID to be minted
     * @param aShoulder A shoulder to add to the NOID
     * @param aNoidLength A maximum NOID length
     * @param aChecksumRequired True if the NOID returned should have a checksum character at the end
     */
    public NoidMinter(final String aNamespace, final NoidType aNoidType, final String aShoulder, final int aNoidLength,
            final boolean aChecksumRequired) {
        checkArgument(aNoidLength > 0 && aNoidLength < 128, LOGGER.getMessage(MessageCodes.ARK_001));
        checkNotNull(aNoidType, LOGGER.getMessage(MessageCodes.ARK_006));
        checkNotNull(aNamespace, LOGGER.getMessage(MessageCodes.ARK_018));

        myCharacters = Arrays.asList(aNoidType.getCharacters());
        myCurrentNoidChars = new ArrayList<>(Collections.nCopies(aNoidLength, myCharacters.getFirst()));
        myBitArray = new int[aNoidLength + 2];
        hasChecksums = aChecksumRequired;
        myNoidLength = aNoidLength;
        myNamespace = aNamespace;
        myShoulder = aShoulder;
        myNoidType = aNoidType;
        myIndex = 0;

        // Warn if the supplied shoulder doesn't conform to the "first digit" convention
        if (aShoulder != null && !FIRST_DIGIT_PATTERN.matcher(aShoulder).matches()) {
            LOGGER.warn(MessageCodes.ARK_011, aShoulder);
        }
    }

    @Override
    public boolean usesChecksums() {
        return hasChecksums;
    }

    @Override
    @NotNull
    public Optional<String> getShoulder() {
        return Optional.ofNullable(myShoulder);
    }

    @Override
    @NotNull
    public NoidType getNoidType() {
        return myNoidType;
    }

    @Override
    public int getNoidLength() {
        return myNoidLength;
    }

    @Override
    public boolean hasNext() {
        return myBitArray[myNoidLength] != 1;
    }

    @Override
    @NotNull
    public Future<List<String>> next(final int aCount) {
        final List<String> noidList = new ArrayList<>();

        if (aCount <= 0) {
            throw new IndexOutOfBoundsException(aCount);
        }

        for (int count = aCount; count > 0; count--) {
            if (!hasNext()) {
                break; // We return what we can
            }

            noidList.add(nextNoid());
        }

        return Future.succeededFuture(noidList);
    }

    @Override
    @NotNull
    public Future<String> next() {
        if (!hasNext()) {
            return Future.failedFuture(new NoSuchElementException(LOGGER.getMessage(MessageCodes.ARK_010)));
        }

        return Future.succeededFuture(nextNoid());
    }

    /**
     * Gets the total number of NOIDs this minter can mint.
     *
     * @return The total number of NOIDs this minter can mint
     */
    @Override
    public long getSize() {
        return myNoidType.getNoidCount(myNoidLength);
    }

    /**
     * Gets the index position of the current NOID.
     *
     * @return The index position of the current NOID
     */
    @Override
    public long getIndex() {
        return myIndex;
    }

    @Override
    public String toString() {
        return LOGGER.getMessage(MessageCodes.ARK_008, NoidMinter.class.getSimpleName(), myNamespace, myNoidType,
                myNoidLength, myShoulder == null ? "<null>" : myShoulder, hasChecksums, myIndex, getSize(), hasNext(),
                myCurrentNoidChars, getBitArray());
    }

    /**
     * Gets the NOID minter's namespace.
     *
     * @return The namespace of the minter
     */
    @Override
    @NotNull
    public String getNamespace() {
        return myNamespace;
    }

    @Override
    public void close() throws IOException {
        // This is intentionally left empty; nothing to close here
    }

    /**
     * Returns a newly minted NOID.
     *
     * @param aNOID A basic NOID string (without checksum or shoulder)
     * @return A newly minted NOID
     */
    protected String mint(final String aNOID) {
        if (myShoulder != null) {
            if (hasChecksums) {
                return ChecksumUtils.appendChecksum(myShoulder + aNOID, myNoidType);
            } else {
                return myShoulder + aNOID;
            }
        } else if (hasChecksums) {
            return ChecksumUtils.appendChecksum(aNOID, myNoidType);
        } else {
            return aNOID;
        }
    }

    /**
     * Mints the next NOID.
     *
     * @return The next minted NOID
     * @throws NoSuchElementException If there are no NOIDs left to mint
     */
    private String nextNoid() {
        final StringBuilder builder = new StringBuilder();

        int bitIndex = 0;

        if (!hasNext()) {
            throw new NoSuchElementException(LOGGER.getMessage(MessageCodes.ARK_010));
        }

        myIndex++;

        for (int index = myNoidLength - 1; index >= 0; index--) {
            myCurrentNoidChars.set(index, myCharacters.get(myBitArray[index]));
        }

        while (myBitArray[bitIndex] == myCharacters.size() - 1) {
            if (bitIndex < myNoidLength + 1) {
                myBitArray[bitIndex] = 0;
            } else {
                myBitArray[myNoidLength] = 1;
                myCurrentNoidChars.forEach(builder::append);

                // Reverse the string so NOIDs can be sorted in ascending order
                return mint(builder.reverse().toString());
            }

            bitIndex++;
        }

        myBitArray[bitIndex]++;
        myCurrentNoidChars.forEach(builder::append);

        // Reverse the string so NOIDs can be sorted in ascending order
        return mint(builder.reverse().toString());
    }

    /**
     * A string representation of the internal bit array.
     *
     * @return A string representation of the internal bit array
     */
    private String getBitArray() {
        return Arrays.stream(myBitArray).mapToObj(Integer::toString).collect(Collectors.joining());
    }
}
