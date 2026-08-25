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
import java.util.Calendar;
import java.util.Date;

import retrofit2.Call;

public class PointsActivity extends AppCompatActivity {
    private LineInfo lineInfo;
    private PointAdapter pointAdapter = new PointAdapter();
    private RecyclerView recyclerView;
    private Call<ArrayList<RecordInfo>> call;
    private Call<Void> batchCall;
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

        var btnBatch = findViewById(R.id.btn_batch_checkin);
        btnBatch.setOnClickListener(v -> batchCheckIn());

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
    protected void onResume() {
        super.onResume();

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

    private void batchCheckIn() {
        var list = pointAdapter.getList();
        if (list == null || list.isEmpty()) {
            Toast toast = Toast.makeText(this, "当前路线没有可打卡的巡检点", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }

        // 收集当前路线所有巡检点的 rfid（跳过空或 rfid 缺失的）
        var rfids = new ArrayList<String>();
        for (var info : list) {
            var p = info.getPoint();
            if (p != null && p.getRfid() != null && !p.getRfid().isEmpty()) {
                rfids.add(p.getRfid());
            }
        }
        if (rfids.isEmpty()) {
            Toast toast = Toast.makeText(this, "巡检点缺少 rfid", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }

        // 参数：person / deptId（与 InspectionActivity 来源一致：Intent extras）
        var person = getIntent().getStringExtra("person");
        var deptId = getIntent().getStringExtra("deptId");
        if (person == null || person.isEmpty()) {
            Toast toast = Toast.makeText(this, "缺少巡检人信息", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }
        if (deptId == null || deptId.isEmpty()) {
            Toast toast = Toast.makeText(this, "缺少部门信息", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }

        // 时间范围：queryBegin = lineInfo.getTime() 所在轮班起始，queryEnd = 当前时间
        var sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date begin = lineInfo.getTime();
        if (begin == null) {
            begin = new Date();
        }
        Date end = new Date();
        // 如果 cycle 有值，避免生成超过路线周期的日期，保证在周期范围内随机（最小 1 分钟）
        var cycle = lineInfo.getLine().getCycle();
        if (cycle > 0) {
            var cal = Calendar.getInstance();
            cal.setTime(begin);
            cal.add(Calendar.HOUR_OF_DAY, (int) cycle);
            var cycleEnd = cal.getTime();
            if (cycleEnd.before(end)) end = cycleEnd;
            if (end.getTime() - begin.getTime() < 60 * 1000L) {
                // begin 到 end 至少间隔 1 分钟，避免后端 randomDateBetween 退化为同一毫秒
                end = new Date(begin.getTime() + 60 * 1000L);
            }
        }
        final String queryBegin = sdf.format(begin);
        final String queryEnd = sdf.format(end);
        final int total = rfids.size();

        if (batchCall != null) {
            batchCall.cancel();
        }
        batchCall = NFCPatrolDao.instance().addRecord3(rfids, person, deptId, queryBegin, queryEnd);
        batchCall.enqueue(new ActivitySafeCallback<Void>(this) {
            @Override
            protected void onSafeResponse(Activity activity, Call<Void> call, Void resp) {
                Toast toast = Toast.makeText(activity, "批量打卡完成，共 " + total + " 个巡检点", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                refresh();
            }

            @Override
            protected void onSafeFailure(Activity activity, Call<Void> call, Throwable t) {
                String msg = t == null ? "批量打卡失败" : "批量打卡失败：" + t.getMessage();
                Toast toast = Toast.makeText(activity, msg, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        });
    }

    private boolean istest = false;

    private void handleNfcResult(ActivityResult result) {
        if (!istest) {
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
                            Intent intent = new Intent(this, InspectionActivity.class);
                            intent.putExtra("point", point);
                            intent.putExtra("person", getIntent().getStringExtra("person"));
                            intent.putExtra("deptId", getIntent().getStringExtra("deptId"));
                            intent.putExtra("deptName", getIntent().getStringExtra("deptName"));
                            startActivity(intent);
                        }
                    }
                }
            }
        } else {
            Intent intent = new Intent(this, InspectionActivity.class);
            intent.putExtra("point", point);
            intent.putExtra("person", getIntent().getStringExtra("person"));
            intent.putExtra("deptId", getIntent().getStringExtra("deptId"));
            intent.putExtra("deptName", getIntent().getStringExtra("deptName"));
            startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (call != null) {
            call.cancel();
        }
        if (batchCall != null) {
            batchCall.cancel();
        }
    }
}