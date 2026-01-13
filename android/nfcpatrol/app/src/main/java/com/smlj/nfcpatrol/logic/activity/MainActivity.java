package com.smlj.nfcpatrol.logic.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
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
import com.smlj.nfcpatrol.core.network.Result;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.LineInfo;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.TNFCPatrolDept;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.TNFCPatrolPoint;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.api.NFCPatrolDao;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;

public class MainActivity extends AppCompatActivity implements LineAdapter.OnItemClickListener {
    private ActivityResultLauncher<Intent> nfcLauncher;
    private String selectedDeptId;
    private Call<Result<ArrayList<LineInfo>>> callLines;
    private LineAdapter lineAdapter = new LineAdapter();
    private RecyclerView recyclerView;
    private LineInfo selectedLine;
    private SharedPreferences prefs;

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

        nfcLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::handleNfcResult);

        Spinner spinner = findViewById(R.id.spinner_filter);
        List<TNFCPatrolDept> depts = new ArrayList<>();
        depts.add(new TNFCPatrolDept("1-1", "烧碱一班"));
        depts.add(new TNFCPatrolDept("1-2", "烧碱二班"));
        depts.add(new TNFCPatrolDept("1-3", "烧碱三班"));
        depts.add(new TNFCPatrolDept("1-4", "烧碱四班"));
        depts.add(new TNFCPatrolDept("2-1", "聚氯乙烯一班"));
        depts.add(new TNFCPatrolDept("2-2", "聚氯乙烯二班"));
        depts.add(new TNFCPatrolDept("2-3", "聚氯乙烯三班"));
        depts.add(new TNFCPatrolDept("2-4", "聚氯乙烯四班"));
        depts.add(new TNFCPatrolDept("3-1", "公辅一班"));
        depts.add(new TNFCPatrolDept("3-2", "公辅二班"));
        depts.add(new TNFCPatrolDept("3-3", "公辅三班"));
        depts.add(new TNFCPatrolDept("3-4", "公辅四班"));
        depts.add(new TNFCPatrolDept("4-1", "热动力一班"));
        depts.add(new TNFCPatrolDept("4-2", "热动力二班"));
        depts.add(new TNFCPatrolDept("4-3", "热动力三班"));
        depts.add(new TNFCPatrolDept("4-4", "热动力四班"));

        ArrayAdapter<TNFCPatrolDept> adapter = new ArrayAdapter<>(this, R.layout.spinner_selected_item, depts);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TNFCPatrolDept one = (TNFCPatrolDept) parent.getItemAtPosition(position);
                String key = one.getId();      // ✅ 给接口 / 查询用
                String value = one.getName();  // 仅展示

                Log.d("smlj-NFCPatrol", "onItemSelected: " + key + "  " + value);

                selectedDeptId = key;
                prefs.edit().putString("prefsDeptId", selectedDeptId).apply();
                
                Refresh(key);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        spinner.setAdapter(adapter);

        prefs = getSharedPreferences("smlj-nfcpatrol", MODE_PRIVATE);
        var prefsDeptId = prefs.getString("prefsDeptId", "1-1");
        boolean founded = false;
        for (int i = 0; i < depts.size(); i++) {
            var one = depts.get(i);
            if (one.getId().equals(prefsDeptId)) {
                spinner.setSelection(i);
                founded = true;
                break;
            }
        }
        if (!founded) {
            spinner.setSelection(0);
        }

        recyclerView = findViewById(R.id.rv_line_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        lineAdapter = new LineAdapter();
        lineAdapter.setOnItemClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (selectedDeptId != null) {
            Refresh(selectedDeptId);
        }
    }

    private void Refresh(String deptId) {
        if (callLines != null) {
            // 避免连续点击引起上次回包刷新本次UI
            callLines.cancel();
        }

        callLines = NFCPatrolDao.instance().queryLinesByDept(deptId);
        callLines.enqueue(new ActivitySafeCallback<Result<ArrayList<LineInfo>>>(this) {
            @Override
            protected void onSafeResponse(Activity activity, Call<Result<ArrayList<LineInfo>>> call, Result<ArrayList<LineInfo>> response) {
                if (response.getCode() == 200) {
                    var ls = response.getData();
                    if (ls == null || ls.isEmpty()) {
                        Toast toast = Toast.makeText(activity, "当前班组无巡检计划", Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                    lineAdapter.setList(ls);
                    recyclerView.setAdapter(lineAdapter);
                }
            }

            @Override
            protected void onSafeFailure(Activity activity, Call<Result<ArrayList<LineInfo>>> call, Throwable t) {

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

                Call<Result<PageSerializable<TNFCPatrolPoint>>> callPoints = NFCPatrolDao.instance().queryPoints(rfId);
                callPoints.enqueue(new ActivitySafeCallback<Result<PageSerializable<TNFCPatrolPoint>>>(MainActivity.this) {
                    @Override
                    protected void onSafeResponse(Activity activity, Call<Result<PageSerializable<TNFCPatrolPoint>>> call, Result<PageSerializable<TNFCPatrolPoint>> response) {
                        if (response.getCode() == 200) {
                            var ls = response.getData().getList();
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
                                    startActivity(intent);
                                }
                            }
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