package com.smlj.nfcpatrol.core.network.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.smlj.nfcpatrol.core.converter.MillsToDateConverter;
import com.smlj.nfcpatrol.core.network.OkHttpProvider;
import com.smlj.nfcpatrol.core.network.UnwrapConverterFactory;

import java.util.Date;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitProvider {
    private static Retrofit _retrofit;

    private static final String _baseUrl = "http://117.36.227.42:7090/x/";

    private static final Gson _gson = new GsonBuilder().registerTypeAdapter(Date.class, new MillsToDateConverter())
            /*.registerTypeAdapter()*/
            .create();

    public static Retrofit instance() {
        if (_retrofit == null) {
            _retrofit = new Retrofit.Builder()
                    .baseUrl(_baseUrl) // ⚠️ 必须以 / 结尾
                    .client(OkHttpProvider.instance())
                    .addConverterFactory(new UnwrapConverterFactory())
                    .addConverterFactory(GsonConverterFactory.create(_gson))
                    .build();
        }
        return _retrofit;
    }
}
