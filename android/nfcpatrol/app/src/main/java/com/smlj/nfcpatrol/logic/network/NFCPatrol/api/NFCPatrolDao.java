package com.smlj.nfcpatrol.logic.network.NFCPatrol.api;

import com.smlj.nfcpatrol.core.network.PageSerializable;
import com.smlj.nfcpatrol.core.network.Result;
import com.smlj.nfcpatrol.core.network.RetrofitProvider;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.LineInfo;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.RecordInfo;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.TNFCPatrolPoint;

import java.util.ArrayList;

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

    public Call<Result<ArrayList<LineInfo>>> queryLinesByDept(String rfId) {
        return api.queryLinesByDept(rfId);
    }

    public Call<Result<ArrayList<RecordInfo>>> queryPointsInfoByLine(int lineid, String queryBegin) {
        return api.queryPointsInfoByLine(lineid, queryBegin);
    }

    public Call<Result<Void>> addRecord(String rfid, String person, String content, int errornum) {
        return api.addRecord(rfid, person, content, errornum);
    }

    public Call<Result<PageSerializable<TNFCPatrolPoint>>> queryPoints(String rfid) {
        return api.queryPoints(rfid);
    }
}
