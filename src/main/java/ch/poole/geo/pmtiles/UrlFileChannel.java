package ch.poole.geo.pmtiles;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.jetbrains.annotations.NotNull;

/**
 * Wrapper around an HttpURLConnection to implement a read-only FileChannel
 * 
 * @author simon
 *
 */
public class UrlFileChannel extends FileChannel {

    private static final String RANGE_HEADER = "Range";

    private static final String OPERATION_NOT_SUPPORTED = "Operation not supported";

    private long      position = 0;
    private final URL url;

    public UrlFileChannel(@NotNull URL url) {
        this.url = url;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        int count = read(dst, position);
        position += count;
        return count;
    }

    @Override
    public int read(ByteBuffer dst, long pos) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(true);
        final int capacity = dst.capacity();
        conn.setRequestProperty(RANGE_HEADER, "bytes=" + pos + "-" + (pos + capacity - 1));
        try (InputStream is = conn.getInputStream()) {
            dst.rewind();
            int offset = 0;
            int count = 0;
            int remaining = capacity;
            while (remaining > 0 && (count = is.read(dst.array(), offset, remaining)) != -1) {
                remaining -= count;
                offset += count;
            }
            return capacity - remaining;
        }
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED);
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED);
    }

    @Override
    public long position() throws IOException {
        return position;
    }

    @Override
    public FileChannel position(long newPosition) throws IOException {
        position = newPosition;
        return this;
    }

    @Override
    public long size() throws IOException {
        throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED);
    }

    @Override
    public FileChannel truncate(long size) throws IOException {
        throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void force(boolean metaData) throws IOException {
        throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED);
    }

    @Override
    public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
        throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED);
    }

    @Override
    public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
        throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED);
    }

    @Override
    public int write(ByteBuffer src, long position) throws IOException {
        throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED);
    }

    @Override
    public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
        throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED);
    }

    @Override
    public FileLock lock(long position, long size, boolean shared) throws IOException {
        throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED);
    }

    @Override
    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED);
    }

    @Override
    protected void implCloseChannel() throws IOException {
        // likely doing nothing here is correct
    }
}
