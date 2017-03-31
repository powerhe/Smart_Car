package com.lenovo.newdevice.tangocar.remote;

/**
 * Created by liujk2 on 2017/2/8.
 */

import android.util.Log;

import com.lenovo.newdevice.tangocar.utils.BytesUtils;
import com.lenovo.newdevice.tangocar.utils.CRC16;
import com.lenovo.newdevice.tangocar.utils.DataSerializable;
import com.lenovo.newdevice.tangocar.utils.SerializableUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.lenovo.newdevice.tangocar.MainActivity.TAG;
/**
 * remote control message
 * include server and client
 * message format
 * | magic | type | reserve | serial no | length of content | content | crc |
 * magic 4 bytes
 * type 1 byte
 * reserve 3 bytes
 * sn 4 bytes
 * length 4 bytes
 * content length bytes
 * crc 2 bytes
 */
public class RCMessage {
    private static final boolean DEBUG = false;

    public static final byte CLIENT_CONTROLLER = 1;
    public static final byte CLIENT_MASTER = 2;
    public static final byte CLIENT_SLAVE = 3;

    public static final byte TYPE_INVALID = 0;
    public static final byte TYPE_HEARTBEAT = 1;
    public static final byte TYPE_REPLY = 2;
    public static final byte TYPE_HANDSHAKE = 3;
    public static final byte TYPE_HANDSHAKE_OK = 4;

    public static final byte TYPE_VALUE_MASK = 127; // 0x7F
    public static final byte TYPE_CLASS_DATA = -128; // 0x80

    public static final byte TYPE_CTL_INIT = 20;
    public static final byte TYPE_CTL_GET_ALL = 20;
    public static final byte TYPE_CTL_GET_CONFIG = 21;
    public static final byte TYPE_CTL_GET_STATUS = 22;
    public static final byte TYPE_CTL_GET_POSE = 23;
    public static final byte TYPE_CTL_GET_DISTANCE = 24;
    public static final byte TYPE_CTL_GET_MAP = 25;
    public static final byte TYPE_CTL_GET_ADF = 26;
    public static final byte TYPE_CTL_SET_MASTER = 27;
    public static final byte TYPE_CTL_SET_SLAVE = 28;
    public static final byte TYPE_CTL_ALL_BEGIN = 29;
    public static final byte TYPE_CTL_ALL_END = 30;
    public static final byte TYPE_CTL_CLEAR_MAP = 31;
    public static final byte TYPE_CTL_SET_MAP = 32;
    public static final byte TYPE_CTL_SET_ADF = 33;
    public static final byte TYPE_CTL_RESTART_TANGO = 34;
    public static final byte TYPE_CTL_CLEAR_PATH = 35;
    public static final byte TYPE_CTL_GET_LOG = 36;
    public static final byte TYPE_CTL_MAX = 49;

    public static final byte TYPE_DATA_INIT = 50;
    public static final byte TYPE_DATA_CAR_CONFIG = 50;
    public static final byte TYPE_DATA_CAR_STATUS = 51;
    public static final byte TYPE_DATA_CAR_DISTANCE = 52;
    public static final byte TYPE_DATA_CAR_POSE = 53;
    public static final byte TYPE_DATA_CAR_CONTROL = 54;
    public static final byte TYPE_DATA_CAR_PATH = 55;
    public static final byte TYPE_DATA_MAP = 56;
    public static final byte TYPE_DATA_ADF = 57;
    public static final byte TYPE_DATA_CAR_EXPECTED_PATH = 58;
    public static final byte TYPE_DATA_LOG = 59;
    public static final byte TYPE_DATA_MAX = 79;

    public static final byte TYPE_FILE_MAP = 80;
    public static final byte TYPE_FILE_ADF = 81;

    public static final byte[] MAGIC_BYTES = {'R', 'C', 'M', 'G'};

    private static int sCurrentSN = 0;
    private static Object sSNLock = new Object();
    private static int getSN() {
        synchronized (sSNLock) {
            return sCurrentSN ++;
        }
    }

    private static RCMessage sHeartBeat = null;

