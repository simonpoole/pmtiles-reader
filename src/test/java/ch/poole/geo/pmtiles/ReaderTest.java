package ch.poole.geo.pmtiles;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

public class ReaderTest {
    File testFile1;
    File testFile2;
    File testFile3;
    File testFile4;

    @Before
    public void setup() {
        ClassLoader classLoader = getClass().getClassLoader();
        testFile1 = new File(classLoader.getResource("stamen_toner(raster)CC-BY+ODbL_z3.pmtiles").getFile());
        testFile2 = new File(classLoader.getResource("usgs-mt-whitney-8-15-webp-512.pmtiles").getFile());
        testFile3 = new File(classLoader.getResource("protomaps(vector)ODbL_firenze.pmtiles").getFile());
        testFile4 = new File(classLoader.getResource("leaf-dirs.pmtiles").getFile());
    }

    @Test
    public void header() {
        try (Reader reader = new Reader(testFile1)) {
            assertEquals(Reader.PMTILES_VERSION, reader.header.version);
            assertEquals(Constants.COMPRESSION_GZIP, reader.header.internalCompression);
            assertEquals(Constants.COMPRESSION_NONE, reader.getTileCompression());
            assertEquals(Constants.TYPE_PNG, reader.getTileType());
            assertEquals(0, reader.getMinZoom());
            assertEquals(3, reader.getMaxZoom());
            assertArrayEquals(new double[] { -180.0, -85.0, 180.0, 85.0 }, reader.getBounds(), 0.0000001);
            assertArrayEquals(new double[] { 0, 0 }, reader.getCenter(), 0.0000001);
            assertEquals(0, reader.getCenterZoom());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void tile000() {
        getAndCompareTile(testFile1, 0, 0, 0, new byte[] { (byte) 0x18, (byte) 0x97, (byte) 0x5d, (byte) 0xa8, (byte) 0x1e, (byte) 0x33, (byte) 0x28,
                (byte) 0x4b, (byte) 0x08, (byte) 0x22, (byte) 0x4a, (byte) 0x66, (byte) 0x77, (byte) 0xb4, (byte) 0xe8, (byte) 0x09 });
    }

    @Test
    public void zoomOffset() {
        try (Reader reader = new Reader(testFile1)) {
            assertEquals(0, reader.getZoomOffset(0));
            assertEquals(1, reader.getZoomOffset(1));
            assertEquals(5, reader.getZoomOffset(2));
            assertEquals(21, reader.getZoomOffset(3));
            assertEquals(366503875925L, reader.getZoomOffset(20));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Retrieve a tile and check that it has the correct checksum
     * 
     * @param file PMTiles file
     * @param z zoom
     * @param x x tile coord
     * @param y y tile coord
     * @param digest checksum
     */
    private void getAndCompareTile(File file, int z, int x, int y, byte[] digest) {
        try (Reader reader = new Reader(file)) {
            byte[] tile = reader.getTile(z, x, y);
            assertNotNull(tile);
            byte[] d = MessageDigest.getInstance("MD5").digest(tile);
            System.out.println(z + " " + x + " " + y + " " + toHex(d));
            assertArrayEquals(digest, d);
        } catch (IOException | NoSuchAlgorithmException e) {
            fail(e.getMessage());
        }
    }

    public static String toHex(byte[] data) {
        StringBuilder buf = new StringBuilder(data.length * 2);
        for (byte b : data) {
            buf.append(String.format("%02x", b & 0xFF));
        }
        return buf.toString();
    }

    @Test
    public void tile_2_2_3() {
        getAndCompareTile(testFile1, 2, 2, 3, new byte[] { (byte) 0x73, (byte) 0x43, (byte) 0xea, (byte) 0xd2, (byte) 0x54, (byte) 0xbb, (byte) 0x26,
                (byte) 0x6e, (byte) 0x02, (byte) 0x9d, (byte) 0x9e, (byte) 0xb9, (byte) 0x5f, (byte) 0x04, (byte) 0x17, (byte) 0x86 });
    }

    @Test
    public void tile_3_8_3() {
        getAndCompareTile(testFile1, 3, 8, 3, new byte[] { (byte) 0xc4, (byte) 0x25, (byte) 0xe2, (byte) 0x6c, (byte) 0x8d, (byte) 0x26, (byte) 0xdd,
                (byte) 0x48, (byte) 0x71, (byte) 0x3f, (byte) 0xc4, (byte) 0x9e, (byte) 0xc1, (byte) 0xe7, (byte) 0xd6, (byte) 0xd2 });
    }

    /**
     * These two tiles should be the same
     */
    @Test
    public void runlength() {
        getAndCompareTile(testFile1, 3, 5, 7, new byte[] { (byte) 0x4f, (byte) 0xf4, (byte) 0xd1, (byte) 0x39, (byte) 0xef, (byte) 0xd3, (byte) 0x97,
                (byte) 0xfd, (byte) 0xf4, (byte) 0x1f, (byte) 0x49, (byte) 0xce, (byte) 0xc1, (byte) 0xef, (byte) 0x23, (byte) 0x84 });
        // higher hilbert id first so that cache gets exercised as the rl is just 2
        try (Reader reader = new Reader(testFile1)) {
            byte[] tile1 = reader.getTile(3, 5, 7);
            assertNotNull(tile1);
            byte[] tile2 = reader.getTile(3, 4, 7);
            assertArrayEquals(tile1, tile2);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * This tile is indexed via a leaf directory
     */
    @Test
    public void tile_15_17145_11469() {
        getAndCompareTile(testFile4, 15, 17145, 11469, new byte[] { (byte) 0xa2, (byte) 0x87, (byte) 0xcc, (byte) 0x8f, (byte) 0x17, (byte) 0xa2, (byte) 0xdb,
                (byte) 0x14, (byte) 0x54, (byte) 0x55, (byte) 0x8a, (byte) 0x15, (byte) 0x68, (byte) 0x52, (byte) 0x18, (byte) 0x41 });
    }

    @Test
    public void metaData() {
        try (Reader reader = new Reader(testFile1)) {
            String meta = reader.getMetadata();
            assertEquals("{}", meta);
        } catch (IOException e) {
            fail(e.getMessage());
            e.printStackTrace();
        }
    }

    // @Test
    // public void performance() {
    // getAllTiles(testFile4);
    // }

    public void getAllTiles(@NotNull File file) {
        try (Reader reader = new Reader(file)) {
            reader.setLeafDirectoryCacheSize(70);
            double[] bounds = reader.getBounds();
            System.out.println("zoom " + reader.getMinZoom() + " " + reader.getMaxZoom());
            long tileCount = 0;
            long start = System.currentTimeMillis();
            for (int z = reader.getMinZoom(); z <= reader.getMaxZoom(); z++) {
                int x1 = xTile(z, bounds[0]);
                int x2 = xTile(z, bounds[2]);
                int y1 = yTile(z, bounds[1]);
                int y2 = yTile(z, bounds[3]);

                for (int x = x1; x < x2; x++) {
                    for (int y = y2; y < y1; y++) {
                        byte[] tile = reader.getTile(z, x, y);
                        tileCount++;
                    }
                }
            }
            System.out.println("Elapsed time " + (System.currentTimeMillis() - start) + " for " + tileCount + " tiles");
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    private int xTile(int zoom, double lon) {
        return (int) Math.floor((lon + 180) / 360 * (1 << zoom));
    }

    private int yTile(int zoom, double lat) {
        return (int) Math.floor((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1 << zoom));
    }
}
