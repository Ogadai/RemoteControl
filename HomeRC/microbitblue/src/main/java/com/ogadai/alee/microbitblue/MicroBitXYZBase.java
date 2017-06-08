package com.ogadai.alee.microbitblue;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.util.UUID;

/**
 * Created by alee on 08/06/2017.
 */

public abstract class MicroBitXYZBase implements IMicroBitBlueService, IMicroBitBlueChanged {
    private IHandler mHandler;
    private MicroBitBlueController mController;
    private BluetoothGattService mXYZService;

    private UUID mServiceUUID;
    private UUID mCharacteristicUUID;

    private static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public MicroBitXYZBase(UUID serviceUUID, UUID characteristicUUID, IHandler handler) {
        mServiceUUID = serviceUUID;
        mCharacteristicUUID = characteristicUUID;
        mHandler = handler;
    }

    @Override
    public void initialise(MicroBitBlueController controller) {
        mController = controller;
    }

    @Override
    public void connect(final Runnable finished) {
        mXYZService = mController.getGatt().getService(mServiceUUID);
        handleXYZEvents(new Runnable() {
            @Override
            public void run() {
                Log.i("MicroBitTemperature", "Initialised");
                finished.run();
            }
        });
    }

    private void handleXYZEvents(Runnable finished) {
        BluetoothGattCharacteristic microbitTemperature = mXYZService.getCharacteristic(mCharacteristicUUID);

        BluetoothGattDescriptor descriptor = microbitTemperature.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mController.writeDescriptor(microbitTemperature, descriptor, finished);

        mController.onCharacteristicChanged(microbitTemperature, this);
    }

    @Override
    public void changed(byte[] value) {
        float x = bytesToFloat(value, 0);
        float y = bytesToFloat(value, 2);
        float z = bytesToFloat(value, 4);

        float strength = (float)Math.sqrt(x * x + y * y);
        float angle = getAngle(x, y);

        Log.i("MicroBitEventXYZ", String.format("(%.2f,%.2f,%.2f) %.1f° @ %.0f", x, y, z, angle, strength));

        mHandler.onXYZ(x, y, z, angle, strength);
    }

    private float getAngle(float x, float y) {
        if (y > 0) {
            return (float)(90 - Math.atan2(x, y) * 180 / Math.PI);
        } else if (y < 0) {
            return (float)(270 - Math.atan2(x, y) * 180 / Math.PI);
        } else {
            return x < 0 ? 180 : 0;
        }
    }

    public static float bytesToFloat(byte[] bytes, int offset) {
        return (float)MicroBitByteDecode.getShort(bytes, offset) / 1000;
    }

    public interface IHandler {
        void onXYZ(float x, float y, float z, float angle, float strength);
    }
}
