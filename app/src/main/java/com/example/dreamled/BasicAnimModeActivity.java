package com.example.dreamled;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import yuku.ambilwarna.AmbilWarnaDialog;

public class BasicAnimModeActivity extends AppCompatActivity implements BleControllerInterface{

    BleController bleCtrl = new BleController();

    private static byte[] mode_state;

    //Buttons
    private Button btnDisconnectAndKill;
    private ToggleButton btnBasicAnimMode;
    private ToggleButton btnAudioBasedMode;
    private ToggleButton btnDirUp;
    private ToggleButton btnDirDown;
    private ToggleButton btnDirLeft;
    private ToggleButton btnDirRight;
    private ToggleButton btnDirYCenterOut;
    private ToggleButton btnDirYOutCenter;
    private ToggleButton btnStagNone;
    private ToggleButton btnStagAsc;
    private ToggleButton btnStagDesc;
    private ToggleButton btnStagMnt;
    private ToggleButton btnStagVal;
    private ToggleButton btnColor0;
    private ToggleButton btnColor1;
    private ToggleButton btnColor2;
    private ToggleButton btnColor3;
    private ToggleButton btnColor4;
    private ToggleButton btnColor5;
    private TextView btnCustColor0;
    private TextView btnCustColor1;
    private TextView btnCustColor2;
    private TextView btnCustColor3;
    private TextView btnCustColor4;
    private Button btnUpdateCustColor0;
    private Button btnUpdateCustColor1;
    private Button btnUpdateCustColor2;
    private Button btnUpdateCustColor3;
    private Button btnUpdateCustColor4;
    private View.OnClickListener customColorSelectedOnClickFunc;

