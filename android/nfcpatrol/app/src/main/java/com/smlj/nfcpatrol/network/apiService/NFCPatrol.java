package com.smlj.nfcpatrol.network.apiService;

import com.smlj.nfcpatrol.network.OkHttp;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.Request;

public class NFCPatrol {
    public static Call GetPointInfo(String rfId) {
        Request request = new Request.Builder()
                .url(OkHttp.baseUrl + "queryPoints")
                .post(new FormBody.Builder()
                        .add("queryByRfId", "rfId")
                        .build())
                .build();

        return OkHttp.transfer.newCall(request);
    }
}
