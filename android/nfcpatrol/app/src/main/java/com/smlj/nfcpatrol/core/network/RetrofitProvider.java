package com.smlj.nfcpatrol.core.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitProvider {
    private static Retrofit _retrofit;

    private static final String baseUrl = "http://10.8.54.24:7090/x/nfcPatrol/";

    public static Retrofit instance() {
        if (_retrofit == null) {
            _retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl) // ⚠️ 必须以 / 结尾
                    .client(OkHttpProvider.instance())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return _retrofit;
    }
}
