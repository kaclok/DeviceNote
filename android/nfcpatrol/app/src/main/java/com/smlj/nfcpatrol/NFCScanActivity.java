package com.smlj.nfcpatrol;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.smlj.nfcpatrol.utils.NFCUtil;

public class NFCScanActivity extends AppCompatActivity {
    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_nfcscan);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        // 创建PendingIntent，当检测到NFC标签时发送给NFCScanActivity的Activity，并且让NFCScanActivity为FLAG_ACTIVITY_SINGLE_TOP模式
        // 1. 创建Intent：当NFC事件发生时，要启动哪个Activity
        var intent = new Intent(getApplicationContext(), NFCScanActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // Android 12（API 31）及以上必须指定可变性,即PendingIntent.FLAG_IMMUTABLE或者PendingIntent.FLAG_MUTABLE必须有
        // ⚠️ NFC需要FLAG_MUTABLE，因为系统要修改Intent
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }
        // 2. 创建PendingIntent：封装这个Intent，交给系统保管
        pendingIntent = PendingIntent.getActivity(this,
                0,
                intent,
                flags);

        Button btnCancel = findViewById(R.id.cancel_button);
        btnCancel.setOnClickListener(v-> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 将PendingIntent注册到NFC系统，当发生nfc被监测事件机会执行PendingIntent
        nfcAdapter.enableForegroundDispatch(this,
                pendingIntent,  // NFC事件发生时执行的Intent
                null, // 监听哪些NFC事件
                null); // 监听哪些NFC技术
    }

    @Override
    protected void onPause() {
        super.onPause();
        //  Disable Foreground Dispatch
        nfcAdapter.disableForegroundDispatch(this);
    }

    // 每次到栈顶，但是intent参数发生变化
    // 4. 当NFC标签被检测到时，系统会调用这里
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag == null) {
                return;
            }

            Intent resultIntent = new Intent();
            resultIntent.putExtra("rfId", NFCUtil.ByteArrayToHexString(tag.getId()));
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        }
    }
}