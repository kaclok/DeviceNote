package com.smlj.nfcpatrol.logic.network.NFCPatrol.api;

import com.smlj.nfcpatrol.core.network.PageSerializable;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.LineInfo;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.RecordInfo;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.TNFCPatrolPoint;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

/*
@Query 只适用于 POST /queryPoints?queryByRfId=123&pageNum=1&pageSize=10

@Field 只适用于 Content-Type: application/x-www-form-urlencoded
@FormUrlEncoded
@Field("xxx")

@Body 只适用于 Content-Type: application/json

@Part 适用于 Content-Type: multipart/form-data
@Multipart
@Part("queryByRfId") RequestBody queryByRfId
 */

public interface NFCPatrolAPIService {
    @FormUrlEncoded
    @POST("nfcPatrol/queryLinesByDept")
// Retrofit 接口方法的参数，必须有注解（@Body / @Query / @Field / @Path 等）
    Call<ArrayList<LineInfo>> queryLinesByDept(@Field("deptid") String deptId);

    @FormUrlEncoded
    @POST("nfcPatrol/queryPointsInfoByLine")
// Retrofit 接口方法的参数，必须有注解（@Body / @Query / @Field / @Path 等）
    Call<ArrayList<RecordInfo>> queryPointsInfoByLine(@Field("lineid") int lineid, @Field("queryBegin") String queryBegin);

    @FormUrlEncoded
    @POST("nfcPatrol/addRecord")
// Retrofit 接口方法的参数，必须有注解（@Body / @Query / @Field / @Path 等）
    Call<Void> addRecord(@Field("rfid") String rfid, @Field("person") String person, @Field("content") String content, @Field("errornum") int errornum, @Field("deptid") String deptid);

    @FormUrlEncoded
    @POST("nfcPatrol/queryPoints")
// Retrofit 接口方法的参数，必须有注解（@Body / @Query / @Field / @Path 等）
    Call<PageSerializable<TNFCPatrolPoint>> queryPoints(@Field("queryByRfId") String queryByRfId);
}
