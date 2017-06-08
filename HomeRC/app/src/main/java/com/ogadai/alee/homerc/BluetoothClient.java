package com.ogadai.alee.homerc;

import android.content.Context;

import org.glassfish.tyrus.client.auth.AuthenticationException;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.websocket.DeploymentException;

import com.ogadai.alee.microbitblue.MicroBitBlueController;
import com.ogadai.alee.microbitblue.MicroBitEventSender;
import com.ogadai.alee.microbitblue.MicroBitTemperature;

/**
 * Created by alee on 24/05/2017.
 */

public class BluetoothClient implements RemoteDevice {
    private MicroBitBlueController mController;
    MessageHandler mMessageHandler;

    private MicroBitEventSender mLeftRight;
    private MicroBitEventSender mForwardsBackwards;
    private MicroBitEventSender mSteering;
    private MicroBitTemperature mTemperature;

    public BluetoothClient() {
        mController = new MicroBitBlueController();

        mLeftRight = new MicroBitEventSender(1026);
        mForwardsBackwards = new MicroBitEventSender(1027);
        mSteering = new MicroBitEventSender(1028);
        mTemperature = new MicroBitTemperature(new MicroBitTemperature.IHandler() {
            @Override
            public void onTemperature(int celcius) {
                if (mMessageHandler != null) {
                    mMessageHandler.handleMessage(new DeviceMessage("temperature", Integer.toString(celcius)));
                }
            }
        });

        mController.addService(mLeftRight);
        mController.addService(mForwardsBackwards);
        mController.addService(mSteering);
        mController.addService(mTemperature);
    }

    @Override
    public void connect(Context context, String address) throws AuthenticationException, IOException, DeploymentException, URISyntaxException {
        System.out.println("Connecting : " + address);
        mController.connect(context, new MicroBitBlueController.IConnectCallback() {
            @Override
            public void connected() {
                System.out.println("Connected bluetooth device");
                if (mMessageHandler != null) {
                    mMessageHandler.connected();
                }
            }

            @Override
            public void disconnected() {
                System.out.println("Disonnected bluetooth device");
                if (mMessageHandler != null) {
                    mMessageHandler.disconnected("");
                }
            }

            @Override
            public void failed() {
                System.out.println("Failed to connect bluetooth device");
                if (mMessageHandler != null) {
                    mMessageHandler.disconnected("Failed to connect bluetooth");
                }
            }
        });
    }

    @Override
    public void disconnect()
    {
        System.out.println("Disonnecting bluetooth device");
        mController.disconnect();
    }

    @Override
    public void addMessageHandler(MessageHandler msgHandler) {
        mMessageHandler = msgHandler;
    }

    @Override
    public void sendMessage(DeviceMessage message)
    {
        // "forwards/backwards/left/right":"on"/"off"
        // "steering": -100 => 100
        if (mMessageHandler != null) {
            String name = message.getName();
            if (name.equalsIgnoreCase("forwards")) {
                mForwardsBackwards.send(message.getState().equalsIgnoreCase("on") ? 2 : 0);
            } else if (name.equalsIgnoreCase("backwards")) {
                mForwardsBackwards.send(message.getState().equalsIgnoreCase("on") ? 1 : 0);
            } else if (name.equalsIgnoreCase("right")) {
                mLeftRight.send(message.getState().equalsIgnoreCase("on") ? 2 : 0);
            } else if (name.equalsIgnoreCase("left")) {
                mLeftRight.send(message.getState().equalsIgnoreCase("on") ? 1 : 0);
            } else if (name.equalsIgnoreCase("steering")) {
                int steering = Integer.parseInt(message.getState());
                mSteering.send(steering + 100);
            }
        }
        System.out.println(message.getName() + ": " + message.getState());
    }
}
