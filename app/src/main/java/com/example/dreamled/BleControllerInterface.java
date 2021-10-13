package com.example.dreamled;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.ScanResult;

import java.util.List;

public interface BleControllerInterface {
    // When a timeout Occurs, this function will be called
    void timeoutOccurred();

    // When a characteristic is read successfully, this function will be called
    void onCharacteristicRead(BluetoothGattCharacteristic characteristic);

    // When a characteristic is written successfully, this function will be called
    void onCharacteristicWrite(BluetoothGattCharacteristic characteristic);

    void onBatchScanResults(List<ScanResult> results);

    void onScanResult(ScanResult result);
}
