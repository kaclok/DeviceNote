package com.smlj.nfcpatrol.core;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ActivityCallback<T> implements Callback {
    private final WeakReference<Activity> activityRef;
    private final ResultCallback<T> callback;
    private final Type resultType;

    public ActivityCallback(Activity activity, Type resultType, ResultCallback<T> callback) {
        this.activityRef = new WeakReference<>(activity);
        this.resultType = resultType;
        this.callback = callback;
    }

    @Override
    public void onFailure(@NonNull Call call, @NonNull IOException e) {
        Activity act = activityRef.get();
        if (act == null || act.isFinishing()) {
            return;
        }

        act.runOnUiThread(() -> {
            this.callback.onFailure(call, e);
        });
    }

    @Override
    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
        Activity act = activityRef.get();
        if (act == null || act.isFinishing()) {
            return;
        }

        var json = response.body().string();
        Result<T> r = new Gson().fromJson(json, resultType);

        act.runOnUiThread(() -> {
            this.callback.onResponse(call, r);
        });
    }
}