package com.smlj.nfcpatrol.logic.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.smlj.nfcpatrol.core.utils.VersionMgr;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            VersionMgr.check(this, this::_onNeed, this::_onNoNeed);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void _onNeed() {
        new AlertDialog.Builder(this)
                .setTitle("发现新版本")
                .setCancelable(false) // 禁止返回键取消
                .setPositiveButton("立即更新", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(VersionMgr.downloadUrl));
                        startActivity(intent);
                        // 如果是强制更新，退出应用
                        finish();
                    }
                })
                .setNegativeButton("退出应用", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .create().show();
    }

    private void _onNoNeed() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }
}