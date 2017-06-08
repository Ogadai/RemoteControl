package com.ogadai.alee.microbitblue;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.util.UUID;

/**
 * Created by alee on 08/06/2017.
 */

public class MicroBitEventSender implements IMicroBitBlueService {
    private int mSource;
    private MicroBitBlueController mController;
    private BluetoothGattService mEventService;
    private BluetoothGattCharacteristic mMicrobitSend;

    private static final UUID EVENTSERVICE_SERVICE_UUID = UUID.fromString("e95d93af-251d-470a-a062-fa1922dfa9a8");
    private static final UUID MICROBITSEND_CHARACTERISTIC_UUID = UUID.fromString("e95d5404-251d-470a-a062-fa1922dfa9a8");

    public MicroBitEventSender(int source) {
        mSource = source;
    }

    @Override
    public void initialise(MicroBitBlueController controller) {
        mController = controller;
    }

    @Override
    public void connect(Runnable finished) {
        mEventService = mController.getGatt().getService(EVENTSERVICE_SERVICE_UUID);
        mMicrobitSend = mEventService.getCharacteristic(MICROBITSEND_CHARACTERISTIC_UUID);
        mMicrobitSend.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

        Log.i("MicroBitEventSender", "Initialised");
        finished.run();
    }

    public void send(int value) {
        send(value, null);
    }
    public void send(int value, Runnable finished) {
        mMicrobitSend.setValue(mSource + value * 0x10000, BluetoothGattCharacteristic.FORMAT_UINT32, 0);
        mController.writeCharacteristic(mMicrobitSend, finished);
    }
}
