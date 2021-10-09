package com.example.dreamled;

import android.bluetooth.BluetoothDevice;

public class DeviceInfoModel {

    private String deviceName, deviceHardwareAddress;
    private BluetoothDevice bleDev;

    public DeviceInfoModel(){}

    public DeviceInfoModel(BluetoothDevice bleDevice){
        this.bleDev = bleDevice;
        this.deviceName = bleDevice.getName();
        this.deviceHardwareAddress = bleDevice.getAddress();
    }

    public String getDeviceName(){return deviceName;}

    public String getDeviceHardwareAddress(){return deviceHardwareAddress;}

    public BluetoothDevice getDevice(){return bleDev;}
}