package ch.poole.geo.pmtiles;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;

import org.jetbrains.annotations.NotNull;

/**
 * Wrapper around an HttpURLConnection to implement a read-only FileChannel
 * 
 * @author simon
 *
 */
public class HttpUrlConnectionChannel extends UrlFileChannel {

    private final URL url;
    private String savedETag = null;

    public HttpUrlConnectionChannel(@NotNull URL url) {
        this.url = url;
    }

    @Override
    public int read(ByteBuffer dst, long pos) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(true);
        final int capacity = dst.capacity();
        conn.setRequestProperty(RANGE_HEADER, "bytes=" + pos + "-" + (pos + capacity - 1));
        try (InputStream is = conn.getInputStream()) {
            String eTag = conn.getHeaderField(ETAG_HEADER);
            if (eTag != null) {
                if (savedETag != null && !eTag.equals(savedETag)) {
                    throw new SourceChangedException();
                }
                savedETag = eTag;
            }
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
}
