
package info.freelibrary.ark.utils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import info.freelibrary.ark.MessageCodes;
import info.freelibrary.ark.NoidType;

/**
 * A serializable minter of sequential NOIDs.
 */
public class NoidMinter implements Iterator<String>, Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoidMinter.class, MessageCodes.BUNDLE);

    // We warn if a shoulder doesn't adhere to the "First Digit" convention.
    private static final Pattern FIRST_DIGIT_PATTERN = Pattern.compile("^[a-zA-Z]+[0-9]$");

    /**
     * The <code>serialVersionUID</code> for NoidMinter.
     */
    private static final long serialVersionUID = -3059924955130497273L;

    protected boolean hasChecksums;

    protected String myShoulder;

    private final List<Character> myCharacters;

    private final List<Character> myCurrentNoidChars;

    private final int myNoidLength;

    private final int[] myBitArray;

    private final NoidType myNoidType;

    private final String myNamespace;

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
        myCurrentNoidChars = new ArrayList<>(Collections.nCopies(aNoidLength, myCharacters.get(0)));
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
    public boolean hasNext() {
        return myBitArray[myNoidLength] != 1;
    }

    /**
     * Gets a batch of NOIDs. It's possible that there aren't enough NOIDs left to satisfy the request; in this case,
     * the amount of NOIDs that can be returned are. The caller is responsible for checking the list size to confirm the
     * number of returned NOIDs.
     *
     * @param aCount A requested number of NOIDs
     * @return A list containing the requested NOIDs
     */
    public List<String> next(final int aCount) {
        final List<String> noids = new ArrayList<>();

        for (int count = aCount; count > 0; count--) {
            if (!hasNext()) {
                break; // We return what we can
            }

            noids.add(next());
        }

        return noids;
    }

    @Override
    public String next() {
        final StringBuilder builder = new StringBuilder();

        int bitIndex = 0;

        if (!hasNext()) {
            throw new IndexOutOfBoundsException(LOGGER.getMessage(MessageCodes.ARK_010));
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
                myCurrentNoidChars.forEach(character -> builder.append(character));

                // Reverse the string so NOIDs can be sorted in ascending order
                return mint(builder.reverse().toString());
            }

            bitIndex++;
        }

        myBitArray[bitIndex]++;
        myCurrentNoidChars.forEach(character -> builder.append(character));

        // Reverse the string so NOIDs can be sorted in ascending order
        return mint(builder.reverse().toString());
    }

    /**
     * Gets the total number of NOIDs this minter can mint.
     *
     * @return The total number of NOIDs this minter can mint
     */
    public long getSize() {
        return myNoidType.getNoidCount(myNoidLength);
    }

    /**
     * Gets the index position of the current NOID.
     *
     * @return The index position of the current NOID
     */
    public long getIndex() {
        return myIndex;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return LOGGER.getMessage(MessageCodes.ARK_019, NoidMinter.class.getSimpleName(), myIndex, myCurrentNoidChars,
                getBitArray());
    }

    /**
     * Gets the NOID minter's namespace.
     *
     * @return The namespace of the minter
     */
    public String getNamespace() {
        return myNamespace;
    }

    /**
     * Returns a newly minted NOID.
     *
     * @param aNOID A basic NOID string (without checksum or shoulder)
     * @param aShoulder A NOID shoulder
     * @param aChecksumRequired True if a checksum is desired; else, false
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
     * A string representation of the internal bit array.
     *
     * @return A string representation of the internal bit array
     */
    private String getBitArray() {
        return Arrays.stream(myBitArray).mapToObj(Integer::toString).collect(Collectors.joining());
    }
}
