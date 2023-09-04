package ch.poole.geo.pmtiles;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Simple PMTiles reader
 * 
 * Note that this strives to be compatible with Android back to 4.1 and tries to avoid using at that time unsupported
 * Java features
 * 
 * @author simon
 *
 */
public class Reader implements AutoCloseable, Closeable {

    /**
     * Sole supported pmtiles version for now
     */
    public static final byte PMTILES_VERSION = 3;

    static class Header {
        private static final int    LENGTH                      = 127;
        private static final byte[] MAGIC                       = new byte[] { 0x50, 0x4D, 0x54, 0x69, 0x6C, 0x65, 0x73 };
        private static final int    VERSION_OFFSET              = 7;
        byte                        version;                                                                              // NOSONAR
        private static final int    ROOT_DIR_OFFSET_OFFSET      = 8;
        private long                rootDirOffset;
        private static final int    ROOT_DIR_LENGTH_OFFSET      = 16;
        private long                rootDirLength;
        private static final int    METADATA_OFFSET_OFFSET      = 24;
        private long                metadataOffset;
        private static final int    METADATA_LENGTH_OFFSET      = 32;
        private long                metadataLength;
        private static final int    LEAF_DIR_OFFSET_OFFSET      = 40;
        private long                leafDirOffset;
        private static final int    LEAF_DIR_LENGTH_OFFSET      = 48;
        @SuppressWarnings("unused")
        private long                leafDirLength;
        private static final int    TILE_DATA_OFFSET_OFFSET     = 56;
        private long                tileDataOffset;
        private static final int    TILE_DATA_LENGTH_OFFSET     = 64;
        @SuppressWarnings("unused")
        private long                tileDataLength;
        private static final int    ADDRESSED_TILES_OFFSET      = 72;
        @SuppressWarnings("unused")
        private long                addressedTiles;
        private static final int    TILE_ENTRIES_OFFSET         = 80;
        @SuppressWarnings("unused")
        private long                tileEntries;
        private static final int    TILE_CONTENTS_OFFSET        = 88;
        @SuppressWarnings("unused")
        private long                tileContents;
        private static final int    CLUSTERED_OFFSET            = 96;
        @SuppressWarnings("unused")
        private byte                clustered;
        private static final int    INTERNAL_COMPRESSION_OFFSET = 97;
        byte                        internalCompression;
        private static final int    TILE_COMPRESSION_OFFSET     = 98;
        private byte                tileCompression;
        private static final int    TILE_TYPE_OFFSET            = 99;
        private byte                tileType;
        private static final int    MIN_ZOOM_OFFSET             = 100;
        private byte                minZoom;
        private static final int    MAX_ZOOM_OFFSET             = 101;
        private byte                maxZoom;
        private static final int    LATITUDE_OFFSET             = 4;
        private static final int    MIN_POSITION_OFFSET         = 102;
        private int                 minLatitude;
        private int                 minLongitude;
        private static final int    MAX_POSITION_OFFSET         = 110;
        private int                 maxLatitude;
        private int                 maxLongitude;
        private static final int    CENTER_ZOOM_OFFSET          = 118;
        private byte                centerZoom;
        private static final int    CENTER_POSITION_OFFSET      = 119;
        private int                 centerLatitude;
        private int                 centerLongitude;

        /**
         * Reader the root header from an FileInputStream
         * 
         * @param fis the input stream
         * @throws IOException if reading fails
         */
        void read(@NotNull FileChannel channel) throws IOException {
            ByteBuffer buffer = ByteBuffer.allocate(LENGTH).order(ByteOrder.LITTLE_ENDIAN);

            int count = channel.read(buffer, 0);
            if (count != LENGTH) {
                throw new IOException("Incomplete header");
            }

            buffer.position(0);
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
            minLongitude = buffer.getInt(MIN_POSITION_OFFSET);
            minLatitude = buffer.getInt(MIN_POSITION_OFFSET + LATITUDE_OFFSET);
            maxLongitude = buffer.getInt(MAX_POSITION_OFFSET);
            maxLatitude = buffer.getInt(MAX_POSITION_OFFSET + LATITUDE_OFFSET);
            centerZoom = buffer.get(CENTER_ZOOM_OFFSET);
            centerLongitude = buffer.getInt(CENTER_POSITION_OFFSET);
            centerLatitude = buffer.getInt(CENTER_POSITION_OFFSET + LATITUDE_OFFSET);
        }
    }

