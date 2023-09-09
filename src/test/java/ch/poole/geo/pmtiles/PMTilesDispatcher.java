package ch.poole.geo.pmtiles;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;

public class PMTilesDispatcher extends Dispatcher {
    static final String RANGE_HEADER = "Range";
    private static final String ETAG_HEADER  = "ETag";

    private static final Pattern RANGE_PATTERN = Pattern.compile("^bytes=([0-9]+)-([0-9]+)");

    private final FileChannel channel;
    private String            eTag = "1234";

    /**
     * Construct a new dispatcher that will return tiles from a PMTiles source
     * 
     * @param file the source
     * @throws IOException if the source can't copied out of the assets and opened
     */
    public PMTilesDispatcher(@NotNull File file) throws IOException {
        try {
            if (!file.exists()) {
                throw new IOException(file.getAbsolutePath() + " doesn't exist");
            }
            channel = new FileInputStream(file).getChannel();
        } catch (IOException e) {
            throw e;
        }
    }

    /**
     * Set the ETag header to use
     * 
     * @param tag value for ETag
     */
    public void setEtag(@NotNull String tag) {
        eTag = tag;
    }

    @Override
    public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
        try (Buffer data = new Buffer()) {
            Matcher matcher = RANGE_PATTERN.matcher(request.getHeader(RANGE_HEADER));
            if (matcher.find()) {
                long start = Long.parseLong(matcher.group(1));
                long end = Long.parseLong(matcher.group(2));
                ByteBuffer buffer = ByteBuffer.allocate((int) (end - start + 1));
                channel.read(buffer, start);
                if (buffer.capacity() != 0) {
                    data.write(buffer.array());
                    final MockResponse response = new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(data);
                    response.setHeader(ETAG_HEADER, eTag);
                    return response;
                }
            }
            return new MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND);
        } catch (IOException | NumberFormatException e) {
            return new MockResponse().setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
        }
    }

    @Override
    public void shutdown() {
        try {
            channel.close();
        } catch (IOException e) {
            // Ignore
        }
    }
}
