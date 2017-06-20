package com.ogadai.alee.microbitblue;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by alee on 08/06/2017.
 */

public class MicroBitBlueController {

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;

    private Context mReceiverContext;
    private BroadcastReceiver mReceiver;
    private boolean mFoundDevice;

    private ArrayList<IMicroBitBlueService> mServices;

    private IConnectCallback mConnectCallback;

    private HashMap<UUID, Runnable> mWriteCharacteristicCallbacks;
    private HashMap<UUID, Runnable> mWriteDescriptorCallbacks;
    private HashMap<UUID, IMicroBitBlueChanged> mChangedCharacteristicCallbacks;
    private ArrayList<BluetoothGattCharacteristic> mNotificationCharacteristics;

    private static final String MICROBIT_DEVICE_NAME = "bbc micro:bit";

    public MicroBitBlueController() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mServices = new ArrayList<>();
    }

    public void connect(Context context) {
        connect(context, null);
    }
    public void connect(Context context, IConnectCallback callback) {
        mConnectCallback = callback;
        reset();

        if (mBluetoothGatt != null) {
            reconnectGatt();
        } else {
            BluetoothDevice targetDevice = queryPairedDevices(MICROBIT_DEVICE_NAME);
            if (targetDevice != null) {
                String message = "Trying to connect '" + MICROBIT_DEVICE_NAME + "'";
                Log.i("MicroBitBlue", message);
                mConnectCallback.status(message);
                connectGatt(context, targetDevice);
            } else {
                String message = "Looking for '" + MICROBIT_DEVICE_NAME + "'";
                Log.i("MicroBitBlue", message);
                mConnectCallback.status(message);
                discoverDevices(context, MICROBIT_DEVICE_NAME);
            }
        }
    }

    public void disconnect() {
        reset();
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
        }

        if (mReceiver != null) {
            mReceiverContext.unregisterReceiver(mReceiver);
            mReceiver = null;
            mReceiverContext = null;
        }
    }

    private void reset() {
        clearCharacteristicNotifications();
        mWriteCharacteristicCallbacks = new HashMap<>();
        mWriteDescriptorCallbacks = new HashMap<>();
        mChangedCharacteristicCallbacks = new HashMap<>();
        mNotificationCharacteristics = new ArrayList<>();
    }

    public void addService(IMicroBitBlueService controller) {
        mServices.add(controller);
        controller.initialise(this);
    }

    public BluetoothGatt getGatt() {
        return mBluetoothGatt;
    }

    public void writeCharacteristic(final BluetoothGattCharacteristic characteristic, final Runnable callback) {
        if (callback != null) {
            mWriteCharacteristicCallbacks.put(characteristic.getUuid(), new Runnable() {
                @Override
                public void run() {
                    mWriteCharacteristicCallbacks.remove(characteristic.getUuid());
                    callback.run();
                }
            });
        }

        boolean success = mBluetoothGatt.writeCharacteristic(characteristic);
        Log.d("MicroBitBlue", "writeCharacteristic - " + success);
    }

    public void writeDescriptor(final BluetoothGattCharacteristic characteristic, final BluetoothGattDescriptor descriptor, final Runnable callback) {
        if (callback != null) {
            mWriteDescriptorCallbacks.put(characteristic.getUuid(), new Runnable() {
                @Override
                public void run() {
                    mWriteDescriptorCallbacks.remove(characteristic.getUuid());
                    callback.run();
                }
            });
        }

        boolean success = mBluetoothGatt.writeDescriptor(descriptor);
        Log.d("MicroBitBlue", "writeDescriptor - " + success);
    }

    public void onCharacteristicChanged(final BluetoothGattCharacteristic characteristic, final IMicroBitBlueChanged callback) {
        mBluetoothGatt.setCharacteristicNotification(characteristic, true);
        mChangedCharacteristicCallbacks.put(characteristic.getUuid(), callback);
        mNotificationCharacteristics.add(characteristic);
    }

    private void clearCharacteristicNotifications() {
        if (mNotificationCharacteristics != null) {
            for (BluetoothGattCharacteristic characteristic : mNotificationCharacteristics) {
                mBluetoothGatt.setCharacteristicNotification(characteristic, false);
            }
        }
    }

    private void discoverDevices(Context context, final String targetName) {
        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        mFoundDevice = false;
        mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    mBluetoothAdapter.cancelDiscovery();

                    if (!mFoundDevice) {
                        mConnectCallback.failed("Couldn't find " + targetName);
                    }
                } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String deviceName = device.getName();
                    if (deviceName != null && deviceName.length() >= targetName.length()
                            && deviceName.toLowerCase().substring(0, targetName.length()).compareTo(targetName) == 0) {
                        mFoundDevice = true;
                        String message = "Trying to connect '" + MICROBIT_DEVICE_NAME + "'";
                        Log.i("MicroBitBlue", message);
                        mConnectCallback.status(message);
                        connectGatt(context, device);
                    }

                }
            }
        };

        mReceiverContext = context;
        mReceiverContext.registerReceiver(mReceiver, filter);

        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        mBluetoothAdapter.startDiscovery();
    }

    private BluetoothDevice queryPairedDevices(String targetName) {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        // If there are paired devices
        BluetoothDevice targetDevice = null;
        if (pairedDevices.size() > 0) {
            String deviceList = "";
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                if (deviceList.length() > 0) {
                    deviceList += ",";
                }
                deviceList += device.getName();
                Log.i("MicroBitBlue", "Paired device: " + device.getName());

                String deviceName = device.getName().toLowerCase();
                if (deviceName.length() >= targetName.length() && deviceName.substring(0, targetName.length()).compareTo(targetName) == 0) {
                    targetDevice = device;
                }
            }
        }

        return targetDevice;
    }

    private void connectGatt(Context context, BluetoothDevice device) {
        mBluetoothGatt = device.connectGatt(context, true, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d("MicroBitBlue", "Gatt Connected");
                    mBluetoothGatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d("MicroBitBlue", "Gatt Disconnected");
                    mConnectCallback.disconnected();
                } else {
                    Log.d("MicroBitBlue", "Gatt Connection State - " + newState);
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                Log.d("MicroBitBlue", "Gatt Services Discoverred - " + status);

                initialiseServices();
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
            }

            @Override
            public void onCharacteristicChanged (BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);

                IMicroBitBlueChanged callback = mChangedCharacteristicCallbacks.get(characteristic.getUuid());
                if (callback != null) {
                    callback.changed(characteristic.getValue());
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);

                Runnable callback = mWriteCharacteristicCallbacks.get(characteristic.getUuid());
                if (callback != null) {
                    callback.run();
                }
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorWrite(gatt, descriptor, status);

                Runnable callback = mWriteDescriptorCallbacks.get(descriptor.getCharacteristic().getUuid());
                if (callback != null) {
                    callback.run();
                }
            }
        });
    }

    private void reconnectGatt() {
        boolean result = mBluetoothGatt.connect();
        Log.d("MicroBitBlue", "Gatt Reconnect - " + result);

        mBluetoothGatt.discoverServices();
    }

    private void initialiseServices() {
        final ArrayList<IMicroBitBlueService> initialiseList = new ArrayList<>();
        initialiseList.addAll(mServices);

        connectNext(initialiseList);
    }

    private void connectNext(final ArrayList<IMicroBitBlueService> initialiseList) {
        if (initialiseList.size() > 0) {
            IMicroBitBlueService nextService = initialiseList.get(0);
            initialiseList.remove(nextService);

            nextService.connect(new Runnable() {
                @Override
                public void run() {
                    connectNext(initialiseList);
                }
            });
        } else if (mConnectCallback != null) {
            mConnectCallback.connected();
        }
    }

    public interface IConnectCallback {
        void connected();
        void disconnected();
        void failed(String message);
        void status(String message);
    }
}
