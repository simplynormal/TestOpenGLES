package com.hcmut.test.remote;

import retrofit2.Callback;

public class LayerRequest {
    private static class Bound {
        public final float minLon;
        public final float maxLon;
        public final float minLat;
        public final float maxLat;

        public Bound(float minLon, float maxLon, float minLat, float maxLat) {
            this.minLon = minLon;
            this.maxLon = maxLon;
            this.minLat = minLat;
            this.maxLat = maxLat;
        }
    }
    public final Bound bound;

    public LayerRequest(float minLon, float maxLon, float minLat, float maxLat) {
        this.bound = new Bound(minLon, maxLon, minLat, maxLat);
    }

    public void post(Callback<BaseResponse<LayerResponse>> callback) {
        APITurnByTurn apiTurnByTurn = RetrofitClient.getApiTurnByTurn();
        apiTurnByTurn.postGetLayer(this).enqueue(callback);
    }
}
