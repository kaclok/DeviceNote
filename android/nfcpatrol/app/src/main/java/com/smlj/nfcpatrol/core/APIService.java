package com.smlj.nfcpatrol.core;

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

public interface APIService {
    @FormUrlEncoded
    @POST("remotePackageVersion")
    Call<String> remotePackageVersion(@Field("packageName") String packageName);
}
