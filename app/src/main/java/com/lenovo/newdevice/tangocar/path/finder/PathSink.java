package com.lenovo.newdevice.tangocar.path.finder;

/**
 * A path point receiver.
 */
public interface PathSink {
    void receive(int x, int y);
}
