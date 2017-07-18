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

    public static final String CONNECTION_PREFFILE = "rc_connection";
    public static final String ADDRESSPREF = "address";
    public static final String STEERINGPREF = "steering";
    public static final String MOTOR1SWAPPREF = "motor1swap";
    public static final String MOTOR2SWAPPREF = "motor2swap";

    public String getAddress() { return mAddress; }
    public void setAddress(String address) { mAddress = address; }

    public boolean getSteering() { return mSteering; }
    public void setSteering(boolean steering) { mSteering = steering; }

    public boolean getMotor1Swap() { return mMotor1Swap; }
    public void setMotor1Swap(boolean swap) { mMotor1Swap = swap; }

    public boolean getMotor2Swap() { return mMotor2Swap; }
    public void setMotor2Swap(boolean swap) { mMotor2Swap = swap; }

    public void readSettings(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(CONNECTION_PREFFILE, Context.MODE_PRIVATE);
        mAddress = prefs.getString(ADDRESSPREF, "ws://raspberrypirc:8080");
        mSteering = prefs.getBoolean(STEERINGPREF, true);
        mMotor1Swap = prefs.getBoolean(MOTOR1SWAPPREF, false);
        mMotor2Swap = prefs.getBoolean(MOTOR2SWAPPREF, false);
    }

    public void saveSettings(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(CONNECTION_PREFFILE, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(ADDRESSPREF, mAddress);
        editor.putBoolean(STEERINGPREF, mSteering);
        editor.putBoolean(MOTOR1SWAPPREF, mMotor1Swap);
        editor.putBoolean(MOTOR2SWAPPREF, mMotor2Swap);
        editor.commit();
    }

}
