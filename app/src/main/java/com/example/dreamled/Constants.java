package com.example.dreamled;

import android.os.ParcelUuid;

/**
 * Constants for use in the Bluetooth Advertisements sample
 */
public class Constants {

    /**
     * UUID identified with this app - set as Service UUID for BLE Advertisements.
     *
     * Bluetooth requires a certain format for UUIDs associated with Services.
     */
    public static final ParcelUuid ms_UUID = ParcelUuid
            .fromString("00002423-1212-efde-1523-785feabcd123");
    public static final ParcelUuid ms_button_char_UUID = ParcelUuid
            .fromString("00002424-1212-efde-1523-785feabcd123");

    public static final int REQUEST_ENABLE_BT = 1;

    public static String bleDeviceName = "Dream Led";

    // BLE Device Modes
    public static final int DEV_MODE_NOT_CONNECTED = -1;
    public static final int DEV_MODE_BASIC_ANIM = 0;
    public static final int DEV_MODE_AUDIO_BASED = 1;
}
