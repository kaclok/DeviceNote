package com.smlj.nfcpatrol.logic.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.webkit.WebView;
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
import com.smlj.nfcpatrol.core.network.PageSerializable;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.LineInfo;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.TNFCPatrolPoint;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.api.NFCPatrolDao;

import java.util.ArrayList;

import retrofit2.Call;

public class MainActivity extends AppCompatActivity implements LineAdapter.OnItemClickListener {
    private ActivityResultLauncher<Intent> nfcLauncher;
    private String selectedDeptId;
    private Call<ArrayList<LineInfo>> callLines;
    private LineAdapter lineAdapter = new LineAdapter();
    private RecyclerView recyclerView;
    private LineInfo selectedLine;

    @SuppressLint("SetTextI18n")
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

        // 获取手机上webview的版本
        // 此API要求Android 7.0（API 24）及以上
        String version = "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            var wv = WebView.getCurrentWebViewPackage();
            if (wv != null) {
                version = wv.versionName;
            }
        } else {
            // 对于Android 7.0以下的设备，WebView是系统组件，版本通常与系统绑定
            version = "System WebView (Pre-N)";
        }
        Log.d("===========", "WebView versionName: " + version);

        nfcLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::handleNfcResult);

        var zzName = getIntent().getStringExtra("zzName");
        var deptName = getIntent().getStringExtra("deptName");
        var person = getIntent().getStringExtra("person");
        selectedDeptId = getIntent().getStringExtra("deptId");

        var btnWebview = findViewById(R.id.btn_webview);
        btnWebview.setOnClickListener(v -> {
            /*String url = "http://117.36.227.42:4177/pages/ai_entry/index.html";
            Intent intent = new Intent(this, WebViewActivity.class);
            intent.putExtra("url", url);
            startActivity(intent);*/
            Refresh(selectedDeptId);
        });

        TextView tv_title = findViewById(R.id.tv_title);
        tv_title.setText(zzName + " / " + deptName + " / " + person);

        recyclerView = findViewById(R.id.rv_line_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        lineAdapter = new LineAdapter();
        lineAdapter.setOnItemClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Refresh(selectedDeptId);
    }

    private void Refresh(String deptId) {
        if (callLines != null) {
            // 避免连续点击引起上次回包刷新本次UI
            callLines.cancel();
        }

        callLines = NFCPatrolDao.instance().queryLinesByDept(deptId);
        callLines.enqueue(new ActivitySafeCallback<ArrayList<LineInfo>>(this) {
            @Override
            protected void onSafeResponse(Activity activity, Call<ArrayList<LineInfo>> call, ArrayList<LineInfo> resp) {
                var ls = resp;
                if (ls == null || ls.isEmpty()) {
                    Toast toast = Toast.makeText(activity, "当前班组无巡检计划", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
                lineAdapter.setList(ls);
                recyclerView.setAdapter(lineAdapter);
            }
        });
    }

    @Override
    public void onBtnStatusClicked(LineInfo line) {
        var pointIds = line.getLine().getPointids();
        if (pointIds == null || pointIds.length == 0) {
            Toast toast = Toast.makeText(this, "当前路线未配置巡检点", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        } else {
            Intent intent = new Intent(this, PointsActivity.class);
            intent.putExtra("line", line);
            startActivity(intent);
        }
    }

    @Override
    public void onBtnScanClicked(LineInfo line) {
        this.selectedLine = line;

        Intent intent = new Intent(this, NFCScanActivity.class);
        nfcLauncher.launch(intent);
    }

    private void handleNfcResult(ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_OK) {
            Intent data = result.getData();
            if (data != null) {
                String rfId = data.getStringExtra("rfId");

                Call<PageSerializable<TNFCPatrolPoint>> callPoints = NFCPatrolDao.instance().queryPoints(rfId);
                callPoints.enqueue(new ActivitySafeCallback<PageSerializable<TNFCPatrolPoint>>(MainActivity.this) {
                    @Override
                    protected void onSafeResponse(Activity activity, Call<PageSerializable<TNFCPatrolPoint>> call, PageSerializable<TNFCPatrolPoint> resp) {
                        var ls = resp.getList();
                        if (ls == null || ls.isEmpty()) {
                            Toast toast = Toast.makeText(activity, "该NFC标签未注册或者不合法", Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        } else {
                            var point = ls.get(0);
                            if (selectedLine != null && point.getLineid() != selectedLine.getLine().getId()) {
                                Toast toast = Toast.makeText(activity, "该NFC标签非选中路线的巡检点", Toast.LENGTH_SHORT);
                                toast.setGravity(Gravity.CENTER, 0, 0);
                                toast.show();
                            } else {
                                Intent intent = new Intent(activity, SubmitActivity.class);
                                intent.putExtra("point", point);
                                intent.putExtra("person", getIntent().getStringExtra("person"));
                                startActivity(intent);
                            }
                        }
                    }
                });
            }
        }
    }
}