    private boolean received;
    private byte type;
    private int sn;
    private int length;
    private byte[] content;
    private int crc16;
    private byte replayType;
    byte[] typeAndReserve;
    private int replaySN;
    private CRC16 mCRC16;

    public RCMessage(byte type) {
        this.type = type;
        typeAndReserve = new byte[4];
        typeAndReserve[0] = type;
        typeAndReserve[1] = 0;
        typeAndReserve[2] = 0;
        typeAndReserve[3] = 0;
        replayType = TYPE_INVALID;
        mCRC16 = new CRC16();
        sn = getSN();
        length = 0;
        content = null;
    }

    public byte getType() {
        return type;
    }

    public RCMessage getReply() {
        RCMessage message = new RCMessage(TYPE_REPLY);
        message.length = 8;
        message.replayType = type;
        message.replaySN = sn;
        message.fillContent();
        message.computeCRC();
        return message;
    }

    public static RCMessage readFromInputStream(InputStream inputStream) throws IOException {
        int magicBufLen = MAGIC_BYTES.length;
        byte[] bufForMagic = new byte[magicBufLen];
        int readCount;
        DataInputStream input = new DataInputStream(inputStream);

        // read magic
        readCount = input.read(bufForMagic);
        if (readCount == 0) {
            throw new IOException("read data length is 0");
        }
        if (DEBUG) Log.d(TAG, "read " + BytesUtils.bytesToString(bufForMagic));
        if (readCount != magicBufLen
                || !BytesUtils.bytesEquals(bufForMagic, MAGIC_BYTES)) {
            throw new IOException("not valid MAGIC_BYTES, " + BytesUtils.bytesToString(bufForMagic)
                    + ". should be " + BytesUtils.bytesToString(MAGIC_BYTES));
        }
        byte type = TYPE_INVALID;
        // read type and reserve
        readCount = input.read(bufForMagic);
        if (DEBUG) Log.d(TAG, "read " + BytesUtils.bytesToString(bufForMagic));
        if (readCount != magicBufLen) {
            throw new IOException("read length error!");
        }
        type = bufForMagic[0];
        RCMessage message = new RCMessage(type);
        message.received = true;
        // read sn
        message.sn = input.readInt();
        if (DEBUG) Log.d(TAG, "read Int " + message.sn);
        // read length
        message.length = input.readInt();
        if (DEBUG) Log.d(TAG, "read Int " + message.length);
        // read content
        message.readContent(input);
        if (DEBUG) Log.d(TAG, "read " + BytesUtils.bytesToString(message.content));
        // read crc
        int crc16 = input.readShort();
        crc16 &= 0xffff;
        if (DEBUG) Log.d(TAG, "read Short " + crc16);
        message.computeCRC();
        if (crc16 != message.crc16) {
            throw new IOException("crc is not correct! " + crc16 + " vs " + message.crc16);
        }
        return message;
    }

    public static RCMessage getHeartBeatMessage() {
        if (sHeartBeat == null) {
            sHeartBeat = new RCMessage(TYPE_HEARTBEAT);
            sHeartBeat.computeCRC();
        } else {
            sHeartBeat.updateSN();
        }
        return sHeartBeat;
    }

    public static RCMessage getMessageForType(byte type) {
        RCMessage message = new RCMessage(type);
        message.computeCRC();
        return message;
    }

    public static RCMessage getControlMessage(byte type) {
        if (type < TYPE_CTL_INIT || type > TYPE_CTL_MAX) {
            return null;
        }
        RCMessage message = new RCMessage(type);
        message.computeCRC();
        return message;
    }

    public static RCMessage getMessageFromString(byte type, String str) {
        if (str == null) {
            return null;
        }
        RCMessage message = new RCMessage(type);
        byte[] content = SerializableUtils.getBytesFromString(str);
        message.length = content.length;
        message.content = content;
        message.computeCRC();
        return message;
    }

    public static String getStringFromMessage(RCMessage message) {
        if (message == null) {
            return null;
        }
        byte type = message.type;
        if (type != TYPE_DATA_LOG) {
            return null;
        }
        if (message.length == 0) {
            return null;
        }
        return SerializableUtils.getStringFromBytes(message.content);
    }