    /**
     * PMTiles directory
     * 
     * We keep the PMTiles structure and don't try to create individual directory entries
     * 
     * Caveats: currently we don't support more than Integer.MAX_VALUE entries per directory, and only GZIP and ZIP
     * compression.
     * 
     * @author simon
     *
     */
    private class Directory {

        long[] ids;
        long[] runLengths;
        long[] lengths;
        long[] offsets;

        /**
         * Read the directory contents from the input stream
         * 
         * @param channel a FileChannel
         * @param offset the offset the data is in the file
         * @param length the length of the data
         * @param compression the internal compression method
         * @throws IOException if reading fails
         */
        void read(@NotNull FileChannel channel, long offset, long length, byte compression) throws IOException {
            ByteBuffer dirBuffer = ByteBuffer.allocate((int) length);
            dirBuffer.order(ByteOrder.LITTLE_ENDIAN);

            int count = channel.read(dirBuffer, offset);
            if (count != length) {
                throw new IOException("directory incomplete read " + count + " bytes of " + length); // NOSONAR
            }
            dirBuffer = Util.decompress(dirBuffer, compression);

            long entries = VarInt.getVarLong(dirBuffer);
            if (entries > Integer.MAX_VALUE) {
                throw new UnsupportedOperationException("Currently directories with more than Integer.MAX_VALUE are not supported");
            }
            ids = new long[(int) entries];
            runLengths = new long[(int) entries];
            lengths = new long[(int) entries];
            offsets = new long[(int) entries];

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
         * Find the tile with Hilbert index id
         * 
         * This uses a binary search in the id array.
         * 
         * @param header the PMTiles header
         * @param id the Hilbert index
         * @return a "tile" or null
         * @throws IOException if reading the tile fails
         */
        @Nullable
        byte[] findTile(@NotNull Header header, long id) throws IOException {
            int index = Arrays.binarySearch(ids, id);
            if (index >= 0) {
                long runLength = runLengths[index];
                if (runLength > 0) {
                    return readTile(header, index);
                } else {
                    return findTileInLeaf(header, id, index);
                }
            }
            // insertion point was returned
            // get previous entry
            int prev = -index - 2;
            if (prev >= 0) {
                long runLength = runLengths[prev];
                if (runLength > 0) {
                    if (ids[prev] + runLength - 1 >= id) {
                        return readTile(header, prev);
                    }
                } else {
                    return findTileInLeaf(header, id, prev);
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
        @Nullable
        private byte[] findTileInLeaf(@NotNull Header header, long id, int dirIndex) throws IOException {
            // leaf directory
            synchronized (leafCache) {
                final long leafId = ids[dirIndex];
                Directory leaf = leafCache.get(leafId); // NOSONAR Android compatibility
                if (leaf == null) {

                    leaf = new Directory();
                    leaf.read(channel, header.leafDirOffset + offsets[dirIndex], lengths[dirIndex], header.internalCompression);
                    leafCache.put(leafId, leaf);
                }
                return leaf.findTile(header, id);
            }
        }

        /**
         * Read and return a tile that is indexed in this directory
         * 
         * @param header the PMTiles header
         * @param dirIndex which entry this is in this directory
         * @return a "tile" or null
         * @throws IOException if reading the leaf directory or the tile fails
         */
        private byte[] readTile(@NotNull Header header, int dirIndex) throws IOException {
            final long tileLength = lengths[dirIndex];
            if (tileLength > Integer.MAX_VALUE) {
                throw new UnsupportedOperationException("Currently tiles larger than Integer.MAX_VALUE are not supported");
            }
            ByteBuffer tileBuffer = ByteBuffer.allocate((int) tileLength);
            int count = channel.read(tileBuffer, header.tileDataOffset + offsets[dirIndex]);
            if (count != tileLength) {
                throw new IOException("incomplete tile read " + count + " bytes of " + tileLength);
            }
            return tileBuffer.array();
        }
    }

    private class DirCache<K, V> extends LinkedHashMap<K, V> { // NOSONAR
        private static final long serialVersionUID = 1L;

        private static final int DEFAULT_CACHE_SIZE = 20;

        private int cacheSize = DEFAULT_CACHE_SIZE;

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > cacheSize;
        }

        /**
         * Set the size of the cache
         * 
         * @param size the size to set
         */
        public void setCacheSize(int cacheSize) {
            this.cacheSize = cacheSize;
        }
    }

    private final FileChannel         channel;
    Header                            header    = new Header();
    private Directory                 root      = new Directory();
    private DirCache<Long, Directory> leafCache = new DirCache<>();
    private List<Long>                tileCount = new ArrayList<>();

    /**
     * Construct a new Reader instance
     * 
     * @param file the PMTiles file
     * @throws IOException on read errors and similar issues
     */
    @SuppressWarnings("resource")
    public Reader(@NotNull File file) throws IOException {
        this(new FileInputStream(file).getChannel()); // NOSONAR closing the channel will close the stream
    }

    /**
     * Construct a new instance from a FileChannel
     * 
     * Note that while we only need the functionality of SeekableByteChannel this doesn't exist on Android prior to api
     * level 24 (Android 7.0)
     * 
     * @param channel the FileChannel
     * @throws IOException if we cannot read from the channel
     */
    public Reader(@NotNull FileChannel channel) throws IOException {
        this.channel = channel;
        init(channel);
        tileCount.add(0L);
    }

    /**
     * Read the header and root directory
     * 
     * @param channel the FileChannel to use
     * @throws IOException if reading fails
     */
    private void init(@NotNull FileChannel channel) throws IOException {
        header.read(channel);
        root.read(channel, header.rootDirOffset, header.rootDirLength, header.internalCompression);
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
        try {
            long id = Hilbert.zxyToIndex(zoom, x, y) + getZoomOffset(zoom);
            return root.findTile(header, id);
        } catch (SourceChangedException sce) {
            init(channel);
            return getTile(zoom, x, y);
        }
    }

    /**
     * Get the tile compression used
     * 
     * Note that we do not attempt to de-compress tiles and leave that to the calling application
     * 
     * @return a byte value identifying the compression in use
     */
    public byte getTileCompression() {
        return header.tileCompression;
    }

    /**
     * Get the tile type
     * 
     * @return a byte value identifying the tile type
     */
    public byte getTileType() {
        return header.tileType;
    }

    /**
     * Get the minimum zoom
     * 
     * @return the minimum zoom level
     */
    public byte getMinZoom() {
        return header.minZoom;
    }

    /**
     * Get the maximum zoom
     * 
     * @return the maximum zoom level
     */
    public byte getMaxZoom() {
        return header.maxZoom;
    }

    /**
     * Get the bounds for this file
     * 
     * @return left, bottom, right, top
     */
    public double[] getBounds() {
        return new double[] { header.minLongitude / 1E7D, header.minLatitude / 1E7D, header.maxLongitude / 1E7D, header.maxLatitude / 1E7D };
    }

    /**
     * Get the center of the tiles
     * 
     * @return lon, lat
     */
    public double[] getCenter() {
        return new double[] { header.centerLongitude / 1E7D, header.centerLatitude / 1E7D };
    }

    /**
     * Get a suggested zoom for the center
     * 
     * @return a zoom value
     */
    public byte getCenterZoom() {
        return header.centerZoom;
    }

    /**
     * Get the metadata
     * 
     * @return a String containing the JSON format metadata
     * @throws IOException if extracting the data fails
     */
    @NotNull
    public String getMetadata() throws IOException {
        final int length = (int) header.metadataLength;
        ByteBuffer buffer = ByteBuffer.allocate(length);

        int count = channel.read(buffer, header.metadataOffset);
        if (count != length) {
            throw new IOException("directory incomplete read " + count + " bytes of " + length);
        }

        return new String(Util.decompress(buffer, header.internalCompression).array());
    }

    /**
     * Set how many leaf directories should be retained in cache (the root directory is always cached)
     * 
     * @param size size (in entries) of the cache
     */
    public void setLeafDirectoryCacheSize(int size) {
        leafCache.setCacheSize(size);
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    /**
     * Get the offset for the Hilbert curve based id for a zoom level
     * 
     * This only works if z=0 has been pre-initialised to 0
     * 
     * @param z the zoom level
     * @return the accumulated number of tiles up to, but not including zoom z
     */
    long getZoomOffset(int z) {
        final int size = tileCount.size();
        if (size < z + 1) {
            for (int i = size; i < z + 1; i++) {
                tileCount.add((1 << (i - 1)) * (1 << (i - 1)) + tileCount.get(i - 1));
            }
        }
        return tileCount.get(z);
    }
}
