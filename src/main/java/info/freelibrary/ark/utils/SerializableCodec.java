
package info.freelibrary.ark.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import info.freelibrary.ark.MessageCodes;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

/**
 * A message codec for serializable objects.
 */
public class SerializableCodec<T> implements MessageCodec<T, T> {

    /* The codec's logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SerializableCodec.class, MessageCodes.BUNDLE);

    /* The name of the codec. */
    private final String myCodecName;

    /**
     * Creates a message codec for serializable objects.
     *
     * @param aSerializable A class that implements {@link Serializable}
     */
    public SerializableCodec(final Class<T> aSerializable) {
        myCodecName = aSerializable.getSimpleName() + "Codec";
    }

    @Override
    public void encodeToWire(final Buffer aBuffer, final T aSerializable) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                ObjectOutput out = new ObjectOutputStream(byteStream)) {
            final byte[] bytes;

            out.writeObject(aSerializable);
            out.flush();

            bytes = byteStream.toByteArray();

            aBuffer.appendInt(bytes.length);
            aBuffer.appendBytes(bytes);
        } catch (final IOException details) {
            LOGGER.error(details, details.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public T decodeFromWire(final int aPosition, final Buffer aBuffer) {
        final int length = aBuffer.getInt(aPosition); // getInt() == 4 bytes
        final byte[] bytes = aBuffer.getBytes(aPosition + 4, aPosition + 4 + length);

        try (ObjectInputStream byteStream = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (T) byteStream.readObject();
        } catch (IOException | ClassNotFoundException details) {
            LOGGER.error(details, details.getMessage());
        }

        return null;
    }

    @Override
    public T transform(final T aPOJO) {
        return aPOJO; // If sent locally over event bus, no translation is needed
    }

    @Override
    public String name() {
        return myCodecName;
    }

    @Override
    public byte systemCodecID() {
        return -1; // Always -1
    }

}
