package ch.poole.geo.pmtiles;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import okhttp3.mockwebserver.MockWebServer;

public class RemoteTest {

    private MockWebServer     tileServer;
    private PMTilesDispatcher tileDispatcher;
    private String            tileUrl;

    @Before
    public void setup() {
        ClassLoader classLoader = getClass().getClassLoader();
        File testFile = new File(classLoader.getResource("protomaps(vector)ODbL_firenze.pmtiles").getFile());
        tileServer = new MockWebServer();
        try {
            tileDispatcher = new PMTilesDispatcher(testFile);
            tileServer.setDispatcher(tileDispatcher);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        tileUrl = tileServer.url("/").toString() + "firenze.pmtiles";
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        try {
            tileServer.shutdown();
        } catch (IOException e) {
            // Ignore
        }
    }

    @Test
    public void header() {
        try (Reader reader = new Reader(new HttpUrlConnectionChannel(new URL(tileUrl)))) {
            assertEquals(Reader.PMTILES_VERSION, reader.header.version);
            assertEquals(Constants.COMPRESSION_GZIP, reader.header.internalCompression);
            assertEquals(Constants.COMPRESSION_GZIP, reader.getTileCompression());
            assertEquals(Constants.TYPE_MVT, reader.getTileType());
            assertEquals(0, reader.getMinZoom());
            assertEquals(14, reader.getMaxZoom());
            assertArrayEquals(new double[] { 11.154026, 43.7270125, 11.3289395, 43.8325455 }, reader.getBounds(), 0.0000001);
            assertArrayEquals(new double[] { 11.2414827, 43.779779 }, reader.getCenter(), 0.0000001);
            assertEquals(0, reader.getCenterZoom());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void tile_13_4350_2984() {
        try (Reader reader = new Reader(new HttpUrlConnectionChannel(new URL(tileUrl)))) {
            byte[] tile = reader.getTile(13, 4350, 2984);
            assertNotNull(tile);
            byte[] d = MessageDigest.getInstance("MD5").digest(tile);
            assertArrayEquals(new byte[] { (byte) 0x3a, (byte) 0x36, (byte) 0xe9, (byte) 0xc5, (byte) 0x38, (byte) 0x7c, (byte) 0xfe, (byte) 0x8b, (byte) 0xad,
                    (byte) 0x20, (byte) 0xf7, (byte) 0xe4, (byte) 0x74, (byte) 0x84, (byte) 0xd6, (byte) 0x3d }, d);
        } catch (IOException | NoSuchAlgorithmException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void eTag() {
        try (Reader reader = new Reader(new HttpUrlConnectionChannel(new URL(tileUrl)))) {
            assertEquals("bytes=0-126", tileServer.takeRequest().getHeader(PMTilesDispatcher.RANGE_HEADER)); // PMTiles
                                                                                                             // header
            assertEquals("bytes=127-532", tileServer.takeRequest().getHeader(PMTilesDispatcher.RANGE_HEADER)); // PMTiles
                                                                                                               // root
                                                                                                               // directory
            byte[] tile = reader.getTile(13, 4350, 2984);
            assertNotNull(tile);
            assertEquals("bytes=703091-719277", tileServer.takeRequest().getHeader(PMTilesDispatcher.RANGE_HEADER)); // tile

            // setting the etag to a different value should force everything to be redone
            tileDispatcher.setEtag("4321");
            tile = reader.getTile(13, 4350, 2984);
            assertEquals("bytes=703091-719277", tileServer.takeRequest().getHeader(PMTilesDispatcher.RANGE_HEADER));
            assertEquals("bytes=0-126", tileServer.takeRequest().getHeader(PMTilesDispatcher.RANGE_HEADER));
            assertEquals("bytes=127-532", tileServer.takeRequest().getHeader(PMTilesDispatcher.RANGE_HEADER));
            assertEquals("bytes=703091-719277", tileServer.takeRequest().getHeader(PMTilesDispatcher.RANGE_HEADER));
        } catch (IOException | InterruptedException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void checkUnimplemented() {
        try (FileChannel channel = new HttpUrlConnectionChannel(new URL(tileUrl))) {
            ByteBuffer buffer = ByteBuffer.allocate(10);
            try {
                channel.read(new ByteBuffer[] { buffer }, 0, 0);
                fail("read should have failed");
            } catch (UnsupportedOperationException ex) {
                // expected
            }
            try {
                channel.write(buffer);
                fail("write should have failed");
            } catch (UnsupportedOperationException ex) {
                // expected
            }
            try {
                channel.write(new ByteBuffer[] { buffer }, 0, 0);
                fail("write should have failed");
            } catch (UnsupportedOperationException ex) {
                // expected
            }
            try {
                channel.size();
                fail("size should have failed");
            } catch (UnsupportedOperationException ex) {
                // expected
            }
            try {
                channel.truncate(10);
                fail("truncate should have failed");
            } catch (UnsupportedOperationException ex) {
                // expected
            }
            try {
                channel.force(true);
                fail("force should have failed");
            } catch (UnsupportedOperationException ex) {
                // expected
            }
            try {
                channel.transferTo(0, 0, null);
                fail("transferTo should have failed");
            } catch (UnsupportedOperationException ex) {
                // expected
            }
            try {
                channel.transferFrom(null, 0, 0);
                fail("transferFrom should have failed");
            } catch (UnsupportedOperationException ex) {
                // expected
            }
            try {
                channel.write(buffer, 0);
                fail("write should have failed");
            } catch (UnsupportedOperationException ex) {
                // expected
            }
            try {
                channel.map(null, 0, 0);
                fail("map should have failed");
            } catch (UnsupportedOperationException ex) {
                // expected
            }
            try {
                channel.lock(0, 0, false);
                fail("lock should have failed");
            } catch (UnsupportedOperationException ex) {
                // expected
            }
            try {
                channel.tryLock(0, 0, false);
                fail("tryLock should have failed");
            } catch (UnsupportedOperationException ex) {
                // expected
            }
        } catch (IOException ex) {
            fail(ex.getMessage());
        }
    }
}
