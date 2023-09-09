/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package ch.poole.geo.pmtiles;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Common methods to encode and decode varints and varlongs into ByteBuffers and arrays. Varints and varlongs are
 * variable length encoders as defined in https://developers.google.com/protocol-buffers/docs/encoding.
 */
public class VarInt {

    /**
     * Maximum encoded size of 32-bit positive integers (in bytes)
     */
    public static final int MAX_VARINT_SIZE = 5;

    /**
     * maximum encoded size of 64-bit longs, and negative 32-bit ints (in bytes)
     */
    public static final int MAX_VARLONG_SIZE = 10;

    private VarInt() {
    }

    /**
     * Reads a varint from the current position of the given ByteBuffer and returns the decoded value as 32 bit integer.
     *
     * <p>
     * The position of the buffer is advanced to the first byte after the decoded varint.
     *
     * @param src the ByteBuffer to get the var int from
     * @return The integer value of the decoded varint
     */
    public static int getVarInt(ByteBuffer src) {
        int tmp;
        if ((tmp = src.get()) >= 0) {
            return tmp;
        }
        int result = tmp & 0x7f;
        if ((tmp = src.get()) >= 0) {
            result |= tmp << 7;
        } else {
            result |= (tmp & 0x7f) << 7;
            if ((tmp = src.get()) >= 0) {
                result |= tmp << 14;
            } else {
                result |= (tmp & 0x7f) << 14;
                if ((tmp = src.get()) >= 0) {
                    result |= tmp << 21;
                } else {
                    result |= (tmp & 0x7f) << 21;
                    result |= (tmp = src.get()) << 28;
                    while (tmp < 0) {
                        // We get into this loop only in the case of overflow.
                        // By doing this, we can call getVarInt() instead of
                        // getVarLong() when we only need an int.
                        tmp = src.get();
                    }
                }
            }
        }
        return result;
    }

    /**
     * Reads an up to 64 bit long varint from the current position of the given ByteBuffer and returns the decoded value
     * as long.
     *
     * <p>
     * The position of the buffer is advanced to the first byte after the decoded varint.
     *
     * @param src the ByteBuffer to get the var int from
     * @return The integer value of the decoded long varint
     */
    public static long getVarLong(ByteBuffer src) {
        long tmp;
        if ((tmp = src.get()) >= 0) {
            return tmp;
        }
        long result = tmp & 0x7f;
        if ((tmp = src.get()) >= 0) {
            result |= tmp << 7;
        } else {
            result |= (tmp & 0x7f) << 7;
            if ((tmp = src.get()) >= 0) {
                result |= tmp << 14;
            } else {
                result |= (tmp & 0x7f) << 14;
                if ((tmp = src.get()) >= 0) {
                    result |= tmp << 21;
                } else {
                    result |= (tmp & 0x7f) << 21;
                    if ((tmp = src.get()) >= 0) {
                        result |= tmp << 28;
                    } else {
                        result |= (tmp & 0x7f) << 28;
                        if ((tmp = src.get()) >= 0) {
                            result |= tmp << 35;
                        } else {
                            result |= (tmp & 0x7f) << 35;
                            if ((tmp = src.get()) >= 0) {
                                result |= tmp << 42;
                            } else {
                                result |= (tmp & 0x7f) << 42;
                                if ((tmp = src.get()) >= 0) {
                                    result |= tmp << 49;
                                } else {
                                    result |= (tmp & 0x7f) << 49;
                                    if ((tmp = src.get()) >= 0) {
                                        result |= tmp << 56;
                                    } else {
                                        result |= (tmp & 0x7f) << 56;
                                        result |= ((long) src.get()) << 63;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

}
