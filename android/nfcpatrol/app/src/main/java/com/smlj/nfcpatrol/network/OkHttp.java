package com.smlj.nfcpatrol.network;

import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class OkHttp {
    public static final String baseUrl = "http://10.8.54.24:7090/x/nfcPatrol/";

    public static final Interceptor headerInterceptor = chain -> {
        Request original = chain.request();

        Request request = original.newBuilder()
                .header("Authorization", "Bearer " + getToken())
                .header("Content-Type", "application/json")
                .build();

        return chain.proceed(request);
    };

    public static final OkHttpClient transfer = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            // 添加拦截器
            .addInterceptor(headerInterceptor)
            .build();

    private static String getToken() {
        return "token";
    }
}
