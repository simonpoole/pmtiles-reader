package ch.poole.geo.pmtiles;

public final class Constants {
    
    private Constants() {
        throw new IllegalStateException("Utility class, can't be instantiated"); 
    }

    static final byte NOT_CLUSTERED_VALUE = 0;
    static final byte CLUSTERED_VALUE     = 1;

    static final byte COMPRESSION_UNKNOWN = 0;
    static final byte COMPRESSION_NONE    = 1;
    static final byte COMPRESSION_GZIP    = 2;
    static final byte COMPRESSION_BROTLI  = 3;
    static final byte COMPRESSION_ZSTD    = 4;

    static final byte TYPE_UNKNOWN = 0;
    static final byte TYPE_MVT     = 1;
    static final byte TYPE_PNG     = 2;
    static final byte TYPE_JPEG    = 3;
    static final byte TYPE_WEBP    = 4;
    static final byte TYPE_AVIF    = 5;
}
