package com.hcmut.test.remote;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface APIService {
    @GET("api/segment/direct")
    Call<BaseResponse<List<DirectResponse>>> getFindDirect(@Query("slat") double slat,
                                                           @Query("slng") double slng,
                                                           @Query("elat") double elat,
                                                           @Query("elng") double elng,
                                                           @Query("type") String type);
}
