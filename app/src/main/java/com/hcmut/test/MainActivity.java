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
import android.view.View;
import android.view.Window;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

/**
 * This example use openGL v2.0
 * It creates a render, instead of extending the mGLSurfaceView
 * see ex2 for and extended mGLSurfaceView
 * <p>
 * This code is based off of
 * http://www.learnopengles.com/android-lesson-one-getting-started/
 * <p>
 * Note, there is no xml layout for this example. It's all done in onCreate and
 * the render.
 */

public class MainActivity extends Activity {

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
        return "";
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Test.main();
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
            mGLSurfaceView.setEGLConfigChooser((egl, display) -> {
                int[] attributes = new int[]{
                        EGL10.EGL_RED_SIZE, 8,
                        EGL10.EGL_GREEN_SIZE, 8,
                        EGL10.EGL_BLUE_SIZE, 8,
                        EGL10.EGL_ALPHA_SIZE, 8,
                        EGL10.EGL_DEPTH_SIZE, 16,
                        EGL10.EGL_STENCIL_SIZE, 0,
                        EGL10.EGL_RENDERABLE_TYPE, 4,
//                        EGL10.EGL_SAMPLE_BUFFERS, 1,
//                        EGL10.EGL_SAMPLES, 4,
                        EGL10.EGL_NONE
                };
                int[] result = new int[1];
                egl.eglChooseConfig(display, attributes, null, 0, result);
                int numConfigs = result[0];
                if (numConfigs <= 0) {
                    throw new IllegalArgumentException("No configs match configSpec");
                }
                EGLConfig[] configs = new EGLConfig[numConfigs];
                egl.eglChooseConfig(display, attributes, configs, numConfigs, result);
                return configs[0];
            });

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
                final int eventType = event.getAction();
                final int eventPointerCount = event.getPointerCount();
//                System.out.println("Action: " + actionToString(eventType));
                if (eventPointerCount > 1) {
                    List<Float> eventXs = new ArrayList<>();
                    List<Float> eventYs = new ArrayList<>();
                    for (int i = 0; i < eventPointerCount; i++) {
                        eventXs.add(event.getX(i));
                        eventYs.add(event.getY(i));
                    }
                    if (eventType == MotionEvent.ACTION_POINTER_DOWN) {
                        System.out.println("ACTION_POINTER_DOWN" + eventXs + " " + eventYs);
                    } else if (eventType == MotionEvent.ACTION_MOVE) {
                        mGLSurfaceView.queueEvent(() -> testRenderer.handleZoom(eventXs, eventYs));
                    } else if (eventType == MotionEvent.ACTION_POINTER_UP) {
                        mGLSurfaceView.queueEvent(testRenderer::handleResetZoom);
                    }

                } else if (eventType == MotionEvent.ACTION_DOWN) {
                    mGLSurfaceView.queueEvent(() -> testRenderer.handleTouchPress(eventX, eventY));
                } else if (eventType == MotionEvent.ACTION_MOVE) {
                    mGLSurfaceView.queueEvent(() -> testRenderer.handleTouchDrag(eventX, eventY));
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
