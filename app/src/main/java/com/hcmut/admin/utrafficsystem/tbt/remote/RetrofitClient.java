package com.hcmut.admin.utrafficsystem.tbt.remote;

import android.os.Process;


import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    public static final ThreadPoolExecutor THREAD_POOL_EXECUTOR;
    private static APITurnByTurn apiTurnByTurn;
    private static APIService apiService;

    static {
        THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
                NUMBER_OF_CORES,
                NUMBER_OF_CORES,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());

        THREAD_POOL_EXECUTOR.setThreadFactory(new PriorityThreadFactory(
                Process.THREAD_PRIORITY_BACKGROUND, "Threadpool"));
    }

    private static Retrofit builder(String url) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder okHttpClient = new OkHttpClient.Builder();
        okHttpClient.readTimeout(30000, TimeUnit.MILLISECONDS);
        okHttpClient.connectTimeout(30000, TimeUnit.MILLISECONDS);
        okHttpClient.callTimeout(30000, TimeUnit.MILLISECONDS);
//        okHttpClient.addInterceptor(logging);

        return new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(url)
                .client(okHttpClient.build())
                .build();
    }

    public static APITurnByTurn getApiTurnByTurn() {
        if (apiTurnByTurn == null) {
//            String baseURL = "https://api.bktraffic.com";
//            String baseURL = "https://ap-southeast-1.aws.data.mongodb-api.com";
            String baseURL = "http://192.168.1.106:3000";
            apiTurnByTurn = builder(baseURL).create(APITurnByTurn.class);
        }
        return apiTurnByTurn;
    }

    public static APIService getApiService() {
        if (apiService == null) {
//            String baseURL = "https://api.bktraffic.com";
            String baseURL = "http://192.168.1.106:3000";
            apiService = builder(baseURL).create(APIService.class);
        }
        return apiService;
    }
}
