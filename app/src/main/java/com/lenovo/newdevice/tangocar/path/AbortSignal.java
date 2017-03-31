package com.lenovo.newdevice.tangocar.path;

import java.util.Observable;

public class AbortSignal extends Observable {

    public final void abort() {
        setChanged();
        notifyObservers("Abort called");
    }
}
