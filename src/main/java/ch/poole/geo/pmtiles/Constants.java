package ch.poole.geo.pmtiles;

public final class Constants {

    private Constants() {
        throw new IllegalStateException("Utility class, can't be instantiated");
    }

    static final byte NOT_CLUSTERED_VALUE = 0;
    static final byte CLUSTERED_VALUE     = 1;

    public static final byte COMPRESSION_UNKNOWN = 0;
    public static final byte COMPRESSION_NONE    = 1;
    public static final byte COMPRESSION_GZIP    = 2;
    public static final byte COMPRESSION_BROTLI  = 3;
    public static final byte COMPRESSION_ZSTD    = 4;

    public static final byte TYPE_UNKNOWN = 0;
    public static final byte TYPE_MVT     = 1;
    public static final byte TYPE_PNG     = 2;
    public static final byte TYPE_JPEG    = 3;
    public static final byte TYPE_WEBP    = 4;
    public static final byte TYPE_AVIF    = 5;

    public static final String METADATA_VECTOR_LAYERS  = "vector_layers";
    public static final String METADATA_NAME           = "name";
    public static final String METADATA_DESCRIPTION    = "description";
    public static final String METADATA_ATTRIBUTION    = "attribution";
    public static final String METADATA_TYPE           = "type";
    public static final String METADATA_TYPE_OVERLAY   = "overlay";
    public static final String METADATA_TYPE_BASELAYER = "baselayer";
    public static final String METADATA_VERSION        = "version";
}
