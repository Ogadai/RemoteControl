package com.ogadai.alee.homerc;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private TextureView mContentView;

    private TextView mConnectionStatus;
    private ImageButton mConnectionLight;
    private View mSettingsView;
    private EditText mConnectionAddress;

    private VideoPlayer mVideoPlayer;

    private enum ConnectionColours {
        RED,
        AMBER,
        GREEN
    }

    private RemoteDevice mRemoteDevice;

    private ConnectionDetails mConnectionDetails;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagneticField;

    private int mVideoBlocksReceived;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mConnectionDetails = new ConnectionDetails();
        mConnectionDetails.readSettings(this);

        mContentView = (TextureView)findViewById(R.id.fullscreen_content);
        mConnectionStatus = (TextView)findViewById(R.id.connection_status);
        mConnectionLight = (ImageButton) findViewById(R.id.connection_light);

        mSettingsView = findViewById(R.id.connection_settings);
        mConnectionAddress = (EditText)findViewById(R.id.connection_address);

        mSettingsView.setVisibility(View.INVISIBLE);

        final Context context = this;

        ((ImageButton)findViewById(R.id.connection_setup)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mConnectionAddress.setText(mConnectionDetails.getAddress());
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
                mConnectionDetails.saveSettings(context);

                mSettingsView.setVisibility(View.INVISIBLE);
                setVisibility();

                setupVideoTexture();
            }
        });

        setConnectionStatus("Initialising...");
        setVisibility();

        ImageButton forwards = (ImageButton)findViewById(R.id.forward_button);
        forwards.setOnTouchListener(new ButtonTouchListener("forwards"));

        ImageButton backwards = (ImageButton)findViewById(R.id.backward_button);
        backwards.setOnTouchListener(new ButtonTouchListener("backwards"));
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

        setupVideoTexture();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mMagneticField, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopVideo();
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                disconnect();
            }
        });
        worker.start();

        mSensorManager.unregisterListener(this);
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

    private RemoteDevice createRemoteDevice() {
        String address = mConnectionDetails.getAddress();
        if (address.startsWith("BLE:")) {
            return new BluetoothClient();
        } else {
            return new SocketClient();
        }
    }

    private void connect() {
        try {
            disconnect();

            setConnectionStatus("Connecting...");
            setConnectionLight(ConnectionColours.AMBER);
            System.out.println("connecting to: " + mConnectionDetails.getAddress());

            mVideoBlocksReceived = 0;

            RemoteDevice device = createRemoteDevice();
            device.addMessageHandler(new MessageHandler() {
                @Override
                public void connected() {
                    setConnectionStatus("");
                    setConnectionLight(ConnectionColours.GREEN);
                }

                @Override
                public void disconnected(String message) {
                    setConnectionStatus(message);
                    setConnectionLight(ConnectionColours.RED);
                }

                @Override
                public void handleMessage(DeviceMessage message) {
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

            setConnectionLight(ConnectionColours.AMBER);

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
        if (mRemoteDevice != null) {
            mRemoteDevice.disconnect();
            setConnectionStatus("Disconnecting");
            setConnectionLight(ConnectionColours.AMBER);
        }
        mRemoteDevice = null;
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
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                sendMessageSync(message);
            }
        });
        worker.start();
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

    float[] mGravity;
    float[] mGeomagnetic;
    int mLastRoll = 0;
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;

        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);

            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);

                int roll = (int)(orientation[1] * 10);

                // null zone
                if (roll <= 2 && roll >= -2) roll = 0;

                if (roll != mLastRoll) {
                    mLastRoll = roll;

                    sendMessage(new DeviceMessage("steering", Integer.toString((mLastRoll * -10))));
                }

            }
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
