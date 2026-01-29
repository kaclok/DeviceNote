package com.smlj.nfcpatrol.core.utils;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.smlj.nfcpatrol.core.APIServiceDao;
import com.smlj.nfcpatrol.core.Version;
import com.smlj.nfcpatrol.core.network.ActivitySafeCallback;

import retrofit2.Call;

public class VersionMgr {
    private static final String _TAG = "VersionMgr";
    public static final String downloadUrl = "http://117.36.227.42:4177/pages/downloads/index.html";

    public static void check(Activity activity, Runnable onNeed, Runnable onNoNeed) throws PackageManager.NameNotFoundException {
        // 1. 获取本地版本
        var local = localVersion(activity);
        Log.d(_TAG, "本地版本：" + local);

        // 2. 请求远程 JSON
        var call = APIServiceDao.instance().remotePackageVersion(activity.getPackageName());
        call.enqueue(new ActivitySafeCallback<String>(activity) {
            @Override
            protected void onSafeResponse(Activity activity, Call<String> call, String remote) {
                Log.d(_TAG, "远程版本：" + remote);
                // 3. 比较版本
                var diff = local.compareTo(new Version(remote));
                if (diff < 0) {
                    onNeed.run();
                } else {
                    onNoNeed.run();
                }
            }
        });
    }

    public static Version localVersion(Activity activity) throws PackageManager.NameNotFoundException {
        PackageManager packageManager = activity.getPackageManager();
        PackageInfo packageInfo = packageManager.getPackageInfo(activity.getPackageName(), 0);
        if (packageInfo.versionName != null) {
            return new Version(packageInfo.versionName);
        } else {
            Log.e(_TAG, "未设置versionName");
            return new Version("0.0.0");
        }
    }
}
