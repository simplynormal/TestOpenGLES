package com.hcmut.test.object;

import android.annotation.SuppressLint;
import android.location.Location;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.hcmut.test.BuildConfig;
import com.hcmut.test.algorithm.CoordinateTransform;
import com.hcmut.test.algorithm.Navigation;
import com.hcmut.test.mapnik.symbolizer.LineSymbolizer;
import com.hcmut.test.mapnik.symbolizer.SymMeta;
import com.hcmut.test.osm.Node;
import com.hcmut.test.osm.Way;
import com.hcmut.test.remote.APIService;
import com.hcmut.test.remote.BaseResponse;
import com.hcmut.test.remote.Coord;
import com.hcmut.test.remote.DirectResponse;
import com.hcmut.test.remote.RetrofitClient;
import com.hcmut.test.utils.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("NewApi")
public class Nav {
    private static final String TAG = Nav.class.getSimpleName();
    private static final float BUFFER_DISTANCE_TO_ACTUAL_SIZE_RATIO = 0.75f;
    private final Config config;
    private final Location destination;
    private final ReentrantLock lock = new ReentrantLock();
    private SymMeta symMeta;
    private List<Coord> route = new ArrayList<>(0);
    private boolean isRequestingRoute = false;

    public Nav(Config config, Location destination) {
        this.config = config;
        this.destination = destination;
    }

    public void setCurLocation(Location currentLocation) {
        lock.lock();
        boolean isOnTrack = Navigation.isLocationNearPolyline(currentLocation, route);
//        Log.d(TAG, "setCurrentLocation: isOnTrack = " + isOnTrack);
        if (!isOnTrack && !isRequestingRoute) {
            requestRoute(currentLocation, destination);
        }
        lock.unlock();
    }

    private void requestRoute(Location source, Location destination) {
        Log.d(TAG, "Requesting route" + source + " -> " + destination);
        isRequestingRoute = true;
        APIService apiService = RetrofitClient.getApiService();
        apiService.getFindDirect(source.getLatitude(), source.getLongitude(), destination.getLatitude(), destination.getLongitude(), "time").enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<List<DirectResponse>>> call, @NonNull Response<BaseResponse<List<DirectResponse>>> response) {
                if (response.isSuccessful()) {
                    assert response.body() != null;
                    List<DirectResponse> directResponses = response.body().getData();
                    if (directResponses != null && directResponses.size() > 0) {
                        DirectResponse directResponse = directResponses.get(0);
                        parseRoute(source, directResponse);
                    }
                }
                isRequestingRoute = false;
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<List<DirectResponse>>> call, @NonNull Throwable t) {
                Log.e(TAG, "onFailure: ", t);
                isRequestingRoute = false;
            }
        });
    }

    private void parseRoute(Location source, DirectResponse directResponse) {
        SymMeta fillSymMeta = null;
        SymMeta caseSymMeta = null;
        SymMeta bufferSymMeta = null;
        route = new ArrayList<>(directResponse.getCoords().size() + 1);
        route.add(new Coord() {{
            setLat(source.getLatitude());
            setLng(source.getLongitude());
            setStreet(new SegmentStreet() {{
                name = "Start";
                type = "unclassified";
            }});
        }});
        route.addAll(directResponse.getCoords());
        for (Coord coord : directResponse.getCoords()) {
            Node node = new Node((float) coord.getLng(), (float) coord.getLat());
            Node enode = new Node((float) coord.geteLng(), (float) coord.geteLat());
            String color = coord.getStatus().color;
            String type = coord.getStreet().type;
            float scaled = CoordinateTransform.getScalePixel(config.getScaleDenominator());
            double width = Navigation.bufferDistanceMap.getOrDefault(type, 5.0) * scaled * BUFFER_DISTANCE_TO_ACTUAL_SIZE_RATIO;

            String fillStrokeWidth = String.valueOf(width);
            String caseStrokeWidth = String.valueOf(width + 4);

            Way way = new Way(List.of(node, enode));
            LineSymbolizer fillLineSymbolizer = new LineSymbolizer(config, fillStrokeWidth, color, null, "butt", "round", null, null);
            LineSymbolizer caseLineSymbolizer = new LineSymbolizer(config, caseStrokeWidth, "#7092FF", null, "butt", "round", null, null);

            SymMeta localFillSymMeta = fillLineSymbolizer.toDrawable(way, null);
            SymMeta localCaseSymMeta = caseLineSymbolizer.toDrawable(way, null);
            if (fillSymMeta == null) {
                fillSymMeta = localFillSymMeta;
                caseSymMeta = localCaseSymMeta;
            } else {
                fillSymMeta = fillSymMeta.append(localFillSymMeta);
                caseSymMeta = caseSymMeta.append(localCaseSymMeta);
            }
        }

        if (caseSymMeta != null) {
            symMeta = caseSymMeta.append(fillSymMeta);
            symMeta.save(config);
        }
    }

    public void draw() {
        if (symMeta != null) {
            symMeta.draw();
        }
    }
}
