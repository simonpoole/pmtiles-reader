package ch.poole.geo.pmtiles;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Simple PMTiles reader
 * 
 * Note that this strives to be compatible with Android back to 4.1 and tries to avoid using at that time unsupported
 * Java features
 * 
 * @author simon
 *
 */
public class Reader implements AutoCloseable {
    public static final byte PMTILES_VERSION = 3;

    static class Header {
        static final int    LENGTH                      = 128;
        static final byte[] MAGIC                       = new byte[] { 0x50, 0x4D, 0x54, 0x69, 0x6C, 0x65, 0x73 };
        static final int    VERSION_OFFSET              = 7;
        byte                version;
        static final int    ROOT_DIR_OFFSET_OFFSET      = 8;
        long                rootDirOffset;
        static final int    ROOT_DIR_LENGTH_OFFSET      = 16;
        long                rootDirLength;
        static final int    METADATA_OFFSET_OFFSET      = 24;
        long                metadataOffset;
        static final int    METADATA_LENGTH_OFFSET      = 32;
        long                metadataLength;
        static final int    LEAF_DIR_OFFSET_OFFSET      = 40;
        long                leafDirOffset;
        static final int    LEAF_DIR_LENGTH_OFFSET      = 48;
        long                leafDirLength;
        static final int    TILE_DATA_OFFSET_OFFSET     = 56;
        long                tileDataOffset;
        static final int    TILE_DATA_LENGTH_OFFSET     = 64;
        long                tileDataLength;
        static final int    ADDRESSED_TILES_OFFSET      = 72;
        long                addressedTiles;
        static final int    TILE_ENTRIES_OFFSET         = 80;
        long                tileEntries;
        static final int    TILE_CONTENTS_OFFSET        = 88;
        long                tileContents;
        static final int    CLUSTERED_OFFSET            = 96;
        byte                clustered;
        static final byte   NOT_CLUSTERED_VALUE         = 0;
        static final byte   CLUSTERED_VALUE             = 1;
        static final int    INTERNAL_COMPRESSION_OFFSET = 97;
        byte                internalCompression;
        static final byte   COMPRESSION_UNKNOWN         = 0;
        static final byte   COMPRESSION_NONE            = 1;
        static final byte   COMPRESSION_GZIP            = 2;
        static final byte   COMPRESSION_BROTLI          = 3;
        static final byte   COMPRESSION_ZSTD            = 4;
        static final int    TILE_COMPRESSION_OFFSET     = 98;
        byte                tileCompression;
        static final int    TILE_TYPE_OFFSET            = 99;
        byte                tileType;
        static final byte   TYPE_UNKNOWN                = 0;
        static final byte   TYPE_MVT                    = 1;
        static final byte   TYPE_PNG                    = 2;
        static final byte   TYPE_JPEG                   = 3;
        static final byte   TYPE_WEBP                   = 4;
        static final byte   TYPE_AVIF                   = 5;
        static final int    MIN_ZOOM_OFFSET             = 100;
        byte                minZoom;
        static final int    MAX_ZOOM_OFFSET             = 101;
        byte                maxZoom;
        static final int    MIN_POSITION_OFFSET         = 102;
        long                minPosition;
        static final int    MAX_POSITION_OFFSET         = 110;
        long                maxPosition;
        static final int    CENTER_ZOOM_OFFSET          = 118;
        byte                centerZoom;
        static final int    CENTER_POSITION_OFFSET      = 119;
        long                centerPosition;

