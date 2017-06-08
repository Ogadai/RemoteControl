package com.ogadai.alee.microbitblue;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.util.UUID;

/**
 * Created by alee on 08/06/2017.
 */

public class MicroBitEventListener implements IMicroBitBlueService, IMicroBitBlueChanged {
    private int mSource;
    private IHandler mHandler;
    private MicroBitBlueController mController;
    private BluetoothGattService mEventService;

    private static final UUID EVENTSERVICE_SERVICE_UUID = UUID.fromString("e95d93af-251d-470a-a062-fa1922dfa9a8");
    private static final UUID MICROBITEVENT_CHARACTERISTIC_UUID = UUID.fromString("e95d9775-251d-470a-a062-fa1922dfa9a8");
    private static final UUID CLIENTREQUIREMENTS_CHARACTERISTIC_UUID = UUID.fromString("e95d23c4-251d-470a-a062-fa1922dfa9a8");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public MicroBitEventListener(int source, IHandler handler) {
        mSource = source;
        mHandler = handler;
    }

    @Override
    public void initialise(MicroBitBlueController controller) {
        mController = controller;
    }

    @Override
    public void connect(final Runnable finished) {
        mEventService = mController.getGatt().getService(EVENTSERVICE_SERVICE_UUID);
        enableEventsFromMicroBit(new Runnable() {
            @Override
            public void run() {
                handleMicroBitEvents(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("MicroBitEventListener", "Initialised");
                        finished.run();
                    }
                });
            }
        });
    }

    private void enableEventsFromMicroBit(Runnable finished) {
        BluetoothGattCharacteristic clientRequirements = mEventService.getCharacteristic(CLIENTREQUIREMENTS_CHARACTERISTIC_UUID);
        clientRequirements.setValue(mSource, BluetoothGattCharacteristic.FORMAT_UINT32, 0);
        clientRequirements.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        mController.writeCharacteristic(clientRequirements, finished);
    }

    private void handleMicroBitEvents(Runnable finished) {
        BluetoothGattCharacteristic microbitEvent = mEventService.getCharacteristic(MICROBITEVENT_CHARACTERISTIC_UUID);

        BluetoothGattDescriptor descriptor = microbitEvent.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mController.writeDescriptor(microbitEvent, descriptor, finished);

        mController.onCharacteristicChanged(microbitEvent, this);
    }

    @Override
    public void changed(byte[] value) {
        int intValue = MicroBitByteDecode.getShort(value, 2);
        Log.i("MicroBitEventListener", "Event (" + mSource + ") value=" + intValue);
        mHandler.onEvent(intValue);
    }

    public interface IHandler {
        void onEvent(int value);
    }
}
