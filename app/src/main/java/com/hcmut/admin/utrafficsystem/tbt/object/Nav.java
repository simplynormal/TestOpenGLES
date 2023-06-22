package com.hcmut.admin.utrafficsystem.tbt.object;

import android.annotation.SuppressLint;
import android.location.Location;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.hcmut.admin.utrafficsystem.tbt.algorithm.CoordinateTransform;
import com.hcmut.admin.utrafficsystem.tbt.mapnik.symbolizer.LineSymbolizer;
import com.hcmut.admin.utrafficsystem.tbt.mapnik.symbolizer.SymMeta;
import com.hcmut.admin.utrafficsystem.tbt.mapnik.symbolizer.Symbolizer;
import com.hcmut.admin.utrafficsystem.tbt.osm.Node;
import com.hcmut.admin.utrafficsystem.tbt.remote.APIService;
import com.hcmut.admin.utrafficsystem.tbt.remote.BaseResponse;
import com.hcmut.admin.utrafficsystem.tbt.remote.Coord;
import com.hcmut.admin.utrafficsystem.tbt.remote.DirectResponse;
import com.hcmut.admin.utrafficsystem.tbt.algorithm.Navigation;
import com.hcmut.admin.utrafficsystem.tbt.osm.Way;
import com.hcmut.admin.utrafficsystem.tbt.remote.RetrofitClient;
import com.hcmut.admin.utrafficsystem.tbt.utils.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("NewApi")
public class Nav {
    private static final String TAG = Nav.class.getSimpleName();
    private static final float BUFFER_DISTANCE_TO_ACTUAL_SIZE_RATIO = 0.75f;
    private static final float BUFFER_DISTANCE_TO_FINISH = 5;
    private final Config config;
    private final Location destination;
    private final ReentrantLock lock = new ReentrantLock();
    private SymMeta symMeta;
    private List<Coord> route = new ArrayList<>(0);
    private float routeTime = 0;
    private float routeDistance = 0;
    private boolean isRequestingRoute = false;
    private BiConsumer<Pair<Float, Float>, List<Coord>> onRouteChanged = (disInMeter, timeInMin) -> {
    };
    private Runnable onFinish = () -> {
    };

    public Nav(Config config, Location destination) {
        this.config = config;
        this.destination = destination;
    }

    public void setOnRouteChanged(BiConsumer<Pair<Float, Float>, List<Coord>> onRouteChanged) {
        this.onRouteChanged = onRouteChanged;
    }

    public void setOnFinish(Runnable onFinish) {
        this.onFinish = onFinish;
    }

    private void checkFinish(Location currentLocation) {
        Location start = new Location("") {{
            setLatitude(route.get(route.size() - 2).getLat());
            setLongitude(route.get(route.size() - 2).getLng());
        }};
        Location end = new Location("") {{
            setLatitude(route.get(route.size() - 2).geteLat());
            setLongitude(route.get(route.size() - 2).geteLng());
        }};
        double[] closestLastSeg = Navigation.calculateClosestPoint(currentLocation, start, end);

        start = new Location("") {{
            setLatitude(route.get(route.size() - 1).getLat());
            setLongitude(route.get(route.size() - 1).getLng());
        }};
        end = new Location("") {{
            setLatitude(route.get(route.size() - 1).geteLat());
            setLongitude(route.get(route.size() - 1).geteLng());
        }};

        double[] closestToDest = Navigation.calculateClosestPoint(currentLocation, start, end);

        Location closestLastSegLocation = new Location("") {{
            setLatitude(closestLastSeg[0]);
            setLongitude(closestLastSeg[1]);
        }};

        Location closestToDestLocation = new Location("") {{
            setLatitude(closestToDest[0]);
            setLongitude(closestToDest[1]);
        }};

        float distanceToLastSeg = currentLocation.distanceTo(closestLastSegLocation);
        float distanceToFinish = currentLocation.distanceTo(closestToDestLocation);
        float distanceToDest = currentLocation.distanceTo(destination);

        if ((distanceToLastSeg <= BUFFER_DISTANCE_TO_FINISH) && (distanceToFinish <= BUFFER_DISTANCE_TO_FINISH || distanceToDest <= BUFFER_DISTANCE_TO_FINISH)) {
            onFinish.run();
        }
    }

    public Location setCurLocation(Location currentLocation) {
        lock.lock();
        Pair<Location, Float> result = Navigation.isLocationNearPolyline(currentLocation, route);
        Location curPoint = result.first;
        if (curPoint == null && !isRequestingRoute) {
            symMeta = null;
            onRouteChanged.accept(null, null);
            requestRoute(currentLocation, destination);
            lock.unlock();
            return currentLocation;
        }
        Float distance = Float.isNaN(result.second) ? null : (1 - result.second) * routeDistance;
        Float time = Float.isNaN(result.second) ? null : (1 - result.second) * routeTime;
        onRouteChanged.accept(new Pair<>(distance, time), route);
        checkFinish(currentLocation);
        lock.unlock();
        return curPoint;
    }

    private void requestRoute(Location source, Location destination) {
        Log.d(TAG, "Requesting route " + source + " -> " + destination);
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
                    setCurLocation(source);
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
        route = new ArrayList<>(directResponse.getCoords().size() + 1);
        routeDistance = directResponse.getDistance();
        routeTime = (float) directResponse.getTime();

        Coord firstCoord = directResponse.getCoords().get(0);
        Coord lastCoord = directResponse.getCoords().get(directResponse.getCoords().size() - 1);
        route.add(new Coord() {{
            setLat(source.getLatitude());
            setLng(source.getLongitude());
            seteLat(firstCoord.getLat());
            seteLng(firstCoord.getLng());
            setStreet(new SegmentStreet() {{
                name = "Start";
                type = "unclassified";
            }});
        }});
        route.addAll(directResponse.getCoords());
        route.add(new Coord() {{
            setLat(lastCoord.geteLat());
            setLng(lastCoord.geteLng());
            seteLat(destination.getLatitude());
            seteLng(destination.getLongitude());
            setStreet(new SegmentStreet() {{
                name = "Finish";
                type = "unclassified";
            }});
        }});
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

            String caseColor = Symbolizer.darkenColor(color, 1, 0.5f);
            LineSymbolizer caseLineSymbolizer = new LineSymbolizer(config, caseStrokeWidth, caseColor, null, "butt", "round", null, null);

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
