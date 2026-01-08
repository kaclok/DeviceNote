package com.smlj.nfcpatrol.core.network;

import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;

public class OkHttpProvider {
    private static OkHttpClient _instance;

    public static OkHttpClient instance() {
        if (_instance == null) {
            _instance = new OkHttpClient.Builder()
                    // 添加拦截器
                    .addInterceptor(headerInterceptor)
                    .addInterceptor(logInterceptor)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .build();
        }
        return _instance;
    }

    private static final Interceptor logInterceptor = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY);

    private static final Interceptor headerInterceptor = chain -> {
        Request original = chain.request();
        Request request = original.newBuilder()
                .header("Authorization", "Bearer " + getToken())
                .build();

        return chain.proceed(request);
    };

    private static String getToken() {
        return "token";
    }
}
