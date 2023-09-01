package ch.poole.geo.pmtiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import ch.poole.geo.pmtiles.Reader.Header;

public class ReaderTest {
    File file;

    @Before
    public void setup() {
        ClassLoader classLoader = getClass().getClassLoader();
        file = new File(classLoader.getResource("stamen_toner(raster)CC-BY+ODbL_z3.pmtiles").getFile());
    }

    @Test
    public void header() {
        try (Reader reader = new Reader(file)) {
            assertEquals(Reader.PMTILES_VERSION, reader.header.version);
            assertEquals(Header.COMPRESSION_GZIP, reader.header.internalCompression);
            assertEquals(Header.COMPRESSION_NONE, reader.header.tileCompression);
            assertEquals(Header.TYPE_PNG, reader.header.tileType);
            assertEquals(0, reader.header.minZoom);
            assertEquals(3, reader.header.maxZoom);
        } catch (IOException e) {
            fail(e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void tile000() {
        try (Reader reader = new Reader(file)) {
            byte[] tile = reader.getTile(0, 0, 0);
            assertNotNull(tile);
            try (FileOutputStream fos = new FileOutputStream(new File("test-tile-000.png"))) {
                fos.write(tile);
            }
        } catch (IOException e) {
            fail(e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void tile223() {
        try (Reader reader = new Reader(file)) {
            byte[] tile = reader.getTile(2, 2, 3);
            assertNotNull(tile);
            try (FileOutputStream fos = new FileOutputStream(new File("test-tile-223.png"))) {
                fos.write(tile);
            }
        } catch (IOException e) {
            fail(e.getMessage());
            e.printStackTrace();
        }
    }
}
