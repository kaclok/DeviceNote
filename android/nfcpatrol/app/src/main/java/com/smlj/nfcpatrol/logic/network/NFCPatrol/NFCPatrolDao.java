package com.smlj.nfcpatrol.logic.network.NFCPatrol;

import com.smlj.nfcpatrol.core.network.PageSerializable;
import com.smlj.nfcpatrol.core.network.Result;
import com.smlj.nfcpatrol.core.network.RetrofitProvider;

import retrofit2.Call;

public class NFCPatrolDao {
    private static final NFCPatrolAPIService api = RetrofitProvider.instance().create(NFCPatrolAPIService.class);

    private static NFCPatrolDao _instance;

    public static NFCPatrolDao instance() {
        if (_instance == null) {
            _instance = new NFCPatrolDao();
        }
        return _instance;
    }

    public Call<Result<PageSerializable<TNFCPatrolPoint>>> GetPointInfo(String rfId) {
        return api.GetPointInfo(rfId);
    }
}
