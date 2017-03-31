package com.lenovo.newdevice.tangocar.data;

import android.graphics.Point;

import com.lenovo.newdevice.tangocar.map.MapPath;
import com.lenovo.newdevice.tangocar.map.WorldMapGrid;
import com.lenovo.newdevice.tangocar.utils.MathUtils;

import java.util.ArrayList;
import java.util.Queue;

/**
 * Created by liujk2 on 2017/2/21.
 */

public class GlobalData {

    private RemoteStatus mRemoteStatus;
    private WorldMapGrid mMap;
    private ArrayList<CarData> mCarsData;

    public GlobalData() {
        mMap = new WorldMapGrid();
        mRemoteStatus = new RemoteStatus();
        mCarsData = new ArrayList<CarData>(3);
        addCar();
    }

    public RemoteStatus getRemoteStatus() {
        return mRemoteStatus;
    }

    private int getNewCarIndex() {
        int carCount = mCarsData.size();
        int newIndex;
        for (newIndex = 0; newIndex < carCount; newIndex ++) {
            CarData carData = mCarsData.get(newIndex);
            if (carData == null || !carData.mValid) {
                break;
            }
        }
        return newIndex;
    }

    public int addCar() {
        int newIndex = getNewCarIndex();
        CarData carData = new CarData();
        carData.mIndex = newIndex;
        carData.mValid = true;
        mCarsData.add(newIndex, carData);
        return newIndex;
    }

    public WorldMapGrid getMap() {
        return mMap;
    }

    public CarData getCarData(int carIndex) {
        return mCarsData.get(carIndex);
    }

    public MapPath getPath(int carIndex) {
        CarData carData = mCarsData.get(carIndex);
        if (carData != null) {
            return carData.getPath();
        }
        return null;
    }

    public void clearMap() {
        mMap.clear();
        int carCount = mCarsData.size();
        int newIndex;
        for (newIndex = 0; newIndex < carCount; newIndex ++) {
            CarData carData = mCarsData.get(newIndex);
            if (carData != null && carData.mValid) {
                carData.clearPath();
            }
        }
    }

    public void updateMap(WorldMapGrid map) {
        mMap.update(map);
    }

    public void clearCarPath(int carIndex) {
        CarData carData = mCarsData.get(carIndex);
        if (carData != null) {
            carData.clearPath();
        }
    }

    public void setCarPathAs(int carIndex, MapPath path) {
        CarData carData = mCarsData.get(carIndex);
        if (carData != null) {
            carData.setPath(path);
        }
    }

    public void setCarDistance(int carIndex, FrontDistanceInfo distance) {
        CarData carData = mCarsData.get(carIndex);
        if (carData != null) {
            carData.setDistance(distance);
        }
    }

    public FrontDistanceInfo getCarDistance(int carIndex) {
        CarData carData = mCarsData.get(carIndex);
        if (carData != null) {
            return carData.getDistance();
        }
        return null;
    }

    public void updateCarExpectedPath(int carIndex, Queue<Point> path) {
        CarData carData = mCarsData.get(carIndex);
        if (carData != null) {
            carData.updateExpectedPath(path);
        }
    }

    public void updateCarPose(int carIndex, CarPose carPose) {
        CarData carData = mCarsData.get(carIndex);
        if (carData != null) {
            carData.updatePose(carPose);
        }
    }

    public CarPose getCarPose(int carIndex) {
        CarData carData = mCarsData.get(carIndex);
        if (carData != null) {
            return carData.getPose();
        }
        return null;
    }

    public void setCarStatus(int carIndex, CarStatus carStatus) {
        CarData carData = mCarsData.get(carIndex);
        if (carData != null) {
            carData.setStatus(carStatus);
        }
    }

    public CarStatus getCarStatus(int carIndex) {
        CarData carData = mCarsData.get(carIndex);
        if (carData != null) {
            return carData.getStatus();
        }
        return null;
    }

    public void updateCarConfig(int carIndex, CarConfigValue configValue) {
        CarData carData = mCarsData.get(carIndex);
        if (carData != null) {
            carData.updateConfig(configValue);
        }
    }

    public CarConfigValue getCarConfig(int carIndex) {
        CarData carData = mCarsData.get(carIndex);
        if (carData != null) {
            return carData.getConfig();
        }
        return null;
    }

    public String getCarStatusInfo(int carIndex) {
        CarData carData = mCarsData.get(carIndex);
        if (carData == null) {
            return "Invalid car";
        }
        String statusInfo = "";
        final CarStatus carStatus = carData.getStatus();
        if (!carStatus.isTangoConnected()) {
            statusInfo += "Tango has not connected";
        } else {
            final CarPose carPose = carData.getPose();
            final Point poseGrid = carPose.getCurrentGridIndex();
            if (poseGrid != null) {
                final float currentDegree = carPose.getDeviceYawDegree();
                statusInfo += "Current yaw:" + MathUtils.FORMAT_THREE_DECIMAL.format(currentDegree);
                statusInfo += "\n" + "Current Grid: (" + poseGrid.x + "," + poseGrid.y + ")";
            }
            final CarSpeed carSpeed = carData.getSpeed();
            if (carSpeed.valid()) {
                statusInfo += "\n" + "Move speed:" + MathUtils.FORMAT_THREE_DECIMAL.format(carSpeed.getMoveSpeed())
                        + "/" + MathUtils.FORMAT_THREE_DECIMAL.format(carSpeed.getInstantMoveSpeed());
                statusInfo += "\n" + "Turn speed:" + MathUtils.FORMAT_THREE_DECIMAL.format(carSpeed.getTurnSpeed())
                        + "/" + MathUtils.FORMAT_THREE_DECIMAL.format(carSpeed.getInstantTurnSpeed());
            }
            final Point targetPose = carStatus.getEngineTarget();
            if (targetPose != null) {
                statusInfo += "\n" + "Target Grid: (" + targetPose.x + "," + targetPose.y + ")";
            }
            final FrontDistanceInfo distanceInfo = carData.getDistance();
            if (distanceInfo != null) {
                statusInfo += "\n" + distanceInfo;
            }
            final String engineState = carStatus.getEngineState();
            if (engineState != null) {
                statusInfo += "\n" + engineState;
            }
            final String cmdStr = carStatus.getControlCommand();
            if (cmdStr != null) {
                statusInfo += "\nControl cmd:" + cmdStr;
            }
            final String debugInfo = carStatus.getControlDebugInfo();
            final float targetDegree = carStatus.getControlTargetDegree();
            if (debugInfo != null) {
                statusInfo += "\nControl info:" + debugInfo;
                statusInfo += "\nControl target yaw:" + targetDegree;
            }
        }
        final boolean usbConnected = carStatus.isUsbConnected();
        if (usbConnected) {
            statusInfo += "\nUsb is connected";
            statusInfo += "\nUsb received info: " + carStatus.getUsbReceiveInfo();
        } else {
            statusInfo += "\nUsb is disconnected";
        }
        return statusInfo;
    }
}
