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

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

final class MultisampleConfigChooser implements GLSurfaceView.EGLConfigChooser {

    private static final int EGL_COVERAGE_BUFFERS_NV = 0x30E0;
    private static final int EGL_COVERAGE_SAMPLES_NV = 0x30E1;

    private static final int RED = 8;
    private static final int GREEN = 8;
    private static final int BLUE = 8;
    private static final int ALPHA = 8;
    private static final int STENCIL = 8;
    private static final int DEPTH = 16;

    private static int findConfigAttrib(@NonNull EGL10 gl, @NonNull EGLDisplay display, @NonNull EGLConfig config, int attribute, int defaultValue, int[] tmp) {
        if (gl.eglGetConfigAttrib(display, config, attribute, tmp)) {
            return tmp[0];
        }
        return defaultValue;
    }

    @Override
    public EGLConfig chooseConfig(@NonNull EGL10 gl, @NonNull EGLDisplay display) {
        // try to find a normal multisample configuration first.
        EGLConfig config = ConfigData.create(EGL10.EGL_RED_SIZE, RED,
                EGL10.EGL_GREEN_SIZE, GREEN,
                EGL10.EGL_BLUE_SIZE, BLUE,
                EGL10.EGL_ALPHA_SIZE, ALPHA,
                EGL10.EGL_STENCIL_SIZE, STENCIL,
                EGL10.EGL_DEPTH_SIZE, DEPTH,
                EGL10.EGL_RENDERABLE_TYPE, 4 /* EGL_OPENGL_ES2_BIT */,
                EGL10.EGL_SAMPLE_BUFFERS, 1 /* true */,
                EGL10.EGL_SAMPLES, 2,
                EGL10.EGL_NONE).tryConfig(gl, display);
        if (config != null) {
            System.out.println("Using normal multisampling");
            return config;
        }

        // no normal multisampling config was found. Try to create a
        // coverage multisampling configuration, for the nVidia Tegra2.
        // See the EGL_NV_coverage_sample documentation.
        config = ConfigData.create(EGL10.EGL_RED_SIZE, RED,
                EGL10.EGL_GREEN_SIZE, GREEN,
                EGL10.EGL_BLUE_SIZE, BLUE,
                EGL10.EGL_ALPHA_SIZE, ALPHA,
                EGL10.EGL_STENCIL_SIZE, STENCIL,
                EGL10.EGL_DEPTH_SIZE, DEPTH,
                EGL10.EGL_RENDERABLE_TYPE, 4 /* EGL_OPENGL_ES2_BIT */,
                EGL_COVERAGE_BUFFERS_NV, 1 /* true */,
                EGL_COVERAGE_SAMPLES_NV, 2,  // always 5 in practice on tegra 2
                EGL10.EGL_NONE).tryConfig(gl, display);
        if (config != null) {
            System.out.println("Using coverage multisampling");
            return config;
        }

        // fallback to simple configuration
        config = ConfigData.create(
                EGL10.EGL_RED_SIZE, RED,
                EGL10.EGL_GREEN_SIZE, GREEN,
                EGL10.EGL_BLUE_SIZE, BLUE,
                EGL10.EGL_ALPHA_SIZE, ALPHA,
                EGL10.EGL_STENCIL_SIZE, STENCIL,
                EGL10.EGL_DEPTH_SIZE, DEPTH,
                EGL10.EGL_NONE).tryConfig(gl, display);
        if (config != null) {
            System.out.println("Using no multisampling");
            return config;
        }

        throw new IllegalArgumentException("No supported configuration found");
    }

    private static final class ConfigData {
        final int[] spec;

        private ConfigData(int[] spec) {
            this.spec = spec;
        }

        @NonNull
        private static ConfigData create(int... spec) {
            return new ConfigData(spec);
        }

        @Nullable
        private EGLConfig tryConfig(@NonNull EGL10 gl, @NonNull EGLDisplay display) {
            final int[] tmp = new int[1];

            if (!gl.eglChooseConfig(display, spec, null, 0, tmp)) {
                return null;
            }
            final int count = tmp[0];
            if (count > 0) {
                // get all matching configurations
                final EGLConfig[] configs = new EGLConfig[count];
                if (!gl.eglChooseConfig(display, spec, configs, count, tmp)) {
                    return null;
                }

                return findConfig(gl, display, configs, tmp);
            }

            return null;
        }

        @Nullable
        private EGLConfig findConfig(@NonNull EGL10 gl, @NonNull EGLDisplay display, @NonNull EGLConfig[] configs, int[] tmp) {
            // sometimes eglChooseConfig returns configurations with not requested
            // options: even though we asked for rgb565 configurations, rgb888
            // configurations are considered to be "better" and returned first.
            // We need to explicitly filter data returned by eglChooseConfig
            // adn choose the right configuration.
            for (final EGLConfig config : configs) {
                if (config != null && isDesiredConfig(gl, display, tmp, config)) {
                    return config;
                }
            }

            return null;
        }

        private boolean isDesiredConfig(@NonNull EGL10 gl, @NonNull EGLDisplay display, @NonNull int[] tmp, @NonNull EGLConfig config) {
            for (int i = 0; i + 1 < spec.length; i += 2) {
                final int attribute = spec[i];
                final int desiredValue = spec[i + 1];
                final int actualValue = findConfigAttrib(gl, display, config, attribute, 0, tmp);
                if (attribute == EGL10.EGL_DEPTH_SIZE) {
                    if (actualValue < desiredValue) {
                        return false;
                    }
                } else {
                    if (desiredValue != actualValue) {
                        return false;
                    }
                }
            }

            return true;
        }
    }
}

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
                System.out.println("Action: " + actionToString(eventType));
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
