package com.mmorrell.phoenix.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class PhoenixUtil {

    public static int readInt32(byte[] data, int offset) {
        // convert 4 bytes into an int.
        // create a byte buffer and wrap the array
        ByteBuffer bb = ByteBuffer.wrap(
                Arrays.copyOfRange(
                        data,
                        offset,
                        offset + 4
                )
        );

        // if the file uses little endian as apposed to network
        // (big endian, Java's native) format,
        // then set the byte order of the ByteBuffer
        bb.order(ByteOrder.LITTLE_ENDIAN);

        // read your integers using ByteBuffer's getInt().
        // four bytes converted into an integer!
        return bb.getInt(0);
    }
}