package com.smlj.nfcpatrol.logic.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.smlj.nfcpatrol.R;
import com.smlj.nfcpatrol.core.utils.ListUtil;
import com.smlj.nfcpatrol.logic.Const;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.TNFCPatrolDept;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LoginActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private ArrayAdapter<TNFCPatrolDept> adapterDept;
    private String selectZZId;
    private String selectZZName;
    private String selectDeptId;
    private String selectDeptName;
    private HashMap<String, ArrayList<TNFCPatrolDept>> map = new HashMap<>();
    private Spinner spinnerDept;

    @SuppressLint("ApplySharedPref")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        prefs = getSharedPreferences(Const.prefsTag, MODE_PRIVATE);

        map = new HashMap<>();
        var depts = new ArrayList<TNFCPatrolDept>();
        depts.add(new TNFCPatrolDept("1-1", "烧碱一班"));
        depts.add(new TNFCPatrolDept("1-2", "烧碱二班"));
        depts.add(new TNFCPatrolDept("1-3", "烧碱三班"));
        depts.add(new TNFCPatrolDept("1-4", "烧碱四班"));
        map.put("1", depts);

        depts = new ArrayList<TNFCPatrolDept>();
        depts.add(new TNFCPatrolDept("2-1", "公辅一班"));
        depts.add(new TNFCPatrolDept("2-2", "公辅二班"));
        depts.add(new TNFCPatrolDept("2-3", "公辅三班"));
        depts.add(new TNFCPatrolDept("2-4", "公辅四班"));
        map.put("2", depts);

        depts = new ArrayList<TNFCPatrolDept>();
        depts.add(new TNFCPatrolDept("3-1", "热动力一班"));
        depts.add(new TNFCPatrolDept("3-2", "热动力二班"));
        depts.add(new TNFCPatrolDept("3-3", "热动力三班"));
        depts.add(new TNFCPatrolDept("3-4", "热动力四班"));
        map.put("3", depts);

        depts = new ArrayList<TNFCPatrolDept>();
        depts.add(new TNFCPatrolDept("4-1", "乙炔一班"));
        depts.add(new TNFCPatrolDept("4-2", "乙炔二班"));
        depts.add(new TNFCPatrolDept("4-3", "乙炔三班"));
        depts.add(new TNFCPatrolDept("4-4", "乙炔四班"));
        map.put("4", depts);

        depts = new ArrayList<TNFCPatrolDept>();
        depts.add(new TNFCPatrolDept("5-1", "VCM一班"));
        depts.add(new TNFCPatrolDept("5-2", "VCM二班"));
        depts.add(new TNFCPatrolDept("5-3", "VCM三班"));
        depts.add(new TNFCPatrolDept("5-4", "VCM四班"));
        map.put("5", depts);

        depts = new ArrayList<TNFCPatrolDept>();
        depts.add(new TNFCPatrolDept("6-1", "聚合一班"));
        depts.add(new TNFCPatrolDept("6-2", "聚合二班"));
        depts.add(new TNFCPatrolDept("6-3", "聚合三班"));
        depts.add(new TNFCPatrolDept("6-4", "聚合四班"));
        map.put("6", depts);

        Spinner spinnerZZ = findViewById(R.id.spinner_zz);
        spinnerDept = findViewById(R.id.spinner_dept);
        List<TNFCPatrolDept> deptsZZ = new ArrayList<>();
        deptsZZ.add(new TNFCPatrolDept("1", "烧碱"));
        deptsZZ.add(new TNFCPatrolDept("2", "公辅"));
        deptsZZ.add(new TNFCPatrolDept("3", "热动力"));
        deptsZZ.add(new TNFCPatrolDept("4", "乙炔"));
        deptsZZ.add(new TNFCPatrolDept("5", "VCM"));
        deptsZZ.add(new TNFCPatrolDept("6", "聚合"));

        ArrayAdapter<TNFCPatrolDept> adapterZZ = new ArrayAdapter<>(this, R.layout.spinner_selected_item, deptsZZ);
        adapterZZ.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerZZ.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TNFCPatrolDept one = (TNFCPatrolDept) parent.getItemAtPosition(position);
                String key = one.getId();      // ✅ 给接口 / 查询用
                String value = one.getName();  // 仅展示

                Log.d(Const.prefsTag, "ZZ onItemSelected: " + key + "  " + value);
                selectZZId = key;
                selectZZName = value;

                var depts = map.get(key);
                adapterDept.clear();
                adapterDept.addAll(depts);
                // 默认选中
                spinnerDept.setSelection(0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        spinnerZZ.setAdapter(adapterZZ);

        adapterDept = new ArrayAdapter<>(this, R.layout.spinner_selected_item);
        adapterDept.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerDept.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TNFCPatrolDept one = (TNFCPatrolDept) parent.getItemAtPosition(position);
                String key = one.getId();      // ✅ 给接口 / 查询用
                String value = one.getName();  // 仅展示

                Log.d(Const.prefsTag, "Dept onItemSelected: " + key + "  " + value);
                selectDeptId = key;
                selectDeptName = value;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        spinnerDept.setAdapter(adapterDept);

        EditText etName = findViewById(R.id.et_name);
        Button btnLogin = findViewById(R.id.btn_login);
        btnLogin.setOnClickListener(v -> {
            var person = etName.getText().toString().trim();
            if (person.isEmpty()) {
                Toast toast = Toast.makeText(this, "姓名没有填写", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                return;
            }

            prefs.edit().putString(Const.prefsTag_zzId, selectZZId).apply();
            prefs.edit().putString(Const.prefsTag_zzName, selectZZName).apply();

            prefs.edit().putString(Const.prefsTag_deptId, selectDeptId).apply();
            prefs.edit().putString(Const.prefsTag_deptName, selectDeptName).apply();

            prefs.edit().putString(Const.prefsTag_person, person).apply();

            Intent intent = new Intent(this, MainActivity.class);
            Log.d(Const.prefsTag, "selectZZId:" + selectZZId + " selectZZName:" + selectZZName + " selectDeptId:" + selectDeptId + " selectDeptName:" + selectDeptName);
            intent.putExtra("zzId", selectZZId);
            intent.putExtra("zzName", selectZZName);
            intent.putExtra("deptId", selectDeptId);
            intent.putExtra("deptName", selectDeptName);
            intent.putExtra("person", person);
            startActivity(intent);
        });

        var prefsTag_person = prefs.getString(Const.prefsTag_person, "");
        etName.setText(prefsTag_person);

        var prefsTag_zzId = prefs.getString(Const.prefsTag_zzId, "1");
        int idx = ListUtil.finalIdx(prefsTag_zzId, deptsZZ, (aimId, ele) -> ele.getId().equals(aimId));
        if (idx != -1) {
            spinnerZZ.setSelection(idx);
        }
        var prefsTag_deptId = prefs.getString(Const.prefsTag_deptId, "1-1");
        idx = ListUtil.finalIdx(prefsTag_deptId, map.get(prefsTag_zzId), (aimId, ele) -> ele.getId().equals(aimId));
        if (idx != -1) {
            spinnerDept.setSelection(idx);
        }
    }
}