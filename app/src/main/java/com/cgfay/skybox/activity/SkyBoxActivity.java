/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material,
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose.
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
***/
package com.cgfay.skybox.activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import com.cgfay.skybox.widget.SkyBoxRenderer;
import com.cgfay.skybox.widget.ClickableGLSurfaceView;

public class SkyBoxActivity extends Activity
        implements SurfaceTexture.OnFrameAvailableListener, SensorEventListener {
    /**
     * Hold a reference to our GLSurfaceView
     */
    private ClickableGLSurfaceView mGlSurfaceView;
    private SkyBoxRenderer mSkyBoxRenderer;
    private boolean rendererSet = false;

    private SensorManager mSensorManager;
    private Sensor mRotationSensor;
    private float[] mRotationMatrix = new float[16];

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGlSurfaceView = new ClickableGLSurfaceView(this);

        // Check if the system supports OpenGL ES 2.0.
        ActivityManager activityManager = 
            (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo configurationInfo = activityManager
            .getDeviceConfigurationInfo();
        // Even though the latest emulator supports OpenGL ES 2.0,
        // it has a bug where it doesn't set the reqGlEsVersion so
        // the above check doesn't work. The below will detect if the
        // app is running on an emulator, and assume that it supports
        // OpenGL ES 2.0.
        final boolean supportsEs2 =
            configurationInfo.reqGlEsVersion >= 0x20000
                || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                 && (Build.FINGERPRINT.startsWith("generic")
                  || Build.FINGERPRINT.startsWith("unknown")
                  || Build.MODEL.contains("google_sdk")
                  || Build.MODEL.contains("Emulator")
                  || Build.MODEL.contains("Android SDK built for x86")));

        mSkyBoxRenderer = new SkyBoxRenderer(this);

        if (supportsEs2) {
            // Request an OpenGL ES 2.0 compatible context.
            mGlSurfaceView.setEGLContextClientVersion(2);

            // Assign our renderer.
            mGlSurfaceView.setRenderer(mSkyBoxRenderer);
            rendererSet = true;
        } else {
            /*
             * This is where you could create an OpenGL ES 1.x compatible
             * renderer if you wanted to support both ES 1 and ES 2. Since
             * we're not doing anything, the app will crash if the device
             * doesn't support OpenGL ES 2.0. If we publish on the market, we
             * should also add the following to AndroidManifest.xml:
             *
             * <uses-feature android:glEsVersion="0x00020000"
             * android:required="true" />
             *
             * This hides our app from those devices which don't support OpenGL
             * ES 2.0.
             */
            Toast.makeText(this, "This device does not support OpenGL ES 2.0.",
                Toast.LENGTH_LONG).show();
            return;
        }
        mGlSurfaceView.addClickListener(new ClickableGLSurfaceView.OnClickListener() {
            @Override
            public void onClick(float x, float y) {
                // 转成Opengles 的坐标
                int width = mSkyBoxRenderer.getWidth();
                int height = mSkyBoxRenderer.getHeight();
                float normalizedX = (x - width * 0.5f) / (width * 0.5f);
                float normalizedY = (y - height * 0.5f) / (height * 0.5f);
                mSkyBoxRenderer.setDirection(normalizedX, -normalizedY);
            }

            @Override
            public void doubleClick(float x, float y) {

            }
        });

        setContentView(mGlSurfaceView);

        // 方向向量s
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        Matrix.setIdentityM(mRotationMatrix, 0);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        if (rendererSet) {
            mGlSurfaceView.onPause();
        }

        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        if (rendererSet) {
            mGlSurfaceView.onResume();
        }

        mSensorManager.registerListener(this, mRotationSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mGlSurfaceView.requestRender();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);
        if (mGlSurfaceView != null) {
            mGlSurfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    mSkyBoxRenderer.setRotationMatrix(mRotationMatrix);
                }
            });
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}