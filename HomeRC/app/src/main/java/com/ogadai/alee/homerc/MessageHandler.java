package com.ogadai.alee.homerc;

/**
 * Created by alee on 24/05/2017.
 */

public interface MessageHandler {
    void connected();
    void disconnected(String message);
    void handleMessage(DeviceMessage message);
    void handleMessage(byte[] message);
}
