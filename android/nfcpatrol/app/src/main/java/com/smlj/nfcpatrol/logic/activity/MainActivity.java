package com.smlj.nfcpatrol.logic.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.smlj.nfcpatrol.R;
import com.smlj.nfcpatrol.core.ActivitySafeCallback;
import com.smlj.nfcpatrol.core.network.PageSerializable;
import com.smlj.nfcpatrol.core.network.Result;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.NFCPatrolDao;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.TNFCPatrolPoint;

import retrofit2.Call;

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

                // Retrofit 的 onResponse() 里 可以 直接操作 Activity 的 View，但“有前提条件”，否则会有隐患。
                // Retrofit（Android 版本）默认会把 enqueue() 的回调切回主线程（UI 线程）
                NFCPatrolDao.instance().GetPointInfo(rfId).enqueue(new ActivitySafeCallback<>(this) {
                    @Override
                    protected void onSafeResponse(Activity activity, Call<Result<PageSerializable<TNFCPatrolPoint>>> call, Result<PageSerializable<TNFCPatrolPoint>> response) {
                        if (response.code == 200) {
                            Toast.makeText(activity, response.data.toString(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    protected void onSafeFailure(Activity activity, Call<Result<PageSerializable<TNFCPatrolPoint>>> call, Throwable t) {

                    }
                });
            }
        }
    }
}