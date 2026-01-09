package com.smlj.nfcpatrol.logic.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smlj.nfcpatrol.R;
import com.smlj.nfcpatrol.core.network.ActivitySafeCallback;
import com.smlj.nfcpatrol.core.network.Result;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.LineInfo;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.TNFCPatrolDept;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.api.NFCPatrolDao;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;

public class MainActivity extends AppCompatActivity {
    private String selectedDeptId;
    private Call<Result<ArrayList<LineInfo>>> call;
    private LineAdapter lineAdapter = new LineAdapter();
    private RecyclerView recyclerView;

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
                Refresh(key);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spinner.setAdapter(adapter);

        recyclerView = findViewById(R.id.rv_line_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        lineAdapter = new LineAdapter();
        lineAdapter.setOnItemClickListener(line -> {
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
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (selectedDeptId != null) {
            Refresh(selectedDeptId);
        }
    }

    private void Refresh(String deptId) {
        if (call != null) {
            // 避免连续点击引起上次回包刷新本次UI
            call.cancel();
        }

        call = NFCPatrolDao.instance().queryLinesByDept(deptId);
        call.enqueue(new ActivitySafeCallback<Result<ArrayList<LineInfo>>>(this) {
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
}