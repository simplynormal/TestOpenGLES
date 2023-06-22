package com.hcmut.admin.utrafficsystem.tbt.remote;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APITurnByTurn {
    @Headers("Content-Type: application/json")
    @POST("/api/segment/fetch-layers")
    Call<BaseResponse<LayerResponse>> postGetLayer(@Body LayerRequest layerRequest);

//    @Headers({"Content-Type: application/json", "apikey-bk: 6T3UEwanTaOGvSeT02oFgxembKCuezO53F00hXeDdaFKlZMBUVS6NEAQqdJbJBE4"})
//    @POST("/app/data-rqmxw/endpoint/fetchlayer")
//    Call<BaseResponse<LayerResponse>> postGetLayer(@Body LayerRequest layerRequest);
}