        void read(@Nonnull FileInputStream fis) throws IOException {
            byte[] headerBuffer = new byte[Header.LENGTH];
            int count = fis.read(headerBuffer);
            if (count != headerBuffer.length) {
                throw new IOException("Incomplete header");
            }
            ByteBuffer buffer = ByteBuffer.wrap(headerBuffer).order(ByteOrder.LITTLE_ENDIAN);
            byte[] magic = new byte[VERSION_OFFSET];
            buffer.get(magic);
            if (!Arrays.equals(MAGIC, magic)) {
                throw new IOException("Magic number missing, got " + magic);
            }
            version = buffer.get(VERSION_OFFSET);
            if (version != PMTILES_VERSION) {
                throw new IOException("Unsupported version " + version);
            }
            rootDirOffset = buffer.getLong(ROOT_DIR_OFFSET_OFFSET);
            rootDirLength = buffer.getLong(ROOT_DIR_LENGTH_OFFSET);
            metadataOffset = buffer.getLong(METADATA_OFFSET_OFFSET);
            metadataLength = buffer.getLong(METADATA_LENGTH_OFFSET);
            leafDirOffset = buffer.getLong(LEAF_DIR_OFFSET_OFFSET);
            leafDirLength = buffer.getLong(LEAF_DIR_LENGTH_OFFSET);
            tileDataOffset = buffer.getLong(TILE_DATA_OFFSET_OFFSET);
            tileDataLength = buffer.getLong(TILE_DATA_LENGTH_OFFSET);
            addressedTiles = buffer.getLong(ADDRESSED_TILES_OFFSET);
            tileEntries = buffer.getLong(TILE_ENTRIES_OFFSET);
            tileContents = buffer.getLong(TILE_CONTENTS_OFFSET);
            clustered = buffer.get(CLUSTERED_OFFSET);
            internalCompression = buffer.get(INTERNAL_COMPRESSION_OFFSET);
            tileCompression = buffer.get(TILE_COMPRESSION_OFFSET);
            tileType = buffer.get(TILE_TYPE_OFFSET);
            minZoom = buffer.get(MIN_ZOOM_OFFSET);
            maxZoom = buffer.get(MAX_ZOOM_OFFSET);
            minPosition = buffer.get(MIN_POSITION_OFFSET);
            maxPosition = buffer.get(MAX_POSITION_OFFSET);
            centerZoom = buffer.get(CENTER_ZOOM_OFFSET);
            centerPosition = buffer.getLong(CENTER_POSITION_OFFSET);
        }
    }

    private class Directory {

        long[] ids;
        int[]  runLengths;
        int[]  lengths;
        long[] offsets;

        private Map<Long, Directory> leafs = new HashMap<>();

        private FileChannel channel;

        void read(@Nonnull FileInputStream fis, long offset, long length, byte compression) throws IOException {
            ByteBuffer dirBuffer = ByteBuffer.allocate((int) length);

            channel = fis.getChannel();
            int count = channel.read(dirBuffer, offset);
            if (count != length) {
                throw new IOException("directory incomplete read " + count + " bytes of " + length);
            }
            if (compression != Header.COMPRESSION_NONE) {
                try (ByteArrayInputStream bis = new ByteArrayInputStream(dirBuffer.array())) {
                    switch (compression) {
                    case Header.COMPRESSION_GZIP:
                        dirBuffer = copy(new GZIPInputStream(bis));
                        break;
                    case Header.COMPRESSION_ZSTD:
                        dirBuffer = copy(new ZipInputStream(bis));
                        break;
                    default:
                        throw new UnsupportedOperationException("Internal compression " + compression + " not supported");
                    }
                }
            }

            int entries = VarInt.getVarInt(dirBuffer);
            ids = new long[entries];
            runLengths = new int[entries];
            lengths = new int[entries];
            offsets = new long[entries];

            long lastId = 0;
            for (int i = 0; i < entries; i++) {
                int diff = VarInt.getVarInt(dirBuffer);
                long newId = lastId + diff;
                ids[i] = newId;
                lastId = newId;
            }
            for (int i = 0; i < entries; i++) {
                runLengths[i] = VarInt.getVarInt(dirBuffer);
            }
            for (int i = 0; i < entries; i++) {
                lengths[i] = VarInt.getVarInt(dirBuffer);
            }
            for (int i = 0; i < entries; i++) {
                long value = VarInt.getVarLong(dirBuffer);
                if (value == 0 && i > 0) {
                    offsets[i] = offsets[i - 1] + lengths[i - 1];
                } else {
                    offsets[i] = value - 1;
                }
            }
        }

        /**
         * Copy an InputStream to a ByteBuffer
         * 
         * @param is the InputStream
         * @return a ByteBuffer with the contents of the InputStream
         * @throws IOException if reading or writing fails
         */
        @Nonnull
        ByteBuffer copy(@Nonnull InputStream is) throws IOException {
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                byte[] temp = new byte[1024];
                int len;
                while ((len = is.read(temp)) != -1) {
                    bos.write(temp, 0, len);
                }
                return ByteBuffer.wrap(bos.toByteArray());
            }
        }

