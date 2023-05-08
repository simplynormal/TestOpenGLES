package com.hcmut.test.remote;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APITurnByTurn {
    @Headers("Content-Type: application/json")
    @POST("/api/segment/fetch-layers")
    Call<BaseResponse<LayerResponse>> postGetLayer(@Body LayerRequest layerRequest);
}
