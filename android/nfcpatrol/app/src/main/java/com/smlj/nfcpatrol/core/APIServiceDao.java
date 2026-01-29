package com.smlj.nfcpatrol.core;

import com.smlj.nfcpatrol.core.network.http.RetrofitProvider;

import retrofit2.Call;

public class APIServiceDao {
    private static final APIService api = RetrofitProvider.instance().create(APIService.class);

    private static APIServiceDao _instance;

    public static APIServiceDao instance() {
        if (_instance == null) {
            _instance = new APIServiceDao();
        }
        return _instance;
    }

    public Call<String> remotePackageVersion(String packageName) {
        return api.remotePackageVersion(packageName);
    }
}
