package com.lenovo.newdevice.tangocar.path.finder;

public interface CCallback {
    /**
     * @param x X position of the target node.
     * @param y Y position of the target node.
     * @return The type of this node defined in {@link com.lenovo.newdevice.tangocar.map.GridInfo}
     */
    int getNodeType(int x, int y);

    // Signal from jni to indicate this session has been started.
    void onStart();

    // Signal from jni to indicate this session has been completed.
    void onComplete(int status);
}
