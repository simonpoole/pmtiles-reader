package ch.poole.geo.pmtiles;

/**
 * See https://en.wikipedia.org/wiki/Hilbert_curve#Applications_and_mapping_algorithms
 * 
 * In practical terms this doesn't seem to be slower than
 * <a href="https://github.com/rawrunprotected/hilbert_curves">github.com/rawrunprotected/hilbert_curves</a>
 * <a href="https://threadlocalmutex.com/">threadlocalmutex.com</a> but works for all zoom levels.
 * 
 * https://github.com/davidmoten/hilbert-curve might be an alternative
 *
 */
public final class Hilbert {

    private Hilbert() {
        throw new IllegalStateException("Utility class, can't be instantiated");
    }

    /**
     * Convert tile coordinates to an Index along a 2d Hilbert curve
     * 
     * @param z the zoom level (determines the size of the 2d grid)
     * @param x tile x coordinate
     * @param y tile y coordinate
     * @return an index along the Hilbert curve
     */
    public static long zxyToIndex(int z, long x, long y) {
        long n = 1L << z;
        int rx;
        int ry;
        long s;
        int d = 0;
        for (s = n / 2; s > 0; s /= 2) {
            rx = (x & s) > 0 ? 1 : 0;
            ry = (y & s) > 0 ? 1 : 0;
            d += s * s * ((3 * rx) ^ ry);
            if (ry == 0) {
                if (rx == 1) {
                    x = n - 1 - x;
                    y = n - 1 - y;
                }
                long t = x;
                x = y;
                y = t;
            }
        }
        return d;
    }
}
