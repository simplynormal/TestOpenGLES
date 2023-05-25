package com.hcmut.test.object;

import android.annotation.SuppressLint;
import android.location.Location;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

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
    private static final float FILL_STROKE_WIDTH = 10;
    private static final float CASE_STROKE_WIDTH = 14;
    private final Config config;
    private final Location destination;
    private final ReentrantLock lock = new ReentrantLock();
    private SymMeta symMeta;
    private List<Pair<Location, Location>> route = new ArrayList<>(0);

    public Nav(Config config, Location destination) {
        this.config = config;
        this.destination = destination;
    }

    public void setCurLocation(Location currentLocation) {
        lock.lock();
        boolean isOnTrack = Navigation.isLocationNearPolyline(currentLocation, route, 30);
        Log.d(TAG, "setCurrentLocation: isOnTrack = " + isOnTrack);
        if (!isOnTrack) {
            requestRoute(currentLocation, destination);
        }
        lock.unlock();
    }

    private void requestRoute(Location source, Location destination) {
        Log.d(TAG, "Requesting route" + source + " -> " + destination);
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
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<List<DirectResponse>>> call, @NonNull Throwable t) {
                Log.e(TAG, "onFailure: ", t);
            }
        });
    }

    private void parseRoute(Location source, DirectResponse directResponse) {
        String fillStrokeWidth = String.valueOf(FILL_STROKE_WIDTH);
        String caseStrokeWidth = String.valueOf(CASE_STROKE_WIDTH);
        SymMeta fillSymMeta = null;
        SymMeta caseSymMeta = null;
        route = new ArrayList<>(directResponse.getCoords().size() + 1);
        route.add(new Pair<>(
                new Location("start") {{
                    setLatitude(source.getLatitude());
                    setLongitude(source.getLongitude());
                }},
                new Location("end") {{
                    setLatitude(directResponse.getCoords().get(0).getLat());
                    setLongitude(directResponse.getCoords().get(0).getLng());
                }}
        ));
        for (Coord coord : directResponse.getCoords()) {
            Node node = new Node((float) coord.getLng(), (float) coord.getLat());
            Node enode = new Node((float) coord.geteLng(), (float) coord.geteLat());
            route.add(new Pair<>(
                    new Location("start") {{
                        setLatitude(coord.getLat());
                        setLongitude(coord.getLng());
                    }},
                    new Location("end") {{
                        setLatitude(coord.geteLat());
                        setLongitude(coord.geteLng());
                    }}
            ));
            String color = coord.getStatus().color;

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
