package com.ogadai.alee.homerc;

import com.google.gson.annotations.SerializedName;

/**
 * Created by alee on 29/08/2016.
 */
public class DeviceMessage {
    @SerializedName("name")
    private String mName;

    @SerializedName("state")
    private String mState;

    public DeviceMessage(String name, String state) {
        mName = name;
        mState = state;
    }

    public String getName() { return mName; }
    public final void setName(String name) { mName = name; }

    public String getState() { return mState; }
    public final void setState(String state) { mState = state; }
}
