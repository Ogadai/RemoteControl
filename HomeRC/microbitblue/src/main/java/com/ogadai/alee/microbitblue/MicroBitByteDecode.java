package com.ogadai.alee.microbitblue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by alee on 08/06/2017.
 */

public class MicroBitByteDecode {

    public static short getShort(byte[] bytes, int offset) {
        return getShort(bytes, offset, ByteOrder.LITTLE_ENDIAN);
    }
    public static short getShort(byte[] bytes, int offset, ByteOrder byteOrder) {
        return getShort(new byte[] { bytes[offset], bytes[offset + 1] }, byteOrder);
    }
    public static short getShort(byte[] bytes) {
        return getShort(bytes, ByteOrder.LITTLE_ENDIAN);
    }
    public static short getShort(byte[] bytes, ByteOrder byteOrder) {
        if (bytes.length == 1) {
            return bytes[0];
        } else {
            ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
            byteBuffer.order(byteOrder);

            return byteBuffer.getShort();
        }
    }
}
