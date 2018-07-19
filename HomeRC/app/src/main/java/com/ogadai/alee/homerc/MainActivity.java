package com.ogadai.alee.homerc;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.view.Gravity;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private TextureView mContentView;

    private TextView mConnectionStatus;
    private ImageButton mConnectionLight;
    private View mSettingsView;
    private RadioButton mProfileA;
    private RadioButton mProfileB;
    private EditText mConnectionAddress;
    private CheckBox mCheckSteering;
    private CheckBox mCheckMotor1Swap;
    private CheckBox mCheckMotor2Swap;

    private VideoPlayer mVideoPlayer;

    private TextView mTemperature;

    ImageButton mForwards;
    ImageButton mBackwards;
    ImageButton mLeft;
    ImageButton mRight;
    ImageButton mHorn;

    private enum ConnectionColours {
        RED,
        AMBER,
        GREEN
    }

    private RemoteDevice mRemoteDevice;

    private ConnectionDetails mConnectionDetails;
    private boolean mShouldBeConnected;
    private boolean mConnected;

    private Handler mDelayHandler;
    private Runnable mReconnectRunnable;

    private Runnable mSteerRunnable;
    private DeviceMessage mSteerMessage;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagneticField;

    private int mVideoBlocksReceived;

    private boolean mTriedTurningOnBT = false;
    private static final int REQUEST_ENABLE_BT = 5827;
    private static final int REQUEST_COURSE_LOCATION = 5830;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mDelayHandler = new Handler();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mConnectionDetails = new ConnectionDetails();
        mConnectionDetails.readSettings(this);

        mContentView = (TextureView)findViewById(R.id.fullscreen_content);
        mConnectionStatus = (TextView)findViewById(R.id.connection_status);
        mConnectionLight = (ImageButton) findViewById(R.id.connection_light);

        mSettingsView = findViewById(R.id.connection_settings);
        mProfileA = (RadioButton) findViewById(R.id.connection_profile_a);
        mProfileB = (RadioButton) findViewById(R.id.connection_profile_b);
        mConnectionAddress = (EditText)findViewById(R.id.connection_address);
        mCheckSteering = (CheckBox)findViewById(R.id.check_steering);
        mCheckMotor1Swap = (CheckBox)findViewById(R.id.motor1_swap);
        mCheckMotor2Swap = (CheckBox)findViewById(R.id.motor2_swap);

        mTemperature = (TextView)findViewById(R.id.temperature);

        mSettingsView.setVisibility(View.INVISIBLE);
        mConnected = false;
        mShouldBeConnected = false;

        final Context context = this;

        ((ImageButton)findViewById(R.id.connection_setup)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mConnectionDetails.getProfile().equalsIgnoreCase("B")) {
                    mProfileB.setChecked(true);
                } else {
                    mProfileA.setChecked(true);
                }
                displayConnectionSettings();
                mSettingsView.setVisibility(View.VISIBLE);

                stopVideo();
                Thread worker = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        disconnect();
                    }
                });
                worker.start();
            }
        });
        ((Button)findViewById(R.id.connection_connect)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mConnectionDetails.setAddress(mConnectionAddress.getText().toString());
                mConnectionDetails.setSteering(mCheckSteering.isChecked());
                mConnectionDetails.setMotor1Swap(mCheckMotor1Swap.isChecked());
                mConnectionDetails.setMotor2Swap(mCheckMotor2Swap.isChecked());
                mConnectionDetails.saveSettings(context);

                mSettingsView.setVisibility(View.INVISIBLE);
                setVisibility();

                mTriedTurningOnBT = false;
                initialiseConnection();
            }
        });
        ((RadioButton)findViewById(R.id.connection_profile_a)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeConnectionProfile();
            }
        });
        ((RadioButton)findViewById(R.id.connection_profile_b)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeConnectionProfile();
            }
        });

        setConnectionStatus("Initialising...");
        setVisibility();

        mForwards = (ImageButton)findViewById(R.id.forward_button);
        mBackwards = (ImageButton)findViewById(R.id.backward_button);
        mLeft = (ImageButton)findViewById(R.id.left_button);
        mRight = (ImageButton)findViewById(R.id.right_button);

        mHorn = (ImageButton)findViewById(R.id.horn_button);
        mHorn.setOnTouchListener(new ButtonTouchListener("horn"));

        initialiseConnection();
    }

    private void displayConnectionSettings() {
        mConnectionAddress.setText(mConnectionDetails.getAddress());
        mCheckSteering.setChecked(mConnectionDetails.getSteering());
        mCheckMotor1Swap.setChecked(mConnectionDetails.getMotor1Swap());
        mCheckMotor2Swap.setChecked(mConnectionDetails.getMotor2Swap());
    }

    private void changeConnectionProfile() {
        String profile = mProfileB.isChecked() ? "B" : "";
        mConnectionDetails.readSettings(this, profile);
        displayConnectionSettings();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopVideo();
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                disconnect();
            }
        });
        worker.start();

    }

    private void setVisibility() {
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setVisibility();

        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mMagneticField, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mSensorManager.unregisterListener(this);
    }

    private void setTemperature(final String temperature) {
        final String tempStr = (temperature != null && temperature.length() > 0) ? temperature + "°" : "";
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTemperature.setText(tempStr);
            }
        });
    }
    private void setConnectionStatus(final String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionStatus.setText(status);
            }
        });
    }
    private void setConnectionLight(final ConnectionColours colour) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int drawableId = R.drawable.green;
                switch(colour) {
                    case RED:
                        drawableId = R.drawable.red;
                        break;
                    case AMBER:
                        drawableId = R.drawable.amber;
                        break;
                    case GREEN:
                        drawableId = R.drawable.green;
                        break;
                }
                mConnectionLight.setImageResource(drawableId);
            }
        });
    }

    private void setupUIControls(final boolean steering) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLeft.setVisibility(steering ? View.GONE : View.VISIBLE);
                mRight.setVisibility(steering ? View.GONE : View.VISIBLE);

                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)mForwards.getLayoutParams();
                params.gravity = steering ? Gravity.BOTTOM | Gravity.RIGHT : Gravity.TOP | Gravity.LEFT;
                mForwards.setLayoutParams(params);

                mForwards.setOnTouchListener(new ButtonTouchListener(mConnectionDetails.getMotor1Swap() ? "backwards" : "forwards"));
                mBackwards.setOnTouchListener(new ButtonTouchListener(mConnectionDetails.getMotor1Swap() ? "forwards" : "backwards"));
                mLeft.setOnTouchListener(new ButtonTouchListener(mConnectionDetails.getMotor2Swap() ? "right" : "left"));
                mRight.setOnTouchListener(new ButtonTouchListener(mConnectionDetails.getMotor2Swap() ? "left" : "right"));
            }
        });
    }

    private RemoteDevice createRemoteDevice() {
        if (isBluetoothDevice()) {
            return new BluetoothClient();
        } else {
            return new SocketClient();
        }
    }

    private boolean isBluetoothDevice() {
        String address = mConnectionDetails.getAddress();
        return address.startsWith("BLE:");
    }


    private void connect() {
        try {
            disconnect();

            mShouldBeConnected = true;
            setupUIControls(mConnectionDetails.getSteering());
            setConnectionStatus("Connecting...");
            setTemperature("");
            setConnectionLight(ConnectionColours.AMBER);
            System.out.println("connecting to: " + mConnectionDetails.getAddress());

            mVideoBlocksReceived = 0;

            RemoteDevice device = createRemoteDevice();
            device.addMessageHandler(new MessageHandler() {
                @Override
                public void connected() {
                    mConnected = true;
                    setConnectionStatus("");
                    setConnectionLight(ConnectionColours.GREEN);
                }

                @Override
                public void status(String message) {
                    setConnectionStatus(message);
                }

                @Override
                public void disconnected(String message) {
                    mConnected = false;
                    setConnectionStatus(message);
                    setConnectionLight(ConnectionColours.RED);

                    if (mShouldBeConnected) {
                        reconnectOnDelay();
                    }
                }

                @Override
                public void handleMessage(final DeviceMessage message) {
                    if (message.getName().equalsIgnoreCase("temperature")) {
                        setTemperature(message.getState());
                    }
                    System.out.println("received: " + message.getName() + "=" + message.getState());
                }

                @Override
                public void handleMessage(byte[] message) {
                    mVideoBlocksReceived++;
                    if (mVideoBlocksReceived % 30 == 0) {
                        sendMessageSync(new DeviceMessage("camera", "block-" + mVideoBlocksReceived));
                    }
                    mVideoPlayer.addData(message);
                }
            });

            device.connect(this, mConnectionDetails.getAddress());
            mRemoteDevice = device;

            // Start the camera
            String cameraMessage = "on-(" + mContentView.getWidth() + "," + mContentView.getHeight() + ")";
            sendMessage(new DeviceMessage("camera", cameraMessage));
        } catch (Exception e) {
            System.out.println("error connecting socket: " + e.getMessage());
            setConnectionStatus("Error: " + e.getMessage());
            setConnectionLight(ConnectionColours.RED);
            mRemoteDevice = null;
        }
    }

    private void disconnect() {
        cancelReconnectOnDelay();
        cancelDelaySteer();

        mShouldBeConnected = false;

        if (mRemoteDevice != null) {
            setConnectionStatus("Disconnecting");
            setConnectionLight(ConnectionColours.AMBER);
            mRemoteDevice.disconnect();
        }
        mRemoteDevice = null;
        mConnected = false;

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }
    }

    private void reconnectOnDelay() {
        mReconnectRunnable = new Runnable() {
            @Override
            public void run() {
                //Do something after 100ms
                connect();
            }
        };

        mDelayHandler.postDelayed(mReconnectRunnable, 5000);
    }

    private void cancelReconnectOnDelay() {
        if (mReconnectRunnable!= null) {
            mDelayHandler.removeCallbacks(mReconnectRunnable);
            mReconnectRunnable = null;
        }
    }

    private void connectAsyncAndStreamVideo() {
        initialiseVideo();
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                connect();
            }
        });
        worker.start();
    }

    private void initialiseConnection() {
        if (isBluetoothDevice()) {
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                // Device does not support Bluetooth
                setConnectionStatus("Bluetooth is not supported");
                return;
            }

            if (!mBluetoothAdapter.isEnabled()) {
                if (!mTriedTurningOnBT) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                } else {
                    setConnectionStatus("Bluetooth is not enabled");
                }
            } else {
                checkBluetoothPermissions();
            }
        } else {
            setupVideoTexture();
        }
    }

    private void checkBluetoothPermissions() {
// requestPermissions of ACCESS_COARSE_LOCATION required for SDK 23 or over
//        requestPermissions(new String[] { Manifest.permission.ACCESS_COARSE_LOCATION }, REQUEST_COURSE_LOCATION);
        connectBluetoothDevice();
    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
//        switch (requestCode) {
//            case REQUEST_COURSE_LOCATION:
//                if (grantResults.length > 0) {
//                    for (int gr : grantResults) {
//                        // Check if request is granted or not
//                        if (gr != PackageManager.PERMISSION_GRANTED) {
//                            return;
//                        }
//                    }
//
//                    connectBluetoothDevice();
//                }
//                break;
//            default:
//                return;
//        }
//    }

    private void connectBluetoothDevice() {
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                connect();
            }
        });
        worker.start();
    }

    private void setupVideoTexture() {
        if (mContentView.isAvailable()) {
            connectAsyncAndStreamVideo();
        } else {
            mContentView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    connectAsyncAndStreamVideo();
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {

                }
            });
        }
    }

    private void initialiseVideo() {
        stopVideo();
        mVideoPlayer = new VideoPlayer(new Surface(mContentView.getSurfaceTexture()), mContentView.getWidth(), mContentView.getHeight());
    }

    private void stopVideo() {
        if (mVideoPlayer != null) {
            mVideoPlayer.stop();
            mVideoPlayer = null;
        }
    }

    private void sendMessage(final DeviceMessage message) {
        if (mConnected) {
            Thread worker = new Thread(new Runnable() {
                @Override
                public void run() {
                    sendMessageSync(message);
                }
            });
            worker.start();
        } else {
            Log.i(TAG, "send " + message.getName() + ":" + message.getState());
        }
    }

    private void sendMessageSync(DeviceMessage message) {
        if (mRemoteDevice != null) {
            try {
                mRemoteDevice.sendMessage(message);
            } catch (Exception e) {
                System.out.println("error sending socket message: " + e.getMessage());
                disconnect();
            }
        }
    }

    @Override
    public void onActivityResult (int requestCode,
                                  int resultCode,
                                  Intent data) {
        switch(requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    checkBluetoothPermissions();
                } else {
                    mTriedTurningOnBT = true;
                    setConnectionStatus("Bluetooth is not enabled");
                }
                break;
        }
    }

    float[] mGravity;
    float[] mGeomagnetic;
    float mLastRoll = 0;
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;

        if (mConnectionDetails.getSteering() && mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);

            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);

                float roll = orientation[1] * 10;

                // null zone
                if (roll <= 2 && roll >= -2) roll = 0;

                if (Math.abs(roll - mLastRoll) > 2) {
                    mLastRoll = roll;

                    String msgValue = Integer.toString(((int)mLastRoll * (mConnectionDetails.getMotor2Swap() ? 10 : -10)));

                    mSteerMessage = new DeviceMessage("steering", msgValue);

                    if (mSteerRunnable == null) {
                        mSteerRunnable = new Runnable() {
                            @Override
                            public void run() {
                                mSteerRunnable = null;
                                sendMessage(mSteerMessage);
                            }
                        };
                        mDelayHandler.postDelayed(mSteerRunnable, 100);
                    }
                }

            }
        }
    }

    private void cancelDelaySteer() {
        if (mSteerRunnable != null) {
            mDelayHandler.removeCallbacks(mSteerRunnable);
            mSteerRunnable = null;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private class ButtonTouchListener implements View.OnTouchListener {
        private String mDevice;

        public ButtonTouchListener(String device) {
            mDevice = device;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch(event.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    sendMessage(new DeviceMessage(mDevice, "on"));
                    break;

                case MotionEvent.ACTION_UP:
                    sendMessage(new DeviceMessage(mDevice, "off"));
                    break;
            }
            return false;
        }
    }
}
