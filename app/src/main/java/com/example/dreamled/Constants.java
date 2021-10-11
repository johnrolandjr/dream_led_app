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
    public static final String str_ms_uuid      = "00002423-1212-efde-1523-785feabcd123";
    public static final String str_ms_char_uuid = "00002424-1212-efde-1523-785feabcd123";

    public static final ParcelUuid ms_UUID = ParcelUuid
            .fromString(Constants.str_ms_uuid);
    public static final ParcelUuid ms_char_UUID = ParcelUuid
            .fromString(Constants.str_ms_char_uuid);

    public static final int REQUEST_ENABLE_BT = 1;
    public static final int GATT_MIN_MTU_SIZE = 23;
    public static final int GATT_MAX_MTU_SIZE = 517;

    public static final int GATT_TIMEOUT = 8;

    public static final String INTENT_EXTRA_MODE = "state_mode";
    public static final String INTENT_EXTRA_DIR = "state_dir";
    public static final String INTENT_EXTRA_STAGGER = "state_stagger";
    public static final String INTENT_EXTRA_COLOR = "state_color";

    public static final int NUM_STANDARD_COLORS = 6;

    public static final int STATE_IDX_MODE    = 0;
    public static final int STATE_IDX_LED_DIR = 1;
    public static final int STATE_IDX_STAGGER = 2;
    public static final int STATE_IDX_COLOR   = 3;
    public static final int STATE_LEN         = 4;

    public static String bleDeviceName = "Dream Led";

    // BLE Device Modes
    public static final int DEV_MODE_NOT_CONNECTED = -1;
    public static final int DEV_MODE_BASIC_ANIM = 0;
    public static final int DEV_MODE_AUDIO_BASED = 1;
}
