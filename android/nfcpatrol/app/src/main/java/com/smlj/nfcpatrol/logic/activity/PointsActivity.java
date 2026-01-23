package com.smlj.nfcpatrol.logic.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smlj.nfcpatrol.R;
import com.smlj.nfcpatrol.core.network.ActivitySafeCallback;
import com.smlj.nfcpatrol.logic.Const;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.LineInfo;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.RecordInfo;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.TNFCPatrolPoint;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.api.NFCPatrolDao;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import retrofit2.Call;

public class PointsActivity extends AppCompatActivity {
    private LineInfo lineInfo;
    private PointAdapter pointAdapter = new PointAdapter();
    private RecyclerView recyclerView;
    private Call<ArrayList<RecordInfo>> call;
    private TNFCPatrolPoint point;

    private ActivityResultLauncher<Intent> nfcLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_points);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            lineInfo = getIntent().getSerializableExtra("line", LineInfo.class);
        } else {
            lineInfo = (LineInfo) getIntent().getSerializableExtra("line");
        }

        var btnWebview = findViewById(R.id.btn_webview);
        btnWebview.setOnClickListener(v -> {
            /*String url = "http://117.36.227.42:4177/pages/ai_entry/index.html";
            Intent intent = new Intent(this, WebViewActivity.class);
            intent.putExtra("url", url);
            startActivity(intent);*/

            refresh();
        });

        var prefs = getSharedPreferences(Const.prefsTag, MODE_PRIVATE);
        var prefsTag_person = prefs.getString(Const.prefsTag_person, "*");
        var prefsTag_zzName = prefs.getString(Const.prefsTag_zzName, "*");
        var prefsTag_deptName = prefs.getString(Const.prefsTag_deptName, "*");

        TextView tv_title = findViewById(R.id.tv_title);
        tv_title.setText(prefsTag_zzName + " / " + prefsTag_deptName + " / " + prefsTag_person);

        // 注册NFC扫描启动器
        // 结果回调
        nfcLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handleNfcResult
        );

        recyclerView = findViewById(R.id.rv_point_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        pointAdapter = new PointAdapter();
        pointAdapter.setOnItemClickListener(pointRecord -> {
            point = pointRecord.getPoint();

            Intent intent = new Intent(this, NFCScanActivity.class);
            nfcLauncher.launch(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        refresh();
    }

    private void refresh() {
        if (call != null) {
            call.cancel();
        }

        var lineId = lineInfo.getLine().getId();
        var sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        call = NFCPatrolDao.instance().queryPointsInfoByLine(lineId, sdf.format(lineInfo.getTime()));
        call.enqueue(new ActivitySafeCallback<ArrayList<RecordInfo>>(this) {
            @Override
            protected void onSafeResponse(Activity activity, Call<ArrayList<RecordInfo>> call, ArrayList<RecordInfo> resp) {
                var ls = resp;
                if (ls == null || ls.isEmpty()) {
                    Toast toast = Toast.makeText(activity, "当前路线未配置巡检点", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
                pointAdapter.setList(ls);
                recyclerView.setAdapter(pointAdapter);
            }
        });
    }

    private void handleNfcResult(ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_OK) {
            Intent data = result.getData();
            if (data != null) {
                String rfId = data.getStringExtra("rfId");

                if (point != null) {
                    if (!point.getRfid().equals(rfId)) {
                        Toast toast = Toast.makeText(this, "请扫描选中巡检点的NFC标签", Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    } else {
                        Intent intent = new Intent(this, SubmitActivity.class);
                        intent.putExtra("point", point);
                        startActivity(intent);
                    }
                }
            }
        }
    }
}