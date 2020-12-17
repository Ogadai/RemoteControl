package com.ogadai.alee.homerc;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by alee on 29/08/2016.
 */
public class ConnectionDetails {
    private String mAddress;
    private boolean mSteering;
    private boolean mMotor1Swap;
    private boolean mMotor2Swap;

    private boolean mDPad;

    public static final String CONNECTION_PREFFILE = "rc_connection";
    public static final String ADDRESSPREF = "address";
    public static final String STEERINGPREF = "steering";
    public static final String MOTOR1SWAPPREF = "motor1swap";
    public static final String MOTOR2SWAPPREF = "motor2swap";
    public static final String DPADPREF = "dpad";

    public String getAddress() { return mAddress; }
    public void setAddress(String address) { mAddress = address; }

    public boolean getSteering() { return mSteering; }
    public void setSteering(boolean steering) { mSteering = steering; }

    public boolean getMotor1Swap() { return mMotor1Swap; }
    public void setMotor1Swap(boolean swap) { mMotor1Swap = swap; }

    public boolean getMotor2Swap() { return mMotor2Swap; }
    public void setMotor2Swap(boolean swap) { mMotor2Swap = swap; }

    public boolean getDPad() { return mDPad; }
    public void setDPad(boolean dPad) { mDPad = dPad; }

    public void readSettings(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(CONNECTION_PREFFILE, Context.MODE_PRIVATE);
        mAddress = prefs.getString(ADDRESSPREF, "ws://raspberrypirc:8080");
        // Disabled ws, always use BLE:
        mAddress = "BLE:";

        mSteering = prefs.getBoolean(STEERINGPREF, false);

        mMotor1Swap = prefs.getBoolean(MOTOR1SWAPPREF, false);
        mMotor2Swap = prefs.getBoolean(MOTOR2SWAPPREF, false);
        // Disabled direction swaps
        mMotor1Swap = false;
        mMotor2Swap = false;

        mDPad = prefs.getBoolean(DPADPREF, true);
    }

    public void saveSettings(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(CONNECTION_PREFFILE, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(ADDRESSPREF, mAddress);
        editor.putBoolean(STEERINGPREF, mSteering);
        editor.putBoolean(MOTOR1SWAPPREF, mMotor1Swap);
        editor.putBoolean(MOTOR2SWAPPREF, mMotor2Swap);

        editor.putBoolean(DPADPREF, mDPad);

        editor.commit();
    }

}
