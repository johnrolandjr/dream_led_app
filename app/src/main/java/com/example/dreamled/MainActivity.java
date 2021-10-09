package com.example.dreamled;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
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

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * Stops scanning after 5 seconds.
     */
    private static final long SCAN_PERIOD = 5000;
    private static BluetoothAdapter BA;
    private static boolean scanning = false;
    private BluetoothLeScanner bleScanner;
    private ScanCallback mScanCallback;
    private Handler mHandler;
    private DeviceListAdapter discoveredDevAdapter;
    RecyclerView rvDiscoveredDev;
    private BluetoothGatt bleGatt;
    private BluetoothGattCallback bleGattCb;

    private ArrayList<DeviceInfoModel> discoveredDevList;
    private onClickInterface onDevDiscoveredClickInterface;

    private ArrayList<UUID> uuids;

    // Constants
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_BACKGROUND_LOCATION = 1;

    private final int NOTCONNECTED = 0;
    private final int SEARCHING = 1;
    private final int FOUND = 2;
    private final int CONNECTED = 3;
    private final int DISCOVERING = 4;
    private final int COMMUNICATING = 5;
    private final int DISCONNECTING = 6;
    private final int INTERROGATE = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        discoveredDevList = new ArrayList<>();
        onDevDiscoveredClickInterface = new onClickInterface() {
            @Override
            public void setClick(int pos) {
                DeviceInfoModel selectedBleDevice = discoveredDevList.get(pos);
                Toast.makeText(MainActivity.this,"Selected " + selectedBleDevice.getDeviceName() + ".",Toast.LENGTH_LONG).show();
                if(selectedBleDevice.getDeviceName().equals(Constants.bleDeviceName))
                {
                    // Selected a Ble Device that corresponds to the device we've created in FW
                    // Stop scanning if we are still scanning
                    if(scanning) {
                        stopScanning();
                    }
                    bleGatt = selectedBleDevice.getDevice().connectGatt(MainActivity.this, false, bleGattCb);
                }
            }
        };

        uuids = new ArrayList<UUID>();

        bleGattCb = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if(status != BluetoothGatt.GATT_SUCCESS){
                    gatt.disconnect();
                    return;
                }
                if(newState == BluetoothProfile.STATE_CONNECTED){
                    bleGatt = gatt;
                    gatt.discoverServices();
                }
                else if (newState == BluetoothProfile.STATE_DISCONNECTED){
                    bleGatt = null;
                    gatt.close();
                } else {
                    // Some other state change that I've decided to not do anything extra
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                for(BluetoothGattService service : gatt.getServices()) {
                    uuids.add(service.getUuid());
                }
                gatt.disconnect();
            }
        };

        setDiscoveredRvAdapter();

        ask_for_needed_ble_permissions();
        
        // Initialize class object to the default bluetooth adapter for future use
        BA = BluetoothAdapter.getDefaultAdapter();
        bleScanner = BA.getBluetoothLeScanner();
        mHandler = new Handler();

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
                Set<BluetoothDevice> pairedDevices = BA.getBondedDevices();
                List<Object> deviceList = new ArrayList<>();

                // Populate the paired device section
                if (pairedDevices.size() > 0) {
                    // There are paired devices. Get the name and address of each paired device.
                    for (BluetoothDevice device : pairedDevices) {
                        String deviceName = device.getName();
                        String deviceHardwareAddress = device.getAddress(); // MAC address
                        DeviceInfoModel deviceInfoModel = new DeviceInfoModel(device);
                        // If device is already in list, do not add it to the list
                        if (!deviceList.contains(deviceInfoModel) && deviceInfoModel.getDeviceName() == "Beau") {
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
                startScanning();
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
        View btnOpenActivity = findViewById(R.id.btnOpenActivity);
        btnOpenActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                Intent act2Intent = new Intent(getApplicationContext(), BasicAnimModeActivity.class);
                startActivity(act2Intent);
            }
        });
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

    /* ============================ Terminate Connection at Back press ====================== */
    @Override
    public void onBackPressed() {
        //@future Terminate Bluetooth LE Connection and close app
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }

    /* ======================== Entering the app check for BLE enabled ==================== */
    @Override
    public void onResume() {
        super.onResume();
        if (!BA.isEnabled()) {
            promptEnableBluetooth();
        }
    }

    public void promptEnableBluetooth() {
        if (!BA.isEnabled()) {
            Intent turnOnBle = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOnBle, Constants.REQUEST_ENABLE_BT);
        }
    }

    /* ==== Class functions ==== */
    /**
     * Start scanning for BLE Advertisements (& set it up to stop after a set period of time).
     */
    public void startScanning() {
        if (mScanCallback == null) {
            Log.d(TAG, "Starting Scanning");

            // Will stop the scanning after a set time.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScanning();
                }
            }, SCAN_PERIOD);

            // Kick off a new scan.
            mScanCallback = new SampleScanCallback();
            bleScanner.startScan(buildScanFilters(), buildScanSettings(), mScanCallback);

            String toastText = getString(R.string.scan_start_toast) + " "
                    + TimeUnit.SECONDS.convert(SCAN_PERIOD, TimeUnit.MILLISECONDS) + " "
                    + getString(R.string.seconds);
            Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_LONG).show();
            scanning = true;
        } else {
            Toast.makeText(getApplicationContext(), R.string.already_scanning, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Stop scanning for BLE Advertisements.
     */
    public void stopScanning() {
        Log.d(TAG, "Stopping Scanning");

        // Stop the scan, wipe the callback.
        bleScanner.stopScan(mScanCallback);
        mScanCallback = null;

        // Even if no new results, update 'last seen' times.
        discoveredDevAdapter.notifyDataSetChanged();
        scanning = false;
    }

    /**
     * Custom ScanCallback object - adds to adapter on success, displays error on failure.
     */
    private class SampleScanCallback extends ScanCallback {

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            for (ScanResult result : results) {
                discoveredDevAdapter.add(result);
            }
            discoveredDevAdapter.notifyDataSetChanged();
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            discoveredDevAdapter.add(result);
            discoveredDevAdapter.notifyDataSetChanged();
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    }

    /**
     * Return a List of {@link ScanFilter} objects to filter by Service UUID.
     */
    private List<ScanFilter> buildScanFilters() {
        List<ScanFilter> scanFilters = new ArrayList<>();

        ScanFilter.Builder builder = new ScanFilter.Builder();
        // Comment out the below line to see all BLE devices around you
        //builder.setServiceUuid(Constants.Service_UUID);
        scanFilters.add(builder.build());

        return scanFilters;
    }

    /**
     * Return a {@link ScanSettings} object set to use low power (to preserve battery life).
     */
    private ScanSettings buildScanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
        return builder.build();
    }
}