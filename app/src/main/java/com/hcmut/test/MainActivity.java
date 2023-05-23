package com.hcmut.test;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Window;

import com.hcmut.test.utils.MultisampleConfigChooser;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    /**
     * Hold a reference to our mGLSurfaceView
     */
    private GLSurfaceView mGLSurfaceView;

    public static String actionToString(int action) {
        switch (action) {

            case MotionEvent.ACTION_DOWN:
                return "Down";
            case MotionEvent.ACTION_MOVE:
                return "Move";
            case MotionEvent.ACTION_POINTER_DOWN:
                return "Pointer Down";
            case MotionEvent.ACTION_UP:
                return "Up";
            case MotionEvent.ACTION_POINTER_UP:
                return "Pointer Up";
            case MotionEvent.ACTION_OUTSIDE:
                return "Outside";
            case MotionEvent.ACTION_CANCEL:
                return "Cancel";
        }
        return action + "";
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Turn off the window's title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);
        mGLSurfaceView = new GLSurfaceView(this);
        // Check if the system supports OpenGL ES 2.0.
        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;
        final TestRenderer testRenderer = new TestRenderer(this);

        if (supportsEs2) {
            // Request an OpenGL ES 2.0 compatible context.
            mGLSurfaceView.setEGLContextClientVersion(2);
            mGLSurfaceView.setEGLConfigChooser(new MultisampleConfigChooser());

            // Set the renderer to our demo renderer, defined below.
            mGLSurfaceView.setRenderer(testRenderer);
        } else {
            // This is where you could create an OpenGL ES 1.x compatible
            // renderer if you wanted to support both ES 1 and ES 2.
            return;
        }
        mGLSurfaceView.setOnTouchListener((v, event) -> {
            if (event != null) {
                final float eventX = event.getX();
                final float eventY = event.getY();
                final int eventType = event.getActionMasked();
                final int eventPointerCount = event.getPointerCount();
                Log.d(TAG, "Action: " + actionToString(eventType));
                if (eventPointerCount > 1) {
                    List<Float> eventXs = new ArrayList<>();
                    List<Float> eventYs = new ArrayList<>();
                    for (int i = 0; i < eventPointerCount; i++) {
                        eventXs.add(event.getX(i));
                        eventYs.add(event.getY(i));
                    }
                    if (eventType == MotionEvent.ACTION_POINTER_DOWN) {
                        mGLSurfaceView.queueEvent(() -> testRenderer.actionPointerDown(eventXs, eventYs));
                    } else if (eventType == MotionEvent.ACTION_MOVE) {
                        mGLSurfaceView.queueEvent(() -> testRenderer.actionPointerMove(eventXs, eventYs));
                    } else if (eventType == MotionEvent.ACTION_POINTER_UP) {
                        mGLSurfaceView.queueEvent(() -> testRenderer.actionPointerUp(eventXs, eventYs));
                    }

                } else if (eventType == MotionEvent.ACTION_DOWN) {
                    mGLSurfaceView.queueEvent(() -> testRenderer.actionDown(eventX, eventY));
                } else if (eventType == MotionEvent.ACTION_MOVE) {
                    mGLSurfaceView.queueEvent(() -> testRenderer.actionMove(eventX, eventY));
                } else if (eventType == MotionEvent.ACTION_POINTER_UP) {
                    mGLSurfaceView.queueEvent(() -> testRenderer.actionUp(eventX, eventY));
                }
                return true;
            } else {
                return false;
            }
        });
        setContentView(mGLSurfaceView);
    }

    @Override
    protected void onResume() {
        // The activity must call the GL surface view's onResume() on activity
        // onResume().
        super.onResume();
        mGLSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        // The activity must call the GL surface view's onPause() on activity onPause().
        super.onPause();
        // mGLSurfaceView.onPause();
    }

}
