package com.ogadai.alee.homerc;

/**
 * Created by alee on 24/05/2017.
 */

public interface MessageHandler {
    void handleMessage(String message);
    void handleMessage(byte[] message);
}