    public static RCMessage getMessageFromObject(byte type, DataSerializable obj) {
        if (obj == null) {
            return null;
        }
        RCMessage message = new RCMessage(type);
        byte[] content = SerializableUtils.getBytesFromObject(obj);
        message.length = content.length;
        message.content = content;
        message.computeCRC();
        return message;
    }

    public static DataSerializable getObjectFromMessage(RCMessage message) {
        if (message == null) {
            return null;
        }
        byte type = message.type;
        if (type < TYPE_DATA_INIT || type > TYPE_DATA_MAX) {
            return null;
        }
        if (message.length == 0) {
            return null;
        }
        return SerializableUtils.getObjectFromBytes(message.content);
    }

    public boolean isNotData() {
        if (type < TYPE_DATA_INIT) {
            return true;
        }
        return false;
    }

    public boolean isHeartBeat() {
        return type == TYPE_HEARTBEAT;
    }

    public boolean isType(byte type) {
        return type == this.type;
    }

    public boolean isReplyForType(byte type) {
        return (this.type == TYPE_REPLY) && (replayType == type);
    }

    public boolean isReplyFor(RCMessage message) {
        if (type != TYPE_REPLY) {
            return false;
        }
        if (replayType == message.type
                && replaySN == message.sn) {
            return true;
        }
        return false;
    }

    public void updateSN() {
        sn = getSN();
        computeCRC();
    }

    private void readContent(DataInputStream input) throws IOException {
        if (length > 0) {
            content = new byte[length];
            int readLen = 0;
            int reserveLen = length;
            while (readLen < length) {
                int len = input.read(content, readLen, reserveLen);
                if (len < 0) {
                    throw new IOException("read content error!");
                }
                readLen += len;
                reserveLen -= len;
            }
        }
        if (type == TYPE_REPLY) {
            replayType = content[0];
            replaySN = BytesUtils.getIntFromBytes(content, 4);
        }
    }

    private void fillContent() {
        boolean needFill = false;
        if (length > 0) {
            if (content == null) {
                content = new byte[length];
                needFill = true;
            }
        }
        if (needFill) {
            if (type == TYPE_REPLY) {
                content[0] = replayType;
                content[1] = 0;
                content[2] = 0;
                content[3] = 0;
                BytesUtils.putIntToBytes(content, 4, replaySN);
            }
        }
    }

    public void send(OutputStream ouput) throws IOException {
        DataOutputStream outputStream = new DataOutputStream(ouput);

        if (DEBUG) Log.d(TAG, "write " + BytesUtils.bytesToString(MAGIC_BYTES));
        outputStream.write(MAGIC_BYTES);
        if (DEBUG) Log.d(TAG, "write " + BytesUtils.bytesToString(typeAndReserve));
        outputStream.write(typeAndReserve);
        if (DEBUG) Log.d(TAG, "write Int " + sn);
        outputStream.writeInt(sn);
        if (DEBUG) Log.d(TAG, "write Int " + length);
        outputStream.writeInt(length);
        fillContent();
        if (content != null) {
            if (DEBUG) Log.d(TAG, "write " + BytesUtils.bytesToString(content));
            outputStream.write(content);
        }
        if (DEBUG) Log.d(TAG, "write Short " + crc16);
        outputStream.writeShort(crc16);
        outputStream.flush();
    }

    private int computeCRC() {
        mCRC16.reset();
        mCRC16.updateBytes(MAGIC_BYTES);
        mCRC16.updateBytes(typeAndReserve);
        mCRC16.updateInteger(sn);
        mCRC16.updateInteger(length);
        fillContent();
        if (content != null) {
            mCRC16.updateBytes(content);
        }
        crc16 = mCRC16.getCRC16();
        return crc16;
    }

    public String toString() {
        String str = "RCMessage{type:" + type + ", sn:" + sn + ", length:" + length + "}";
        if (type == TYPE_REPLY) {
            str += ", Reply for {type:" + replayType + ", sn:" + replaySN + "}";
        }
        return str;
    }
}