        /**
         * Find the tile with Hilbert index id
         * 
         * @param header the PMTiles header
         * @param id the Hilbert index
         * @return a "tile" or null
         * @throws IOException if reading the tile fails
         */
        @Nullable
        byte[] findTile(@Nonnull Header header, long id) throws IOException {
            int i = Arrays.binarySearch(ids, id);
            if (i >= 0) {
                int runLength = runLengths[i];
                if (runLength > 0) {
                    return readTile(header, i);
                } else {
                    return findTileInLeaf(header, id, i);
                }
            }
            // insertion point was returned
            // get previous entry
            int prev = -i - 2;
            if (prev >= 0) {
                int runLength = runLengths[prev];
                if (runLength > 0 && (ids[prev] + runLength >= id)) {
                    return readTile(header, prev);
                } else {
                    return findTileInLeaf(header, id, i);
                }
            }
            // not found
            return null;
        }

        /**
         * Find a tile in a leaf directory
         * 
         * If the leaf directory hasn't been read yet, read and cache it
         *
         * 
         * @param header the PMTiles header
         * @param id the Hilbert index
         * @param dirIndex which entry this is in this directory
         * @return a "tile" or null
         * @throws IOException if reading the leaf directory or the tile fails
         */
        private byte[] findTileInLeaf(@Nonnull Header header, long id, int dirIndex) throws IOException {
            // leaf directory
            Directory leaf = leafs.get(id); // NOSONAR Android compatibility
            if (leaf == null) {
                leaf = new Directory();
                leaf.read(fis, header.leafDirOffset + offsets[dirIndex], lengths[dirIndex], header.internalCompression);
                leafs.put(id, leaf);
            }
            return leaf.findTile(header, id);
        }

        /**
         * Read and return a tile that is indexed in this directory
         * 
         * @param header the PMTiles header
         * @param dirIndex which entry this is in this directory
         * @return a "tile" or null
         * @throws IOException if reading the leaf directory or the tile fails
         */
        private byte[] readTile(@Nonnull Header header, int dirIndex) throws IOException {
            final int tileLength = lengths[dirIndex];
            ByteBuffer tileBuffer = ByteBuffer.allocate(tileLength);
            int count = channel.read(tileBuffer, header.tileDataOffset + offsets[dirIndex]);
            if (count != tileLength) {
                throw new IOException("incomplete tile read " + count + " bytes of " + tileLength);
            }
            return tileBuffer.array();
        }
    }

    private final FileInputStream fis;
    Header                        header    = new Header();
    Directory                     root      = new Directory();
    List<Long>                    tileCount = new ArrayList<>();

    /**
     * Construct a new Reader
     * 
     * @param file the PMTiles file
     * @throws IOException on read errors and similar issues
     */
    public Reader(@Nonnull File file) throws IOException {
        fis = new FileInputStream(file);
        header.read(fis);
        root.read(fis, header.rootDirOffset, header.rootDirLength, header.internalCompression);
        tileCount.add(0L);
    }

    /**
     * Retrieve a, potentially compressed, tile
     * 
     * @param zoom zoom level
     * @param x x tile coordinate (google/osm convention)
     * @param y y tile coordinate (google/osm convention)
     * @return the "tile" or null if not found
     * @throws IOException on read errors and similar issues
     */
    @Nullable
    public byte[] getTile(int zoom, int x, int y) throws IOException {
        long id = Hilbert.hilbertXYToIndex(zoom, x, y) + getZoomOffset(zoom);
        return root.findTile(header, id);
    }

    /**
     * Get the tile compression used
     * 
     * Note that we do not attempt to de-compress tiles and leave that to the callign application
     * 
     * @return a byte value identifying the compression in use 
     */
    public byte tileCompression() {
        return header.tileCompression;
    }
    
    /**
     * Get the tile type
     * 
     * @return a byte value identifying the tile type 
     */
    public byte tileType() {
        return header.tileType;
    }
    
    /**
     * Get the offset for the Hilbert curve based id for a zoom level
     * 
     * @param z the zoom level
     * @return the accumulated number of tiles up to, but not including zoom z
     */
    private long getZoomOffset(int z) {
        final int size = tileCount.size();
        if (size < (z + 1)) {
            for (int i = size; i < z + 1; i++) {
                tileCount.add((2 >> (i + 1)) + tileCount.get(i - 1));
            }
        }
        return tileCount.get(z);
    }

    @Override
    public void close() throws IOException {
        fis.close();
    }
}
