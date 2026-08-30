
package info.freelibrary.ark;

import io.vertx.core.Future;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * A minter that asynchronously creates IDs.
 */
public interface Minter extends AutoCloseable {

    /**
     * Gets whether the minter has another ID available.
     *
     * @return True if another ID is available; else, false
     */
    boolean hasNext();

    /**
     * Gets the namespace of the minter.
     *
     * @return The namespace of the minter
     */
    String getNamespace();

    /**
     * Gets the index position of the current ID.
     *
     * @return The index position of the current ID
     */
    long getIndex();

    /**
     * Gets the total number of IDs this minter can mint.
     *
     * @return The total number of IDs this minter can mint
     */
    long getSize();

    /**
     * Gets the length of the IDs this minter can mint.
     *
     * @return The length of the IDs this minter can mint
     */
    int getNoidLength();

    /**
     * Asynchronously mints the next ID.
     *
     * @return A future containing the next minted ID
     * @throws NoSuchElementException If there are no more IDs to mint
     */
    Future<String> next();

    /**
     * Asynchronously mints a batch of IDs. If fewer than {@code aCount} IDs are available, the returned list contains
     * as many IDs as can be minted.
     *
     * @param aCount The requested number of IDs
     * @return A future containing minted IDs
     * @throws NoSuchElementException If there are no more IDs to mint
     * @throws IndexOutOfBoundsException If {@code aCount} is less than or equal to zero
     */
    Future<List<String>> next(int aCount);

    /**
     * Gets whether the minter uses checksums.
     *
     * @return True if the minter uses checksums; else, false
     */
    boolean usesChecksums();

    /**
     * Gets the shoulder of the minter.
     *
     * @return The shoulder of the minter
     */
    Optional<String> getShoulder();

    /**
     * Gets the type of Noid minted by this minter.
     *
     * @return The type of Noid minted by this minter
     */
    NoidType getNoidType();

    /**
     * Closes the minter.
     *
     * @throws IOException If there is an error while closing the minter
     */
    @Override
    void close() throws IOException;
}
