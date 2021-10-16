package com.example.dreamled;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import com.example.dreamled.BleController.MyBinder;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements BleControllerInterface {
    private static final String TAG = MainActivity.class.getSimpleName();

    private DeviceListAdapter discoveredDevAdapter;
    RecyclerView rvDiscoveredDev;


    private ArrayList<DeviceInfoModel> discoveredDevList;
    private onClickInterface onDevDiscoveredClickInterface;

    private static boolean mainActivityIsOpen;

    // Constants
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_BACKGROUND_LOCATION = 1;

    private static byte[] lastModeState;

    BleController bleCtrl = new BleController();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainActivityIsOpen = true;
        Intent intent = new Intent(this, BleController.class);
        bindService(intent, bleServiceConnection, Context.BIND_AUTO_CREATE);

        discoveredDevList = new ArrayList<>();
        onDevDiscoveredClickInterface = new onClickInterface() {
            @Override
            public void setClick(int pos) {
                DeviceInfoModel selectedBleDevice = discoveredDevList.get(pos);
                Toast.makeText(MainActivity.this, "Selected " + selectedBleDevice.getDeviceName() + ".", Toast.LENGTH_LONG).show();
                if (selectedBleDevice.getDeviceName().equals(Constants.bleDeviceName)) {
                    // Selected a Ble Device that corresponds to the device we've created in FW
                    // Stop scanning if we are still scanning
                    if (bleCtrl.isScanning()) {
                        bleCtrl.stopScanning();
                    }
                    // Update target ble device that we will be interacting from now on
                    bleCtrl.setDevice(selectedBleDevice.getDevice());
                    // Transition to basic animation mode activity with this device info
                    transitionToActivity();
                    //bleCtrl.connectGatt(selectedBleDevice.getDevice());
                }
            }
        };

        setDiscoveredRvAdapter();

        ask_for_needed_ble_permissions();

        // UI Initialization
        final Button buttonScan = findViewById(R.id.buttonScan);
        final Toolbar toolbar = findViewById(R.id.toolbar);
        //final RecyclerView rvPairedDev = findViewById(R.id.RV_pairedBleDevices);

        // Select Bluetooth Device
        buttonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // When Connect Button is clicked, we need to populate the device list

                // Get List of Paired Bluetooth Device
                Set<BluetoothDevice> pairedDevices = bleCtrl.getBondedDevices();
                List<Object> deviceList = new ArrayList<>();

                // Populate the paired device section
                if (pairedDevices.size() > 0) {
                    // There are paired devices. Get the name and address of each paired device.
                    for (BluetoothDevice device : pairedDevices) {
                        String deviceName = device.getName();
                        String deviceHardwareAddress = device.getAddress(); // MAC address
                        DeviceInfoModel deviceInfoModel = new DeviceInfoModel(device);
                        // If device is already in list, do not add it to the list
                        if (!deviceList.contains(deviceInfoModel) && deviceInfoModel.getDeviceName().equals(Constants.bleDeviceName)) {
                            deviceList.add(deviceInfoModel);
                        }
                    }
                    // Display paired device using recyclerView
                    // recyclerView = findViewById(R.id.RV_pairedBleDevices);
                    //recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
                    //DeviceListAdapter deviceListAdapter = new DeviceListAdapter(view.getContext(), deviceList);
                    //recyclerView.setAdapter(deviceListAdapter);
                    //recyclerView.setItemAnimator(new DefaultItemAnimator());
                }

                // Start discovery and populate the discovered device section
                bleCtrl.startScanning();
            }
        });
        /*// Button to ON/OFF LED on Arduino Board
        buttonToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String cmdText = null;
                String btnState = buttonToggle.getText().toString().toLowerCase();
                switch (btnState){
                    case "turn on":
                        buttonToggle.setText("Turn Off");
                        // Command to turn on LED on Arduino. Must match with the command in Arduino code
                        cmdText = "<turn on>";
                        break;
                    case "turn off":
                        buttonToggle.setText("Turn On");
                        // Command to turn off LED on Arduino. Must match with the command in Arduino code
                        cmdText = "<turn off>";
                        break;
                }
                // Send command to Arduino board
                connectedThread.write(cmdText);
            }
        });*/
    }

    private ServiceConnection bleServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MyBinder binder = (MyBinder) service;
            bleCtrl = binder.getService();
            // Register this MainActivity's interface
            bleCtrl.setInterface(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    private void transitionToActivity() {
        Intent act2Intent = new Intent(getApplicationContext(), BasicAnimModeActivity.class);
        startActivity(act2Intent);
    }

    private void setDiscoveredRvAdapter() {
        discoveredDevAdapter = new DeviceListAdapter(getApplicationContext(), discoveredDevList, onDevDiscoveredClickInterface);
        rvDiscoveredDev = findViewById(R.id.RV_discoveredBleDevices);
        rvDiscoveredDev.setHasFixedSize(false);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        rvDiscoveredDev.setLayoutManager(layoutManager);
        rvDiscoveredDev.setItemAnimator(new DefaultItemAnimator());
        rvDiscoveredDev.setAdapter(discoveredDevAdapter);
    }

    private void ask_for_needed_ble_permissions() {
        if (Build.VERSION.SDK_INT >= 29) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs fine location access");
                builder.setMessage("Please grant location access so this app can detect peripherals.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
                    }
                });
                builder.show();
            }
            if (this.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs background location access");
                builder.setMessage("Please grant location access so this app can detect peripherals.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_BACKGROUND_LOCATION);
                    }
                });
                builder.show();
            }
        }
    }
/*
    private void readStartingMode(){
        if(bleGatt != null)
        {
            UUID genServiceUuid = UUID.fromString(Constants.Service_UUID);
            BluetoothGattCharacteristic genChar = bleGatt.getService(genServiceUuid).getCharacteristic();
            bleGatt.readCharacteristic(genChar);
        }
    }
 */

    /* ============================ Terminate Connection at Back press ====================== */
    @Override
    public void onBackPressed() {
        //@future Terminate Bluetooth LE Connection and close app
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        startActivity(a);
    }

    /* ======================== Entering the app check for BLE enabled ==================== */
    @Override
    public void onResume() {
        super.onResume();
        mainActivityIsOpen = true;
        if (!bleCtrl.isEnabled()) {
            promptEnableBluetooth();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mainActivityIsOpen = false;
    }

    public void promptEnableBluetooth() {
        if (!bleCtrl.isEnabled()) {
            Intent turnOnBle = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOnBle, Constants.REQUEST_ENABLE_BT);
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGattCharacteristic characteristic){
        if(mainActivityIsOpen)
        {
            // If we are still in the main Activity, transition to corresponding activity
            lastModeState = characteristic.getValue();
            transitionToActivity();
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGattCharacteristic characteristic) {
        // Don't do anything, we shouldn't be writing to the state in this activity
    }

    @Override
    public void onBatchScanResults(List<ScanResult> results){
        for (ScanResult result : results) {
            discoveredDevAdapter.add(result);
        }
        discoveredDevAdapter.notifyDataSetChanged();
    }

    @Override
    public void onScanResult(ScanResult result){
        discoveredDevAdapter.add(result);
        discoveredDevAdapter.notifyDataSetChanged();
    }
}