package com.smlj.nfcpatrol.core.network;

import android.app.Activity;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public abstract class ActivitySafeCallback<T> implements Callback<T> {
    private final WeakReference<Activity> activityRef;

    public ActivitySafeCallback(Activity activity) {
        this.activityRef = new WeakReference<>(activity);
    }

    protected Activity getActivity() {
        Activity activity = activityRef.get();
        if (activity == null) return null;
        if (activity.isFinishing()) return null;
        if (activity.isDestroyed()) return null;
        return activity;
    }

    @Override
    public final void onResponse(@NonNull Call<T> call, @NonNull Response<T> response) {
        Activity activity = getActivity();
        if (activity != null && !activity.isFinishing()) {
            if (response.isSuccessful()) {
                // 现在 response.body() 直接就是 T 类型，没有 Result 包装
                onSafeResponse(activity, call, response.body());
            } else {
                onSafeFailure(activity, call, new ApiException(response.code(), response.message()));
            }
        }
    }

    @Override
    public final void onFailure(@NonNull Call<T> call, @NonNull Throwable t) {
        Activity activity = getActivity();
        if (activity != null && !activity.isFinishing()) {
            onSafeFailure(activity, call, t);
        }
    }

    // 由子类实现
    protected abstract void onSafeResponse(Activity activity, Call<T> call, T resp);

    protected void onSafeFailure(Activity activity, Call<T> call, Throwable t) {
    }
}