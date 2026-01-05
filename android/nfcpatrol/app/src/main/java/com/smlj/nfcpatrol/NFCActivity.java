package com.smlj.nfcpatrol;

import android.app.PendingIntent;
import android.content.Intent;
import android.icu.text.DateFormat;
import android.icu.text.SimpleDateFormat;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Objects;
import java.util.Date;

public class NFCActivity extends AppCompatActivity {
    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_nfcactivity);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        nfcAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());
        pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(), NFCActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        // 首次创建时处理 Intent
        handleIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        //  Enable Foreground Dispatch
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //  Disable Foreground Dispatch
        nfcAdapter.disableForegroundDispatch(this);
    }

    // 每次到栈顶，但是intent参数发生变化
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Objects.equals(intent.getAction(), NfcAdapter.ACTION_TAG_DISCOVERED)) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag == null) {
                return;
            }

            // Display Technologies
            String[] techList = tag.getTechList();
            TextView tvTechnologies = findViewById(R.id.tv_tag_technologies);
            tvTechnologies.setText(getResources().getString(R.string.nfc_tag_technologies, getTechnologies(techList)));

            // Display Tag ID
            byte[] tagIdBytes = tag.getId();
            TextView tvTagId = findViewById(R.id.tv_tag_id);
            tvTagId.setText(getResources().getString(R.string.nfc_tag_id_prefix, byte2HexString(tagIdBytes)));
        }
    }

    /**
     * convert bytes to formatted Hex String，* eg. 01:23:45:9A:AB:CD:EF
     */
    private String byte2HexString(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            result.append(String.format("%02X", bytes[i] & 0xFF));
            if (i < bytes.length - 1) {
                result.append(":");
            }
        }
        return result.toString();
    }

    /**
     * get formatted technologies of the Tag, * eg. NfcA,NfcF
     */
    private String getTechnologies(String[] technologies) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < technologies.length; i++) {
            result.append(technologies[i].substring(17));
            if (i < technologies.length - 1) {
                result.append(",");
            }
        }
        return result.toString();
    }
}