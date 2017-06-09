package com.ogadai.alee.microbitblue;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.util.UUID;

/**
 * Created by alee on 08/06/2017.
 */

public class MicroBitTemperature implements IMicroBitBlueService, IMicroBitBlueChanged {
    private IHandler mHandler;
    private MicroBitBlueController mController;
    private BluetoothGattService mTemperatureService;

    private static final UUID TEMPERATURE_SERVICE_UUID = UUID.fromString("e95d6100-251d-470a-a062-fa1922dfa9a8");
    private static final UUID TEMPERATURE_CHARACTERISTIC_UUID = UUID.fromString("e95d9250-251d-470a-a062-fa1922dfa9a8");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public MicroBitTemperature(IHandler handler) {
        mHandler = handler;
    }

    @Override
    public void initialise(MicroBitBlueController controller) {
        mController = controller;
    }

    @Override
    public void connect(final Runnable finished) {
        mTemperatureService = mController.getGatt().getService(TEMPERATURE_SERVICE_UUID);
        handleTemperatureEvents(new Runnable() {
            @Override
            public void run() {
                Log.i("MicroBitTemperature", "Initialised");
                finished.run();
            }
        });
    }

    private void handleTemperatureEvents(Runnable finished) {
        BluetoothGattCharacteristic microbitTemperature = mTemperatureService.getCharacteristic(TEMPERATURE_CHARACTERISTIC_UUID);

        BluetoothGattDescriptor descriptor = microbitTemperature.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mController.writeDescriptor(microbitTemperature, descriptor, finished);

        mController.onCharacteristicChanged(microbitTemperature, this);
    }

    @Override
    public void changed(byte[] value) {
        int intValue = MicroBitByteDecode.getShort(value);
        mHandler.onTemperature(intValue);
    }

    public interface IHandler {
        void onTemperature(int celcius);
    }
}
