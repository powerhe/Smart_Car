package com.lenovo.newdevice.tangocar.path.finder;

public enum FinderStatus {
    RUNNING,
    CANCELED,
    TIMEOUT,
    COMPLETED_FOUND,
    COMPLETED_NOT_FOUND,
    IDLE;

    public static FinderStatus fromInt(int code) {
        for (FinderStatus status : values()) {
            if (status.ordinal() == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Bad code#" + code);
    }
}
