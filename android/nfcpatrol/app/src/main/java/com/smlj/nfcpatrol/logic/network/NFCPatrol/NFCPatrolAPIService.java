package com.smlj.nfcpatrol.logic.network.NFCPatrol;

import com.smlj.nfcpatrol.core.network.PageSerializable;
import com.smlj.nfcpatrol.core.network.Result;

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
    @POST("queryPoints")
    // Retrofit 接口方法的参数，必须有注解（@Body / @Query / @Field / @Path 等）
    Call<Result<PageSerializable<TNFCPatrolPoint>>> GetPointInfo(@Field("queryByRfId") String queryByRfId);
}
