package com.lenovo.newdevice.tangocar.utils;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.lenovo.newdevice.tangocar.MainActivity.TAG;

/**
 * Created by liujk2 on 2017/2/23.
 */

public class SerializableUtils {
    public static byte[] getBytesFromObject(DataSerializable obj) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            writeObjectToData(obj, new DataOutputStream(out));
            return out.toByteArray();
        } catch (IOException e) {
            Log.e(TAG, "write obj to bytes error:", e);
        }
        return null;
    }

    public static DataSerializable getObjectFromBytes(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        try {
            return readObjectFromData(new DataInputStream(in));
        } catch (IOException e) {
            Log.e(TAG, "read obj from bytes error:", e);
        }
        return null;
    }

    public static byte[] getBytesFromString(String str) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            writeStringToData(str, new DataOutputStream(out));
            return out.toByteArray();
        } catch (IOException e) {
            Log.e(TAG, "write string to bytes error:", e);
        }
        return null;
    }

    public static String getStringFromBytes(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        try {
            return readStringFromData(new DataInputStream(in));
        } catch (IOException e) {
            Log.e(TAG, "read string from bytes error:", e);
        }
        return null;
    }

    public static void writeObjectToFile(int dirType, String fileName, DataSerializable obj) {
        FileOutputStream fOut = Utils.getInstance().openFileToWrite(dirType, fileName, false);
        try {
            writeObjectToData(obj, new DataOutputStream(fOut));
        } catch (Exception e) {
            Log.e(TAG, "write object to file error:", e);
        } finally {
            Utils.getInstance().closeOutputStream(fOut);
        }
    }

    public static DataSerializable readObjectFromFile(int dirType, String fileName) {
        DataSerializable obj = null;
        FileInputStream fIn = Utils.getInstance().openFileToRead(dirType, fileName);
        try {
            obj = readObjectFromData(new DataInputStream(fIn));
        } catch (Exception e) {
            Log.e(TAG, "read object from file error:", e);
            obj = null;
        } finally {
            Utils.getInstance().closeInputStream(fIn);
        }
        return obj;
    }

    public static void writeStringToData(String str, DataOutputStream out)
            throws IOException {
        boolean strOK = str != null;
        out.writeBoolean(strOK);
        if (!strOK) {
            return;
        }

        int length = str.length();
        out.writeInt(length);
        for (int i = 0; i < length; i++) {
            out.writeChar(str.charAt(i));
        }
    }

    public static String readStringFromData(DataInputStream in)
            throws IOException {
        boolean strOK = in.readBoolean();
        if (!strOK) {
            return null;
        }

        int length = in.readInt();
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = in.readChar();
        }
        return new String(chars);
    }

    public static void writeObjectToData(DataSerializable obj, DataOutputStream out)
            throws IOException {
        boolean objOK = obj != null;
        out.writeBoolean(objOK);
        if (!objOK) {
            return;
        }

        String className = obj.getClass().getName();
        writeStringToData(className, out);
        obj.writeToDataOutputStream(out);
    }

    public static DataSerializable readObjectFromData(DataInputStream in)
            throws IOException {
        boolean objOK = in.readBoolean();
        if (!objOK) {
            return null;
        }

        String className = readStringFromData(in);
        DataSerializable obj = null;
        try {
            obj = (DataSerializable) Class.forName(className).newInstance();
        } catch (Exception e) {
            throw new IOException(e);
        }
        if (obj != null) {
            obj.readFromDataInputStream(in);
        }
        return obj;
    }
}
