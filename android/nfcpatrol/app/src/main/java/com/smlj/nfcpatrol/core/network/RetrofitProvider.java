package com.smlj.nfcpatrol.core.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.smlj.nfcpatrol.core.network.converter.MillsToDateConverter;

import java.util.Date;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitProvider {
    private static Retrofit _retrofit;

    private static final String _baseUrl = "http://10.8.54.24:7090/x/nfcPatrol/";

    private static final Gson _gson = new GsonBuilder().registerTypeAdapter(Date.class, new MillsToDateConverter())
            /*.registerTypeAdapter()*/
            .create();

    public static Retrofit instance() {
        if (_retrofit == null) {
            _retrofit = new Retrofit.Builder()
                    .baseUrl(_baseUrl) // ⚠️ 必须以 / 结尾
                    .client(OkHttpProvider.instance())
                    .addConverterFactory(GsonConverterFactory.create(_gson))
                    .build();
        }
        return _retrofit;
    }
}
