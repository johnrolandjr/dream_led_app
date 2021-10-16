package com.example.dreamled;

import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import android.content.Context;
import android.content.Intent;
import android.media.audiofx.DynamicsProcessing;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class BleController extends Service {

    private static final String TAG = "BleService";
    private static final int MAX_TRIES = 3;
    public MyBinder mBinder = new MyBinder();

    // Registered interface
    private BleControllerInterface bleCtrlIf;
    private ScanCallback mScanCallback;
    private static Handler mHandler;
    private static BluetoothGatt bleGatt;

    boolean scanning;
    private ArrayList<UUID> uuids;
    BluetoothGattCharacteristic mainChar;
    BluetoothGattCharacteristic cmdChar;
    private BluetoothAdapter BA;
    private BluetoothLeScanner bleScanner;
    private static int actualMaxMtuSize;

    private static Queue<Runnable> bleCommandQueue;
    private static boolean commandQueueBusy;
    private static boolean isRetrying;
    private static int nrTries;

    private static final long SCAN_PERIOD = 5000;
    private static BluetoothDevice btDev;
    private static Context context;

    /*
     * onCreate is called only once. When the service is first started.
     * If the service has already been started, it will not call this function.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
        scanning = false;
        uuids = new ArrayList<UUID>();
        BA = BluetoothAdapter.getDefaultAdapter();
        bleScanner = BA.getBluetoothLeScanner();
        mainChar = null;
        commandQueueBusy = false;
        isRetrying = false;
        nrTries = 0;
        bleCommandQueue = new LinkedList<>();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /*
     * onBind is called when another activity/application calls bindService(). We must return an interface
     * that the clients can use to communicate with the service via an IBinder.
     */
    @Nullable
    @Override
    public MyBinder onBind(Intent intent) {
        return mBinder;
    }

    public void setInterface(BleControllerInterface activityBleInterface)
    {
        bleCtrlIf = activityBleInterface;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }

    public Set<BluetoothDevice> getBondedDevices() {
        checkNullBA();
        return BA.getBondedDevices();
    }

    public boolean isEnabled() {
        checkNullBA();
        return BA.isEnabled();
    }

    private void checkNullBA() {
        if(BA == null)
        {
            BA = BluetoothAdapter.getDefaultAdapter();
        }
    }

    public void setContext(Context applicationContext) {
        context = applicationContext;
    }

    public class MyBinder extends Binder {
        BleController getService() {
            return BleController.this;
        }
    }

    private BluetoothGattCallback bleGattCb = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if(status != BluetoothGatt.GATT_SUCCESS){
                //gatt.disconnect();
                completedCommand(status);
                return;
            }
            if(newState == BluetoothProfile.STATE_CONNECTED){
                bleGatt = gatt;
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED){
                bleGatt = null;
                gatt.close();
            } else {
                // Some other state change that I've decided to not do anything extra
            }
            completedCommand(status);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status != BluetoothGatt.GATT_SUCCESS)
            {
                Log.e(TAG, "ERROR: Service Discovery Failed.");
                completedCommand(status);
                return;
            }

            /*
            for(BluetoothGattService service : gatt.getServices()) {
                uuids.add(service.getUuid());
            }
            */
            bleGatt = gatt;
            // Remove this ble command from the queue
            completedCommand(status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            actualMaxMtuSize = Constants.GATT_MIN_MTU_SIZE;
            if (status != BluetoothGatt.GATT_SUCCESS)
            {
                Log.e(TAG, "ERROR: Failed to retrieve the mtu size.");
                completedCommand(status);
                return;
            }

            if(status == BluetoothGatt.GATT_SUCCESS)
            {
                actualMaxMtuSize = mtu;
            }
            // Let's determine if our main mode characteristic is readable

            BluetoothGattService mainService = gatt.getService(UUID.fromString(Constants.str_ms_uuid));
            if(mainService != null)
            {
                mainChar = mainService.getCharacteristic(UUID.fromString(Constants.str_ms_char_uuid));
                if(mainChar != null)
                {
                    readCharacteristic();
                }
            }
            // Remove this ble command from the queue
            completedCommand(status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status != BluetoothGatt.GATT_SUCCESS)
            {
                Log.e(TAG, "ERROR: Read for characteristic failed.");
                completedCommand(status);
                return;
            }

            // Characteristic was read and valid, perform necessary operations
            bleCtrlIf.onCharacteristicRead(characteristic);
            // Remove this ble command from the queue
            completedCommand(status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status != BluetoothGatt.GATT_SUCCESS)
            {
                Log.e(TAG, "ERROR: Write for characteristic failed.");
                completedCommand(status);
                return;
            }
            // Characteristic was sent successfully, perform necessary operations
            bleCtrlIf.onCharacteristicWrite(characteristic);
            // Remove this ble command from the queue
            completedCommand(status);
        }
    };

    // ------- Service functions ------------
    public void setDevice(BluetoothDevice bleDev)
    {
        btDev = bleDev;
    }

    public boolean isScanning(){
        return scanning;
    }

    /**
     * Custom ScanCallback object - adds to adapter on success, displays error on failure.
     */
    private class SampleScanCallback extends ScanCallback {

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            bleCtrlIf.onBatchScanResults(results);
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            bleCtrlIf.onScanResult(result);
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
        builder.setServiceUuid(Constants.ms_UUID);
        scanFilters.add(builder.build());

        return scanFilters;
    }

    /**
     * Return a {@link ScanSettings} object set to use low power (to preserve battery life).
     */
    private ScanSettings buildScanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_BALANCED);
        return builder.build();
    }

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
            mScanCallback = new BleController.SampleScanCallback();
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
        scanning = false;
    }

    public void connectGatt(BluetoothDevice device) {
        if(device == null) {
            Log.e(TAG, "ERROR: Device is null, ignoring connect request.");
            return;
        }
        bleGatt = device.connectGatt(this, false, bleGattCb);
    }

    private boolean requestMtuSize() {
        if(bleGatt == null){
            Log.e(TAG, "ERROR: Gatt is null, ignoring read request.");
            return false;
        }
        // Pass all the checks, queue up the mtu size request and return true
        boolean result = bleCommandQueue.add(new Runnable() {
            @Override
            public void run() {
                if(!bleGatt.requestMtu(Constants.GATT_MAX_MTU_SIZE)) {
                    Log.e(TAG, "ERROR: Failed trying to request the max mtu size.");
                    completedCommand(1);
                }else{
                    Log.d(TAG, "Requesting the MTU Size.");
                    nrTries++;
                }
            }
        });

        if(result) {
            nextCommand();
        } else {
            Log.e(TAG, "ERROR: Could not queue read characteristic command");
        }
        return result;
    }

    private boolean queueConnect() {
        boolean result = bleCommandQueue.add(new Runnable() {
            @Override
            public void run() {
                if(btDev == null) {
                    Log.e(TAG, "ERROR: Device is null, ignoring connect request.");
                    nrTries++;
                    return;
                }
                bleGatt = btDev.connectGatt(context, false, bleGattCb);
                if(bleGatt == null) {
                    Log.e(TAG, "ERROR: Failed connecting to the device.");
                    completedCommand(1);
                }else{
                    Log.d(TAG, "Connecting to the device.");
                    nrTries++;
                }
            }
        });
        return result;
    }
    private boolean queueDiscoverService(){
        boolean result = bleCommandQueue.add(new Runnable() {
            @Override
            public void run() {
                if(bleGatt == null){
                    Log.e(TAG, "ERROR: Gatt is null. Should have connected to it prior to this command.");
                    nrTries++;
                }
                if(!bleGatt.discoverServices()) {
                    Log.e(TAG, "ERROR: Failed trying to discover the services.");
                    completedCommand(1);
                }else{
                    Log.d(TAG, "Reading the available services.");
                    nrTries++;
                }
            }
        });
        return result;
    }

    private boolean queueReadCharacteristic(){
        boolean result = bleCommandQueue.add(new Runnable() {
            @Override
            public void run() {
                if(bleGatt == null){
                    Log.e(TAG, "ERROR: Gatt is null, ignoring read request.");
                    nrTries++;
                    return;
                }
                BluetoothGattService mainService = bleGatt.getService(UUID.fromString(Constants.str_ms_uuid));
                if(mainService == null)
                {
                    Log.e(TAG, "ERROR: Couldn't retrieve service from Gatt.");
                    nrTries++;
                    return;
                }
                mainChar = mainService.getCharacteristic(UUID.fromString(Constants.str_ms_char_uuid));
                if(mainChar == null)
                {
                    Log.e(TAG, "ERROR: Couldn't find characteristic in service.");
                    nrTries++;
                    return;
                }
                if((mainChar.getProperties() & PROPERTY_READ) == 0)
                {
                    Log.e(TAG, "ERROR: Characteristic is not readible.");
                    nrTries++;
                    return;
                }

                if(!bleGatt.readCharacteristic(mainChar)) {
                    Log.e(TAG, "ERROR: Failed reading the characteristic.");
                    completedCommand(1);
                }else{
                    Log.d(TAG, "Reading main characteristic.");
                    nrTries++;
                }
            }
        });
        return result;
    }

    private boolean queueWriteCharacteristic(byte[] mode_state) {
        boolean result = bleCommandQueue.add(new Runnable() {
            @Override
            public void run() {
                if(bleGatt == null)
                {
                    Log.e(TAG, "ERROR: Gatt is null, ignoring write request.");
                    nrTries++;
                    return;
                }
                BluetoothGattService mainService = bleGatt.getService(UUID.fromString(Constants.str_ms_uuid));
                mainChar = null;
                if(mainService == null){
                    Log.e(TAG, "ERROR: Could not find main service.");
                    nrTries++;
                    return;
                }

                mainChar = mainService.getCharacteristic(UUID.fromString(Constants.str_ms_char_uuid));
                if(mainChar == null){
                    Log.e(TAG, "ERROR: Could not find characteristic.");
                    nrTries++;
                    return;
                }

                int properties = mainChar.getProperties();
                if((properties & PROPERTY_WRITE_NO_RESPONSE) == 0){
                    Log.e(TAG, "ERROR: Main Characteristic is not writable (no response wrtie type).");
                    nrTries++;
                    return;
                }

                byte[] newMode = new byte[4];
                for(int i=0; i<4; i++) {
                    newMode[i] = mode_state[i];
                }
                mainChar.setValue(newMode);
                mainChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                if(!bleGatt.writeCharacteristic(mainChar)) {
                    Log.e(TAG, "ERROR: Failed writing the characteristic.");
                    completedCommand(1);
                }else{
                    Log.d(TAG, "Writing the main characteristic.");
                    nrTries++;
                }
            }
        });

        return result;
    }
    private boolean queueWriteCmdCharacteristic(byte[] cmd) {
        boolean result = bleCommandQueue.add(new Runnable() {
            @Override
            public void run() {
                if(bleGatt == null)
                {
                    Log.e(TAG, "ERROR: Gatt is null, ignoring write request.");
                    nrTries++;
                    return;
                }
                BluetoothGattService mainService = bleGatt.getService(UUID.fromString(Constants.str_ms_uuid));
                cmdChar = null;
                if(mainService == null){
                    Log.e(TAG, "ERROR: Could not find main service.");
                    nrTries++;
                    return;
                }

                cmdChar = mainService.getCharacteristic(UUID.fromString(Constants.str_cmd_char_uuid));
                if(cmdChar == null){
                    Log.e(TAG, "ERROR: Could not find characteristic.");
                    nrTries++;
                    return;
                }

                int properties = cmdChar.getProperties();
                if((properties & PROPERTY_WRITE_NO_RESPONSE) == 0){
                    Log.e(TAG, "ERROR: Cmd Characteristic is not writable (no response wrtie type).");
                    nrTries++;
                    return;
                }

                cmdChar.setValue(cmd);
                cmdChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                if(!bleGatt.writeCharacteristic(cmdChar)) {
                    Log.e(TAG, "ERROR: Failed writing the characteristic.");
                    completedCommand(1);
                }else{
                    Log.d(TAG, "Writing the cmd characteristic.");
                    nrTries++;
                }
            }
        });
        return result;
    }

    private boolean queueDisconnect(){
        boolean result = bleCommandQueue.add(new Runnable() {
            @Override
            public void run() {
                if(bleGatt == null) {
                    Log.e(TAG, "ERROR: Device is null, ignoring disconnect request.");
                    nrTries++;
                    return;
                }
                bleGatt.disconnect();
                Log.d(TAG, "Disconnecting");
            }
        });
        return result;
    }

    public boolean readCharacteristic(){

        // We are going to queue up the following:
        /*
            Gatt Connect
            Discover Service
            Read Characteristic
            Gatt Disconnect
         */
        boolean bQueued = queueConnect();
        if(!bQueued)
        {
            Log.e(TAG,"ERROR: Couldn't queue up connect.");
            emptyQueue();
            return false;
        }

        bQueued = queueDiscoverService();
        if(!bQueued)
        {
            Log.e(TAG,"ERROR: Couldn't queue up discover service.");
            emptyQueue();
            return false;
        }

        bQueued = queueReadCharacteristic();
        if(!bQueued)
        {
            Log.e(TAG,"ERROR: Couldn't queue up read characteristic.");
            emptyQueue();
            return false;
        }

        bQueued = queueDisconnect();
        if(!bQueued)
        {
            Log.e(TAG,"ERROR: Couldn't queue up disconnect.");
            emptyQueue();
            return false;
        }
        else
        {
            nextCommand();
        }
        return true;
    }

    public boolean writeCharacteristic(byte[] mode_state) {
        // We are going to queue up the following:
        /*
            Gatt Connect
            Discover Service
            Write the new Characteristic
            Gatt Disconnect
         */
        boolean bQueued = queueConnect();
        if(!bQueued)
        {
            Log.e(TAG,"ERROR: Couldn't queue up connect.");
            emptyQueue();
            return false;
        }

        bQueued = queueDiscoverService();
        if(!bQueued)
        {
            Log.e(TAG,"ERROR: Couldn't queue up discover service.");
            emptyQueue();
            return false;
        }

        bQueued = queueWriteCharacteristic(mode_state);
        if(!bQueued)
        {
            Log.e(TAG,"ERROR: Couldn't queue up the write characteristic.");
            emptyQueue();
            return false;
        }

        bQueued = queueDisconnect();
        if(!bQueued)
        {
            Log.e(TAG,"ERROR: Couldn't queue up disconnect.");
            emptyQueue();
            return false;
        }
        else
        {
            nextCommand();
        }
        return true;
    }

    public boolean writeCmdCharacteristic(byte[] cmd) {
        // We are going to queue up the following:
        /*
            Gatt Connect
            Discover Service
            Write the new cmd Characteristic
            Gatt Disconnect
         */
        boolean bQueued = queueConnect();
        if(!bQueued)
        {
            Log.e(TAG,"ERROR: Couldn't queue up connect.");
            emptyQueue();
            return false;
        }

        bQueued = queueDiscoverService();
        if(!bQueued)
        {
            Log.e(TAG,"ERROR: Couldn't queue up discover service.");
            emptyQueue();
            return false;
        }

        bQueued = queueWriteCmdCharacteristic(cmd);
        if(!bQueued)
        {
            Log.e(TAG,"ERROR: Couldn't queue up the write cmd characteristic.");
            emptyQueue();
            return false;
        }

        bQueued = queueDisconnect();
        if(!bQueued)
        {
            Log.e(TAG,"ERROR: Couldn't queue up disconnect.");
            emptyQueue();
            return false;
        }
        else
        {
            nextCommand();
        }
        return true;
    }

    private void emptyQueue() {
        bleCommandQueue.clear();
        commandQueueBusy = false;
    }

    private void completedCommand(int status) {
        if(status != BluetoothGatt.GATT_SUCCESS){
            retryCommand();
        }
        else
        {
            commandQueueBusy = false;
            bleCommandQueue.poll();
            nextCommand();
        }
    }

    private void retryCommand(){
        commandQueueBusy = false;
        Runnable currentCommand = bleCommandQueue.peek();
        if(currentCommand != null) {
            if (nrTries >= MAX_TRIES){
                Log.v(TAG, "Max number of tries reached for ble command");
                bleCommandQueue.poll();
            } else {
                isRetrying = true;
            }
        }
        nextCommand();
    }

    private void nextCommand() {
        if(commandQueueBusy){
            return;
        }

        if (bleCommandQueue.size() > 0){
            final Runnable bluetoothCommand = bleCommandQueue.peek();
            commandQueueBusy = true;
            nrTries = 0;

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        bluetoothCommand.run();
                    } catch (Exception ex) {
                        Log.e(TAG, String.format("ERROR: Command exception."), ex);
                    }
                }
            });
        }
    }
}
