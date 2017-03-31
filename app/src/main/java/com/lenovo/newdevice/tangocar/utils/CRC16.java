package com.lenovo.newdevice.tangocar.utils;

/**
 * Created by liujk2 on 2017/2/8.
 */

public class CRC16 {
    private int mWCRC;
    public CRC16() {
        reset();
    }

    public void reset() {
        mWCRC = 0xffff;
    }

    public void updateByte(byte data) {
        int high;
        int flag;

        // 16 位寄存器的高位字节
        high = (mWCRC >> 8) & 0xff;
        // 取被校验串的一个字节与 16 位寄存器的高位字节进行“异或”运算
        mWCRC = high ^ data;
        for (int j = 0; j < 8; j++) {
            flag = mWCRC & 0x0001;
            // 把这个 16 寄存器向右移一位
            mWCRC = mWCRC >> 1;
            // 若向右(标记位)移出的数位是 1,则生成多项式 1010 0000 0000 0001 和这个寄存器进行“异或”运算
            if (flag == 1)
                mWCRC ^= 0xa001;
        }
    }

    public void updateShort(short data) {
        updateByte((byte)((data >> 8) & 0xff));
        updateByte((byte)(data & 0xff));
    }

    public void updateInteger(int data) {
        updateByte((byte)((data >> 24) & 0xff));
        updateByte((byte)((data >> 16) & 0xff));
        updateByte((byte)((data >> 8) & 0xff));
        updateByte((byte)(data & 0xff));
    }

    public void updateBytes(byte[] data) {
        for(int i = 0; i < data.length; i++) {
            updateByte(data[i]);
        }
    }

    public int getCRC16() {
        return mWCRC & 0xffff;
    }
}
