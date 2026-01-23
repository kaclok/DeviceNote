package com.smlj.nfcpatrol.logic.activity;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.smlj.nfcpatrol.R;
import com.smlj.nfcpatrol.core.network.ActivitySafeCallback;
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

        var person = getIntent().getStringExtra("person");

        ((TextView) findViewById(R.id.tv_title)).setText("巡检点：" + point.getPointname());
        findViewById(R.id.btn_submit).setOnClickListener(v -> {
            CheckBox rbValid = findViewById(R.id.rb_valid);
            boolean invalid = rbValid.isChecked();
            var et_detail = ((TextView) findViewById(R.id.et_detail)).getText().toString().trim();

            if (invalid && et_detail.isEmpty()) {
                Toast toast = Toast.makeText(SubmitActivity.this, "有异常必须填写详情", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                return;
            }

            // 异常数量：如果设备正常则为0，否则如果用户未填则默认为1（因为已经标记异常了），或者根据需求设定
            // 这里假设异常状态下至少记1次异常，或者您有其他逻辑
            // 简单起见，如果设备正常 cnt=0，异常且详情不为空 cnt=1 (或者您可以添加专门的异常数量输入框，如果之前的 et_count 还在的话)
            // 根据之前的代码，似乎 et_count 被移除了？如果移除了，这里我们可以根据状态设置 cnt
            int cnt = invalid ? 1 : 0;
            NFCPatrolDao.instance().addRecord(point.getRfid(), person, et_detail, cnt).enqueue(new ActivitySafeCallback<Void>(this) {
                @Override
                protected void onSafeResponse(Activity activity, Call<Void> call, Void resp) {
                    Toast toast = Toast.makeText(activity, "提交成功", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    finish();
                }
            });
        });
    }
}