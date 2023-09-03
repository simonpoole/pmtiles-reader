package ch.poole.geo.pmtiles;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import org.jetbrains.annotations.NotNull;

public final class Util {

    private Util() {
        throw new IllegalStateException("Utility class, can't be instantiated");
    }

    /**
     * De-compress a ByteBuffer with the specified compression method
     * 
     * @param buffer the ByteBuffer to de-compress
     * @param compression the compression method (see Constants)
     * @return either the original buffer is no de-compression is needed or the de-compressed data
     * @throws IOException if de-compressing goes wrong
     */
    static ByteBuffer decompress(@NotNull ByteBuffer buffer, byte compression) throws IOException {
        if (compression != Constants.COMPRESSION_NONE) {
            try (ByteArrayInputStream bis = new ByteArrayInputStream(buffer.array())) {
                switch (compression) {
                case Constants.COMPRESSION_GZIP:
                    buffer = copy(new GZIPInputStream(bis));
                    break;
                case Constants.COMPRESSION_ZSTD:
                    buffer = copy(new InflaterInputStream(bis));
                    break;
                default:
                    throw new UnsupportedOperationException("Internal compression " + compression + " not supported");
                }
            }
        }
        return buffer;
    }

    /**
     * Copy an InputStream to a ByteBuffer
     * 
     * @param is the InputStream
     * @return a ByteBuffer with the contents of the InputStream
     * @throws IOException if reading or writing fails
     */
    @NotNull
    private static ByteBuffer copy(@NotNull InputStream is) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] temp = new byte[1024];
            int len;
            while ((len = is.read(temp)) != -1) {
                bos.write(temp, 0, len);
            }
            return ByteBuffer.wrap(bos.toByteArray());
        }
    }
}
