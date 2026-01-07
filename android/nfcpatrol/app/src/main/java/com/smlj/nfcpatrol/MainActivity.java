package com.smlj.nfcpatrol;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.reflect.TypeToken;
import com.smlj.nfcpatrol.core.ActivityCallback;
import com.smlj.nfcpatrol.core.ResultCallback;
import com.smlj.nfcpatrol.core.PageSerializable;
import com.smlj.nfcpatrol.core.Result;
import com.smlj.nfcpatrol.network.apiService.NFCPatrol;
import com.smlj.nfcpatrol.network.apiService.TNFCPatrolPoint;

import java.io.IOException;

import okhttp3.Call;

public class MainActivity extends AppCompatActivity {
    private ActivityResultLauncher<Intent> nfcLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 注册NFC扫描启动器
        // 结果回调
        nfcLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handleNfcResult
        );

        findViewById(R.id.btn_nfc).setOnClickListener(v -> {
            Intent intent = new Intent(this, NFCScanActivity.class);
            nfcLauncher.launch(intent);
        });
    }

    private void handleNfcResult(ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_OK) {
            Intent data = result.getData();
            if (data != null) {
                String rfId = data.getStringExtra("rfId");
                // Toast.makeText(this, "扫描成功", Toast.LENGTH_SHORT).show();

                var type = new TypeToken<Result<PageSerializable<TNFCPatrolPoint>>>() {
                }.getType();

                NFCPatrol.GetPointInfo(rfId).enqueue(new ActivityCallback<>(this, type, new ResultCallback<PageSerializable<TNFCPatrolPoint>>() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    }

                    @Override
                    public void onResponse(@NonNull Call call, Result<PageSerializable<TNFCPatrolPoint>> result) {
                        if (result.code == 200) {
                            Toast.makeText(MainActivity.this, result.data.toString(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }));
            }
        }
    }
}