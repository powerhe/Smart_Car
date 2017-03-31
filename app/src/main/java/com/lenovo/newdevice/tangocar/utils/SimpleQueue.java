package com.lenovo.newdevice.tangocar.utils;

import java.lang.reflect.Array;

/**
 * Created by liujk2 on 2017/3/21.
 */
public class SimpleQueue<E extends Object> {
    private E[] mArray;
    private int mCapacity;
    private int mSize;
    private int mHead;
    private int mTail;

    public SimpleQueue(Class<E> type, int capacity) {
        mArray = (E[]) Array.newInstance(type, capacity);
        mCapacity = capacity;
        clear();
    }

    private int nexIndex(int index) {
        return (index + 1) % mCapacity;
    }

    synchronized private void putInner(E e) {
        int index = nexIndex(mHead);
        mArray[index] = e;
        mHead = index;
        if (mSize == mCapacity) {
            mTail = nexIndex(mTail);
        } else if (mSize < mCapacity) {
            mSize++;
        }
    }

    public void put(E e) {
        putInner(e);
    }

    public boolean add(E e) {
        if (mSize < mCapacity) {
            putInner(e);
            return true;
        } else {
            return false;
        }
    }

    synchronized private E getInner(int index, boolean remove) {
        if (mSize <= 0) {
            return null;
        }
        if (index < (0 - mSize) || index >= mSize) {
            return null;
        }
        int realIndex = 0;
        if (index >= 0) {
            realIndex = (mTail + index) % mCapacity;
        } else {
            realIndex = (mHead + index + 1 + mCapacity) % mCapacity;
        }
        E e = mArray[realIndex];
        if (index == 0 && remove) {
            mTail = nexIndex(mTail);
            mSize--;
        }
        return e;
    }

    public E remove() {
        return getInner(0, true);
    }

    public E get(int i) {
        return getInner(i, false);
    }

    public E poll() {
        return getInner(0, false);
    }

    public E peek() {
        return remove();
    }

    public int size() {
        return mSize;
    }

    public boolean isEmpty() {
        return mSize == 0;
    }

    public boolean isFull() {
        return mSize == mCapacity;
    }

    public void clear() {
        mSize = 0;
        mHead = 0;
        mTail = 0;
    }
}
