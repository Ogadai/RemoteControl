package com.ogadai.alee.homerc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;

import java.net.URI;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private TextView mContentView;

    private SocketClient mClient;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagneticField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mContentView = (TextView)findViewById(R.id.fullscreen_content);
        setConnectionStatus("Initialising...");

        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        Button forwards = (Button)findViewById(R.id.forward_button);
        forwards.setOnTouchListener(new ButtonTouchListener("fowards"));

        Button backwards = (Button)findViewById(R.id.backward_button);
        backwards.setOnTouchListener(new ButtonTouchListener("backwards"));
    }

    @Override
    protected void onResume() {
        super.onResume();

        setConnectionStatus("Connecting...");
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                connectSocket();
            }
        });
        worker.start();

        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mMagneticField, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();

        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                disconnectSocket();
            }
        });
        worker.start();

        mSensorManager.unregisterListener(this);
    }

    private void setConnectionStatus(final String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mContentView.setText(status);
            }
        });
    }

    private void connectSocket() {
        try {
            SocketClient client = new SocketClient();

            client.addMessageHandler(new SocketClient.MessageHandler() {
                @Override
                public void handleMessage(String message) {

                }

                @Override
                public void handleMessage(byte[] message) {

                }
            });

            URI uri = new URI("ws://192.168.36.51:8080");
            client.Connect(uri);

            mClient = client;
            setConnectionStatus("Connected");
        } catch (Exception e) {
            System.out.println("error connecting socket: " + e.getMessage());
            setConnectionStatus("Error: " + e.getMessage());
            mClient = null;
        }
    }

    private void disconnectSocket() {
        if (mClient != null) {
            mClient.Disconnect();
        }
        mClient = null;
        setConnectionStatus("Disconnected");
    }

    private void sendMessage(DeviceMessage message) {
        Gson gson = new Gson();
        String messageStr = gson.toJson(message);

        System.out.println(messageStr);
        if (mClient != null) {
            mClient.sendMessage(messageStr);
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
