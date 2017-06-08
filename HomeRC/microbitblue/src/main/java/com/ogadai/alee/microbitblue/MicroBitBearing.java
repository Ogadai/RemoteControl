package com.ogadai.alee.microbitblue;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.util.UUID;

/**
 * Created by alee on 08/06/2017.
 */

public class MicroBitBearing implements IMicroBitBlueService, IMicroBitBlueChanged {
    private IHandler mHandler;
    private MicroBitBlueController mController;
    private BluetoothGattService mMagnetometerService;

    private static final UUID MAGNETOMETER_SERVICE_UUID = UUID.fromString("e95df2d8-251d-470a-a062-fa1922dfa9a8");
    private static final UUID BEARING_CHARACTERISTIC_UUID = UUID.fromString("e95d9715-251d-470a-a062-fa1922dfa9a8");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public MicroBitBearing(IHandler handler) {
        mHandler = handler;
    }

    @Override
    public void initialise(MicroBitBlueController controller) {
        mController = controller;
    }

    @Override
    public void connect(final Runnable finished) {
        mMagnetometerService = mController.getGatt().getService(MAGNETOMETER_SERVICE_UUID);
        handleTemperatureEvents(new Runnable() {
            @Override
            public void run() {
                Log.i("MicroBitBearing", "Initialised");
                finished.run();
            }
        });
    }

    private void handleTemperatureEvents(Runnable finished) {
        BluetoothGattCharacteristic microbitBearing = mMagnetometerService.getCharacteristic(BEARING_CHARACTERISTIC_UUID);

        BluetoothGattDescriptor descriptor = microbitBearing.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mController.writeDescriptor(microbitBearing, descriptor, finished);

        mController.onCharacteristicChanged(microbitBearing, this);
    }

    @Override
    public void changed(byte[] value) {
        int intValue = MicroBitByteDecode.getShort(value);
        Log.i("MicroBitEventBearing", "Bearing: " + intValue + "°");
        mHandler.onBearing(intValue);
    }

    public interface IHandler {
        void onBearing(int degrees);
    }
}
