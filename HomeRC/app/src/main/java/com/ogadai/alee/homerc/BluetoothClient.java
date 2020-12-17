package com.ogadai.alee.homerc;

import android.content.Context;
import android.util.Log;

import org.glassfish.tyrus.client.auth.AuthenticationException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.zip.DeflaterInputStream;

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
    private MicroBitEventSender mActions;
//    private MicroBitEventSender mSteerDuration;
    private MicroBitTemperature mTemperature;

    private ArrayList<DeviceMessage> mMessageQueue;

//    private boolean mContinuousSteeringMode = false;
//    private int mLastLeftRightState = 0;

    private static final String TAG = "BluetoothClient";

    public BluetoothClient() {
        mController = new MicroBitBlueController();

        mLeftRight = new MicroBitEventSender(1026);
        mForwardsBackwards = new MicroBitEventSender(1027);
        mSteering = new MicroBitEventSender(1028);
        mActions = new MicroBitEventSender(1029);
//        mSteerDuration = new MicroBitEventSender(1030);
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
        mController.addService(mActions);
//        mController.addService(mSteerDuration);
        mController.addService(mTemperature);

        mMessageQueue = new ArrayList<>();
    }

    @Override
    public void connect(Context context, String address) throws AuthenticationException, IOException, DeploymentException, URISyntaxException {
        System.out.println("Connecting : " + address);
        final int steerDuration = getSteerDuration(address);
        mController.connect(context, new MicroBitBlueController.IConnectCallback() {
            @Override
            public void connected() {
                System.out.println("Connected bluetooth device");
                if (mMessageHandler != null) {
                    mMessageHandler.connected();
                }

//                mSteerDuration.send(steerDuration);
//                mContinuousSteeringMode = (steerDuration != 0);
            }

            @Override
            public void disconnected() {
                System.out.println("Disonnected bluetooth device");
                if (mMessageHandler != null) {
                    mMessageHandler.disconnected("");
                }
            }

            @Override
            public void status(String message) {
                if (mMessageHandler != null) {
                    mMessageHandler.status(message);
                }
            }

            @Override
            public void failed(String message) {
                System.out.println("Failed to connect bluetooth device - " + message);
                if (mMessageHandler != null) {
                    mMessageHandler.disconnected(message);
                }
            }
        });
    }

    private int getSteerDuration(String address) {
        try {
            return Integer.parseInt(address.substring(4));
        } catch (Exception e) {
            return 0;
        }
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
        boolean useMessage = true;
//        if (!mContinuousSteeringMode) {
//            useMessage = false;
//            String name = message.getName();
//            String state = message.getState();
//
//            int nextLeftRightState = 0;
//            if (name.equalsIgnoreCase("right")) {
//                nextLeftRightState = state.equalsIgnoreCase("on") ? 2 : 0;
//            } else if (name.equalsIgnoreCase("left")) {
//                nextLeftRightState = state.equalsIgnoreCase("on") ? 1 : 0;
//            } else if (name.equalsIgnoreCase("steering")) {
//                int steering = Integer.parseInt(state);
//                if (steering < -30) {
//                    nextLeftRightState = 1;
//                } else if (steering > 30) {
//                    nextLeftRightState = 2;
//                } else {
//                    nextLeftRightState = 0;
//                }
//            } else {
//                useMessage = true;
//            }
//
//            if (nextLeftRightState != mLastLeftRightState) {
//                queueMessage(new DeviceMessage("leftright", Integer.toString(nextLeftRightState)));
//                mLastLeftRightState = nextLeftRightState;
//            }
//        }

        if (useMessage) {
            queueMessage(message);
        }
    }

    private void queueMessage(DeviceMessage message) {
        if (addMessageToQueue(message)) {
            processMessageQueue();
        }
    }

    private synchronized boolean addMessageToQueue(DeviceMessage message) {
        for(DeviceMessage existing : mMessageQueue) {
            if (message.getName().compareToIgnoreCase(existing.getName()) == 0) {
                existing.setState(message.getState());
                return true;
            }
        }

        mMessageQueue.add(message);
        if (!mProcessingQueue) {
            mProcessingQueue = true;
            return true;
        }
        return false;
    }

    private synchronized DeviceMessage getNextMessageFromQueue() {
        if (mMessageQueue.size() == 0) return null;
        DeviceMessage message = mMessageQueue.get(0);
        mMessageQueue.remove(0);
        return message;
    }

    private boolean mProcessingQueue = false;
    private void processMessageQueue() {
        final DeviceMessage message = getNextMessageFromQueue();
        if (message == null) {
            mProcessingQueue = false;
            return;
        }

        String name = message.getName();
        MicroBitEventSender sender = null;
        int value = 0;

        // "forwards/backwards/left/right":"on"/"off"
        // "steering": -100 => 100
        if (name.equalsIgnoreCase("forwards")) {
            sender = mForwardsBackwards;
            value = message.getState().equalsIgnoreCase("on") ? 2 : 0;
        } else if (name.equalsIgnoreCase("backwards")) {
            sender = mForwardsBackwards;
            value = message.getState().equalsIgnoreCase("on") ? 1 : 0;
        } else if (name.equalsIgnoreCase("right")) {
            sender = mLeftRight;
            value = message.getState().equalsIgnoreCase("on") ? 2 : 0;
        } else if (name.equalsIgnoreCase("left")) {
            sender = mLeftRight;
            value = message.getState().equalsIgnoreCase("on") ? 1 : 0;
        } else if (name.equalsIgnoreCase("steering")) {
            sender = mSteering;
            value = Integer.parseInt(message.getState()) + 100;
        } else if (name.equalsIgnoreCase("leftright")) {
            sender = mLeftRight;
            value = Integer.parseInt(message.getState());
        } else if (name.equalsIgnoreCase("horn")) {
            sender = mActions;
            value = message.getState().equalsIgnoreCase("on") ? 1 : 0;
        }

        if (sender != null) {
            sender.send(value, new Runnable() {
                @Override
                public void run() {
                    processMessageQueue();
                }
            });
            Log.i(TAG, message.getName() + ": " + message.getState());
        }
    }
}