    private ServiceConnection bleServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BleController.MyBinder binder = (BleController.MyBinder) service;
            bleCtrl = binder.getService();
            // Register this MainActivity's interface
            bleCtrl.setInterface(BasicAnimModeActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        // Bind this activity to the ble service
        Intent bindIntent = new Intent(this, BleController.class);
        bindService(bindIntent, bleServiceConnection, Context.BIND_AUTO_CREATE);

        Bundle intent = getIntent().getExtras();
        mode_state = new byte[Constants.STATE_LEN];
        mode_state[Constants.STATE_IDX_MODE] = intent.getByte(Constants.INTENT_EXTRA_MODE);
        mode_state[Constants.STATE_IDX_LED_DIR] = intent.getByte(Constants.INTENT_EXTRA_DIR);
        mode_state[Constants.STATE_IDX_STAGGER] = intent.getByte(Constants.INTENT_EXTRA_STAGGER);
        mode_state[Constants.STATE_IDX_COLOR] = intent.getByte(Constants.INTENT_EXTRA_COLOR);

        initButtonViews();
        initButtonViewStates();
        updateButtonViewStates(mode_state);

        customColorSelectedOnClickFunc = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // When the custom color is selected, update the mode to the new color and inform the ble device
                customColorSelected(v);
            }
        };
        /*
        View btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view){
                Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(mainIntent);
            }
        });
        */
    }

    @Override
    protected void onStart() {
        super.onStart();
        bleCtrl.readCharacteristic();
    }

    private void initButtonViewStates() {
        // Enable the basic animation mode and disable the audio based mode btn for now
        btnBasicAnimMode.setChecked(true);
        btnAudioBasedMode.setChecked(false);
        btnAudioBasedMode.setEnabled(false);

        // Set Led Direction buttons accordingly
        clrAllLedDirBtns();
        setLedDirBtn(mode_state[Constants.STATE_IDX_LED_DIR], true);

        // Set Stagger buttons accordingly
        clrAllStaggerBtns();
        setStaggerBtn(mode_state[Constants.STATE_IDX_LED_DIR], true);

        // Set Color buttons accordingly
        disableAllCustColorBtnSelections();
        updateButtonViewStates(mode_state);
    }

    private void updateButtonViewStates(byte[] mode_state) {
        // At the moment, just update the color selection based on the mode_state
        clearAllColorBtnSelections();
        setColorBtn(mode_state[Constants.STATE_IDX_COLOR], true);
    }

    private void setColorBtn(byte b, boolean val) {
        switch(b)
        {
            case(0):
                btnColor0.setChecked(val);
                break;
            case(1):
                btnColor1.setChecked(val);
                break;
            case(2):
                btnColor2.setChecked(val);
                break;
            case(3):
                btnColor3.setChecked(val);
                break;
            case(4):
                btnColor4.setChecked(val);
                break;
            case(5):
                btnColor5.setChecked(val);
                break;
            /* Indices after 5 are custom */
            case(Constants.NUM_STANDARD_COLORS):
                //btnCustColor0.setChecked(val);
                break;
            case(Constants.NUM_STANDARD_COLORS + 1):
                //btnCustColor1.setChecked(val);
                break;
            case(Constants.NUM_STANDARD_COLORS + 2):
                //btnCustColor2.setChecked(val);
                break;
            case(Constants.NUM_STANDARD_COLORS + 3):
                //btnCustColor3.setChecked(val);
                break;
            case(Constants.NUM_STANDARD_COLORS + 4):
                //btnCustColor4.setChecked(val);
                break;
        }
    }

    private void disableAllCustColorBtnSelections() {
        btnCustColor0.setBackgroundColor(Color.TRANSPARENT);
        btnCustColor1.setBackgroundColor(Color.TRANSPARENT);
        btnCustColor2.setBackgroundColor(Color.TRANSPARENT);
        btnCustColor3.setBackgroundColor(Color.TRANSPARENT);
        btnCustColor4.setBackgroundColor(Color.TRANSPARENT);
    }

    private void clearAllColorBtnSelections() {
        btnColor0.setChecked(false);
        btnColor1.setChecked(false);
        btnColor2.setChecked(false);
        btnColor3.setChecked(false);
        btnColor4.setChecked(false);
        btnColor5.setChecked(false);
    }

    private void setStaggerBtn(byte b, boolean val) {
        switch(b)
        {
            case(0):
                btnStagNone.setChecked(val);
                break;
            case(1):
                btnStagAsc.setChecked(val);
                break;
            case(2):
                btnStagDesc.setChecked(val);
                break;
            case(3):
                btnStagMnt.setChecked(val);
                break;
            case(4):
                btnStagVal.setChecked(val);
                break;
        }
    }

    private void clrAllStaggerBtns() {
        btnStagNone.setChecked(false);
        btnStagAsc.setChecked(false);
        btnStagDesc.setChecked(false);
        btnStagMnt.setChecked(false);
        btnStagVal.setChecked(false);
    }

    private void setLedDirBtn(byte b, boolean val) {
        switch(b)
        {
            case(0):
                btnDirUp.setChecked(val);
                break;
            case(1):
                btnDirDown.setChecked(val);
                break;
            case(2):
                btnDirLeft.setChecked(val);
                break;
            case(3):
                btnDirRight.setChecked(val);
                break;
            case(4):
                btnDirYCenterOut.setChecked(val);
                break;
            case(5):
                btnDirYOutCenter.setChecked(val);
                break;
        }
    }

    private void clrAllLedDirBtns() {
        btnDirUp.setChecked(false);
        btnDirDown.setChecked(false);
        btnDirLeft.setChecked(false);
        btnDirRight.setChecked(false);
        btnDirYCenterOut.setChecked(false);
        btnDirYOutCenter.setChecked(false);
    }

    private void initButtonViews() {
        btnDisconnectAndKill = (Button)findViewById(R.id.btnDisconnectAndKill);
        btnBasicAnimMode = (ToggleButton)findViewById(R.id.btnBasicAnimMode);
        btnAudioBasedMode = (ToggleButton)findViewById(R.id.btnAudioBasedMode);
        btnDirUp = (ToggleButton)findViewById(R.id.btnDirUp);
        btnDirDown = (ToggleButton)findViewById(R.id.btnDirDown);
        btnDirLeft = (ToggleButton)findViewById(R.id.btnDirLeft);
        btnDirRight = (ToggleButton)findViewById(R.id.btnDirRight);
        btnDirYCenterOut = (ToggleButton)findViewById(R.id.btnDirYCenterOut);
        btnDirYOutCenter = (ToggleButton)findViewById(R.id.btnDirYOutCenter);
        btnStagNone = (ToggleButton)findViewById(R.id.btnStagNone);
        btnStagAsc = (ToggleButton)findViewById(R.id.btnStagAsc);
        btnStagDesc = (ToggleButton)findViewById(R.id.btnStagDesc);
        btnStagMnt = (ToggleButton)findViewById(R.id.btnStagMnt);
        btnStagVal = (ToggleButton)findViewById(R.id.btnStagVal);
        btnColor0 = (ToggleButton)findViewById(R.id.btnColor0);
        btnColor1 = (ToggleButton)findViewById(R.id.btnColor1);
        btnColor2 = (ToggleButton)findViewById(R.id.btnColor2);
        btnColor3 = (ToggleButton)findViewById(R.id.btnColor3);
        btnColor4 = (ToggleButton)findViewById(R.id.btnColor4);
        btnColor5 = (ToggleButton)findViewById(R.id.btnColor5);
        btnCustColor0 = (TextView) findViewById(R.id.btnCustColor0);
        btnCustColor1 = (TextView) findViewById(R.id.btnCustColor1);
        btnCustColor2 = (TextView) findViewById(R.id.btnCustColor2);
        btnCustColor3 = (TextView) findViewById(R.id.btnCustColor3);
        btnCustColor4 = (TextView) findViewById(R.id.btnCustColor4);
        btnUpdateCustColor0 = (Button)findViewById(R.id.btnUpdateCustColor0);
        btnUpdateCustColor1 = (Button)findViewById(R.id.btnUpdateCustColor1);
        btnUpdateCustColor2 = (Button)findViewById(R.id.btnUpdateCustColor2);
        btnUpdateCustColor3 = (Button)findViewById(R.id.btnUpdateCustColor3);
        btnUpdateCustColor4 = (Button)findViewById(R.id.btnUpdateCustColor4);
    }

    @Override
    public void timeoutOccurred() {
        // If we are using this interface, we know we aren't on the main activity, transition to it
        Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(mainIntent);
    }

    @Override
    public void onCharacteristicRead(BluetoothGattCharacteristic characteristic) {
        mode_state = characteristic.getValue();
        updateButtonViewStates(mode_state);
    }

    @Override
    public void onCharacteristicWrite(BluetoothGattCharacteristic characteristic) {
        // Get the latest state that we wrote
        mode_state = characteristic.getValue();
        updateButtonViewStates(mode_state);
    }

    @Override
    public void onBatchScanResults(List<ScanResult> results) {

    }

    @Override
    public void onScanResult(ScanResult result) {

    }

    public void colorSelected(View view) {
        byte colorIdx = getColorIdxByView(view);
        if(colorIdx != Byte.MIN_VALUE)
        {
            mode_state[Constants.STATE_IDX_COLOR] = colorIdx;
            bleCtrl.writeCharacteristic(mode_state);
        }
    }

    private byte getColorIdxByView(View view) {
        byte idx;
        switch(view.getId())
        {
            case(R.id.btnColor0):
                idx = 0;
                break;
            case(R.id.btnColor1):
                idx = 1;
                break;
            case(R.id.btnColor2):
                idx = 2;
                break;
            case(R.id.btnColor3):
                idx = 3;
                break;
            case(R.id.btnColor4):
                idx = 4;
                break;
            case(R.id.btnColor5):
                idx = 5;
                break;
            default:
                idx = Byte.MIN_VALUE;
                break;
        }
        return idx;
    }

    public void updateCustomColor(View view) {
        byte colorIdx = getCustColorIdxByView(view);
        if(colorIdx != Byte.MIN_VALUE)
        {
            // We have the custom ColorIdx and the view
            // Time to create the dialog to prompt the user to pick a new color
            AmbilWarnaDialog customColorDialog = new AmbilWarnaDialog(this, 0x000000, new AmbilWarnaDialog.OnAmbilWarnaListener() {

                @Override
                public void onCancel(AmbilWarnaDialog dialog) {
                    // Don't do anything
                }

                @Override
                public void onOk(AmbilWarnaDialog dialog, int color) {
                    // Update the corresponding custom color textview (to show the color selected)
                    TextView customColorTv = getCustomColorViewByIdx(colorIdx);
                    if(customColorTv != null)
                    {
                        if(color != Color.BLACK) {
                            customColorTv.setBackgroundColor(color);
                            // Color updated, we need to tell the ble device of this change
                            byte[] updateCustomColorCmd = createCustomColorCmd(colorIdx, color);
                            bleCtrl.writeCmdCharacteristic(updateCustomColorCmd);
                            // We also need to make the custom color text view clickable
                            customColorTv.setOnClickListener(customColorSelectedOnClickFunc);
                        }
                        else{
                            // It's black (not a valid color in this situation
                            // Set it to transparent
                            customColorTv.setBackgroundColor(Color.TRANSPARENT);

                            // Next we want to make it not clickable
                            customColorTv.setOnClickListener(null);
                        }
                    }
                }
            });
            customColorDialog.show();
        }
    }

    private byte[] createCustomColorCmd(byte colorIdx, int color) {
        byte[] cmd = new byte[5];
        cmd[0] = 1; // Command Type is Upddate Custom Color
        cmd[1] = colorIdx; // Color Idx that we are updating
        cmd[2] = (byte)Color.red(color);
        cmd[3] = (byte)Color.green(color);
        cmd[4] = (byte)Color.blue(color);
        return cmd;
    }

    private TextView getCustomColorViewByIdx(byte colorIdx) {
        TextView customColorTv;
        switch(colorIdx)
        {
            case(Constants.NUM_STANDARD_COLORS):
                customColorTv = btnCustColor0;
                break;
            case(Constants.NUM_STANDARD_COLORS + 1):
                customColorTv = btnCustColor1;
                break;
            case(Constants.NUM_STANDARD_COLORS + 2):
                customColorTv = btnCustColor2;
                break;
            case(Constants.NUM_STANDARD_COLORS + 3):
                customColorTv = btnCustColor3;
                break;
            case(Constants.NUM_STANDARD_COLORS + 4):
                customColorTv = btnCustColor4;
                break;
            default:
                customColorTv = null;
        }
        return customColorTv;
    }

    private byte getCustColorIdxByView(View view) {
        byte idx;
        switch(view.getId())
        {
            case(R.id.btnUpdateCustColor0):
                idx = Constants.NUM_STANDARD_COLORS + 0;
                break;
            case(R.id.btnUpdateCustColor1):
                idx = Constants.NUM_STANDARD_COLORS + 1;
                break;
            case(R.id.btnUpdateCustColor2):
                idx = Constants.NUM_STANDARD_COLORS + 2;
                break;
            case(R.id.btnUpdateCustColor3):
                idx = Constants.NUM_STANDARD_COLORS + 3;
                break;
            case(R.id.btnUpdateCustColor4):
                idx = Constants.NUM_STANDARD_COLORS + 4;
                break;
            default:
                idx = Byte.MIN_VALUE;
                break;
        }
        return idx;
    }

    private byte getCustColorIdxByTextView(View view) {
        byte idx;
        switch(view.getId())
        {
            case(R.id.btnCustColor0):
                idx = Constants.NUM_STANDARD_COLORS + 0;
                break;
            case(R.id.btnCustColor1):
                idx = Constants.NUM_STANDARD_COLORS + 1;
                break;
            case(R.id.btnCustColor2):
                idx = Constants.NUM_STANDARD_COLORS + 2;
                break;
            case(R.id.btnCustColor3):
                idx = Constants.NUM_STANDARD_COLORS + 3;
                break;
            case(R.id.btnCustColor4):
                idx = Constants.NUM_STANDARD_COLORS + 4;
                break;
            default:
                idx = Byte.MIN_VALUE;
                break;
        }
        return idx;
    }

    public void customColorSelected(View view) {
        byte colorIdx = getCustColorIdxByTextView(view);
        if(colorIdxIsInRange(colorIdx)) {
            mode_state[Constants.STATE_IDX_COLOR] = colorIdx;
            bleCtrl.writeCharacteristic(mode_state);
        }
    }

    private boolean colorIdxIsInRange(byte colorIdx) {
        return ((colorIdx > 0) && (colorIdx <= Constants.NUM_STANDARD_COLORS + Constants.NUM_CUSTOM_COLORS));
    }
}
