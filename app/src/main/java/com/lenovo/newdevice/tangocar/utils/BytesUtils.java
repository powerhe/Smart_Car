package com.lenovo.newdevice.tangocar.utils;

/**
 * Created by liujk2 on 2017/2/16.
 */

public class BytesUtils {
    public static void putIntToBytes(byte[] bytes, int index, int data) {
        bytes[index] = (byte)((data >> 24) & 0xff);
        bytes[index + 1] = (byte)((data >> 16) & 0xff);
        bytes[index + 2] = (byte)((data >> 8) & 0xff);
        bytes[index + 3] = (byte)(data & 0xff);
    }

    public static int getIntFromBytes(byte[] bytes, int index) {
        int data = 0;
        data |= (bytes[index] << 24) & 0xff000000;
        data |= (bytes[index + 1] << 16) & 0xff0000;
        data |= (bytes[index + 2] << 8) & 0xff00;
        data |= bytes[index + 3] & 0xff;
        return data;
    }

    public static boolean bytesEquals(byte[] bytes1, byte[] bytes2) {
        if (bytes1.length != bytes2.length) {
            return false;
        }
        for (int i = 0; i < bytes1.length; i++) {
            if (bytes1[i] != bytes2[i]) {
                return false;
            }
        }
        return true;
    }

    public static String bytesToString(byte[] bytes) {
        if (bytes == null) {
            return null;
        } else {
            StringBuffer stringBuffer = new StringBuffer("bytes{");
            int value = 0;
            for (int i = 0; i < bytes.length; i ++) {
                stringBuffer.append(' ');
                value = bytes[i] & 0xff;
                stringBuffer.append(Integer.toHexString(value));
                stringBuffer.append('/');
                stringBuffer.append(value);
            }
            stringBuffer.append('}');
            return stringBuffer.toString();
        }
    }
}
