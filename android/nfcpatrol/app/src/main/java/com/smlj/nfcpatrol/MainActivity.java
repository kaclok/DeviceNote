package com.smlj.nfcpatrol;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

        Button btnNFC = findViewById(R.id.btn_nfc);
        if (btnNFC != null) {
            btnNFC.setOnClickListener(v -> {
                Intent intent = new Intent(this, NFCScanActivity.class);
                nfcLauncher.launch(intent);
            });
        }
    }

    private void handleNfcResult(ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_OK) {
            Intent data = result.getData();
            if (data != null) {
                String rfId = data.getStringExtra("rfId");
                Log.e("|||", "handleNfcResult: " + rfId);

                Toast.makeText(this, "扫描成功", Toast.LENGTH_SHORT).show();
            }
        }
    }
}