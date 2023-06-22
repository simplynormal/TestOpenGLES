package com.hcmut.admin.utrafficsystem.tbt;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.hcmut.admin.utrafficsystem.tbt.local.AppDatabase;
import com.hcmut.admin.utrafficsystem.tbt.remote.Coord;
import com.hcmut.admin.utrafficsystem.tbt.utils.Config;
import com.hcmut.admin.utrafficsystem.tbt.utils.MultisampleConfigChooser;
import com.hcmut.test.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    /**
     * Hold a reference to our mGLSurfaceView
     */
    private GLSurfaceView mGLSurfaceView;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Turn off the window's title bar
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);
//        setContentView(R.layout.turn_by_turn);
//        mGLSurfaceView = findViewById(R.id.glSurfaceView);
        // Check if the system supports OpenGL ES 2.0.
//        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
//        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
//        final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;
//        final TestRenderer testRenderer = new TestRenderer(this, NavTest.startLon, NavTest.startLat, NavTest.endLon, NavTest.endLat);
//
//        if (supportsEs2) {
//            // Request an OpenGL ES 2.0 compatible context.
//            mGLSurfaceView.setEGLContextClientVersion(2);
//            mGLSurfaceView.setEGLConfigChooser(new MultisampleConfigChooser());
//
//            // Set the renderer to our demo renderer, defined below.
//            mGLSurfaceView.setRenderer(testRenderer);
//        } else {
//            // This is where you could create an OpenGL ES 1.x compatible
//            // renderer if you wanted to support both ES 1 and ES 2.
//            return;
//        }
//
//        TextView distanceText = findViewById(R.id.turn_by_turn_distance);
//        TextView timeText = findViewById(R.id.turn_by_turn_time);
//
//        Locale locale = Locale.getDefault();
//        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", locale);
//
//        testRenderer.setOnRouteChanged((disTime, route) -> runOnUiThread(() -> {
//            if (disTime == null || route == null || route.size() == 0) {
//                distanceText.setText("Đang tìm...");
//                timeText.setText("Đang tìm đường");
//                return;
//            }
//            float disInMeter = disTime.first;
//            float timeInMin = disTime.second;
//
//            String dStr = String.format(locale, "%.1f km", disInMeter / 1000);
//            distanceText.setText(dStr);
//            // get current time
//            Calendar calendar = Calendar.getInstance();
//            calendar.add(Calendar.MINUTE, Math.round(timeInMin));
//            String timeStr = timeFormat.format(calendar.getTime());
//
//            String tStr = String.format(locale, "%.1f phút - %s", timeInMin, timeStr);
//            timeText.setText(tStr);
//
//            startTimer(testRenderer, route);
//        }));
//
//        testRenderer.onLocationChange(new Location("") {{
//            setLatitude(NavTest.startLat);
//            setLongitude(NavTest.startLon);
//        }});
//
//        testRenderer.setOnFinish(() -> runOnUiThread(() -> {
//            distanceText.setText("Đã đến nơi");
//            timeText.setText("Đã đến nơi");
//            Toast.makeText(this, "Đã đến nơi", Toast.LENGTH_SHORT).show();
//        }));
//
//        mGLSurfaceView.setOnTouchListener((v, event) -> {
//            if (event != null) {
//                final float eventX = event.getX();
//                final float eventY = event.getY();
//                final int eventType = event.getActionMasked();
//                final int eventPointerCount = event.getPointerCount();
//                if (eventPointerCount > 1) {
//                    List<Float> eventXs = new ArrayList<>();
//                    List<Float> eventYs = new ArrayList<>();
//                    for (int i = 0; i < eventPointerCount; i++) {
//                        eventXs.add(event.getX(i));
//                        eventYs.add(event.getY(i));
//                    }
//                    if (eventType == MotionEvent.ACTION_POINTER_DOWN) {
//                        mGLSurfaceView.queueEvent(() -> testRenderer.actionPointerDown(eventXs, eventYs));
//                    } else if (eventType == MotionEvent.ACTION_MOVE) {
//                        mGLSurfaceView.queueEvent(() -> testRenderer.actionPointerMove(eventXs, eventYs));
//                    } else if (eventType == MotionEvent.ACTION_POINTER_UP) {
//                        mGLSurfaceView.queueEvent(() -> testRenderer.actionPointerUp(eventXs, eventYs));
//                    }
//
//                } else if (eventType == MotionEvent.ACTION_DOWN) {
//                    mGLSurfaceView.queueEvent(() -> testRenderer.actionDown(eventX, eventY));
//                } else if (eventType == MotionEvent.ACTION_MOVE) {
//                    mGLSurfaceView.queueEvent(() -> testRenderer.actionMove(eventX, eventY));
//                } else if (eventType == MotionEvent.ACTION_UP) {
//                    mGLSurfaceView.queueEvent(() -> testRenderer.actionUp(eventX, eventY));
//                }
//                return true;
//            } else {
//                return false;
//            }
//        });
//
//
//        Button button = findViewById(R.id.recenter_btn);
//        button.setOnClickListener(v -> mGLSurfaceView.queueEvent(testRenderer::recenter));

        Config config = new Config(this);
        NavTest.test(config);
    }

    private boolean isRunning = false;

    private void startTimer(TestRenderer testRenderer, List<Coord> route) {
        if (isRunning) {
            return;
        }

        isRunning = true;
        final int[] step = {0};
        final double distance = 500;
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Location location = NavTest.getLatLon(distance, step[0], route);
                if (location == null) {
                    cancel();
                    Log.e(TAG, "Done! Size: " + NavTest.getDatabaseSizeInBytes(getApplicationContext(), AppDatabase.DATABASE_NAME));
                    return;
                }
                Log.v(TAG, "run: " + location);
                step[0] += 1;
                testRenderer.onLocationChange(location);
            }
        }, 5000, 2000);
    }

    @Override
    protected void onResume() {
        // The activity must call the GL surface view's onResume() on activity
        // onResume().
        super.onResume();
//        mGLSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        // The activity must call the GL surface view's onPause() on activity onPause().
        super.onPause();
        // mGLSurfaceView.onPause();
    }

}
