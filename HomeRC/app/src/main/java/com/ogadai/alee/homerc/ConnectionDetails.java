package com.ogadai.alee.homerc;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by alee on 29/08/2016.
 */
public class ConnectionDetails {
    private String mAddress;

    public static final String CONNECTION_PREFFILE = "rc_connection";
    public static final String ADDRESSPREF = "address";

    public String getAddress() { return mAddress; }
    public void setAddress(String address) { mAddress = address; }

    public void readSettings(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(CONNECTION_PREFFILE, Context.MODE_PRIVATE);
        mAddress = prefs.getString(ADDRESSPREF, "ws://raspberrypirc:8080");
    }

    public void saveSettings(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(CONNECTION_PREFFILE, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(ADDRESSPREF, mAddress);
        editor.commit();
    }

}
