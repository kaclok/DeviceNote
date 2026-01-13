package com.smlj.nfcpatrol.logic.activity;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.smlj.nfcpatrol.R;
import com.smlj.nfcpatrol.core.network.ActivitySafeCallback;
import com.smlj.nfcpatrol.core.network.Result;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.TNFCPatrolPoint;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.api.NFCPatrolDao;

import retrofit2.Call;

public class SubmitActivity extends AppCompatActivity {
    private TNFCPatrolPoint point;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_submit);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            point = getIntent().getSerializableExtra("point", TNFCPatrolPoint.class);
        } else {
            point = (TNFCPatrolPoint) getIntent().getSerializableExtra("point");
        }

        ((TextView) findViewById(R.id.tv_title)).setText("巡检点：" + point.getPointname());
        findViewById(R.id.btn_submit).setOnClickListener(v -> {
            var et_name = ((TextView) findViewById(R.id.et_name)).getText().toString().trim();
            var et_count = ((TextView) findViewById(R.id.et_count)).getText().toString().trim();
            var et_detail = ((TextView) findViewById(R.id.et_detail)).getText().toString().trim();
            var cnt = Integer.parseInt(et_count);

            boolean isValid = true;
            String tip = null;
            if (et_name.isEmpty()) {
                isValid = false;
                tip = "请输入巡检人员姓名";
            }

            if (cnt != 0 && et_detail.isEmpty()) {
                isValid = false;
                tip = "巡检点有异常必须填写详情";
            }

            if (!isValid) {
                Toast toast = Toast.makeText(SubmitActivity.this, tip, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                return;
            }

            NFCPatrolDao.instance().addRecord(point.getRfid(), et_name, et_detail, cnt).enqueue(new ActivitySafeCallback<Result<Void>>(this) {
                @Override
                protected void onSafeResponse(Activity activity, Call<Result<Void>> call, Result<Void> response) {
                    String tip = "提交失败";
                    if (response.getCode() == 200) {
                        tip = "提交成功";
                        finish();
                    }

                    Toast toast = Toast.makeText(activity, tip, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }

                @Override
                protected void onSafeFailure(Activity activity, Call<Result<Void>> call, Throwable t) {

                }
            });
        });
    }
}