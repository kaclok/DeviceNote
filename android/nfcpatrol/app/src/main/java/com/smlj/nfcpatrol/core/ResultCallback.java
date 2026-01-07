package com.smlj.nfcpatrol.core;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Call;

public interface ResultCallback<T> {
    public void onFailure(@NonNull Call call, @NonNull IOException e);

    public void onResponse(@NonNull Call call, Result<T> result) ;
}
