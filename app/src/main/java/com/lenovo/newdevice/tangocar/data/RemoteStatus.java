package com.lenovo.newdevice.tangocar.data;

/**
 * Created by liujk2 on 2017/3/3.
 */

public class RemoteStatus {
    public enum ClientType{HOST, CONTROLLER};
    public ClientType type;
    public boolean connected;
    public boolean enabled;
}
