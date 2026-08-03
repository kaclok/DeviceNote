package com.smlj.nfcpatrol.logic.activity;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.smlj.nfcpatrol.R;
import com.smlj.nfcpatrol.core.network.ActivitySafeCallback;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.TNFCPatrolPoint;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.TNFCPatrolPosition;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.api.NFCPatrolDao;

import java.util.ArrayList;

import lombok.val;
import retrofit2.Call;

/**
 * NFC 巡检录入页面 (Java 版)
 * <p>
 * 功能:
 * 1. 勾选/取消勾选巡检项 -> 展开/收起位号输入区域, 切换状态标签
 * 2. 根据后台配置动态添加位号输入行
 * 3. EditText 设置 textMultiLine, 输入内容多时自动增高 (Android 原生行为)
 */
public class InspectionActivity extends AppCompatActivity {
    private TNFCPatrolPoint point;
    private Call<ArrayList<TNFCPatrolPosition>> call;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inspection);

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

        // getIntent().getStringExtra("deptName");
        val toolbar = ((MaterialToolbar) findViewById(R.id.toolbar));
        toolbar.setSubtitle(point.getPointname() +  " " + point.getPointnum());

        // 提交按钮
        findViewById(R.id.btnSubmit).setOnClickListener(v -> {

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

        String rfid = "0463A51A647380";
        call = NFCPatrolDao.instance().queryPositions(rfid);
        call.enqueue(new ActivitySafeCallback<ArrayList<TNFCPatrolPosition>>(this) {
            @Override
            protected void onSafeResponse(Activity activity, Call<ArrayList<TNFCPatrolPosition>> call, ArrayList<TNFCPatrolPosition> resp) {
                var ls = resp;
                if (ls == null || ls.isEmpty()) {
                    return;
                }

                // 震动情况 — 3 个位号
                setupCard(
                        R.id.cbVibration,
                        R.id.tvStatusVibration,
                        R.id.expandVibration,
                        "#378ADD",
                        ls
                );

                // 温度情况 — 2 个位号
                setupCard(
                        R.id.cbTemperature,
                        R.id.tvStatusTemperature,
                        R.id.expandTemperature,
                        "#D85A30",
                        ls
                );

                // 油位情况 — 2 个位号
                setupCard(
                        R.id.cbOilLevel,
                        R.id.tvStatusOilLevel,
                        R.id.expandOilLevel,
                        "#EF9F27",
                        ls
                );

                // 其他情况 — 1 个位号
                setupCard(
                        R.id.cbOther,
                        R.id.tvStatusOther,
                        R.id.expandOther,
                        "#5F5E5A",
                        ls
                );
            }
        });
    }

    /**
     * 初始化一个巡检项卡片
     *
     * @param checkBoxId  勾选框 ID
     * @param statusId    状态标签 ID
     * @param containerId 展开区域容器 ID (LinearLayout)
     * @param dotColorHex 位号圆点颜色 (hex 字符串, 如 "#378ADD")
     * @param tags        该巡检点配置的位号列表
     */
    private void setupCard(int checkBoxId, int statusId, int containerId,
                           String dotColorHex, ArrayList<TNFCPatrolPosition> tags) {

        MaterialCheckBox checkBox = findViewById(checkBoxId);
        TextView statusTv = findViewById(statusId);
        LinearLayout container = findViewById(containerId);

        // 动态添加位号输入行
        int dotColor = Color.parseColor(dotColorHex);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (var tag : tags) {
            View rowView = inflater.inflate(R.layout.item_tag_input_row, container, false);

            TextView tvTag = rowView.findViewById(R.id.tvTag);
            tvTag.setText(tag.getPos());

            View viewDot = rowView.findViewById(R.id.viewDot);
            viewDot.setBackgroundTintList(ColorStateList.valueOf(dotColor));

            container.addView(rowView);
        }

        // 勾选框联动: 展开/收起 + 状态切换
        checkBox.setOnCheckedChangeListener(new MaterialCheckBox.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    container.setVisibility(View.VISIBLE);
                    statusTv.setText(getString(R.string.insp_status_abnormal));
                    statusTv.setBackgroundResource(R.drawable.bg_status_abnormal);
                    statusTv.setTextColor(getColor(R.color.insp_red_800));
                } else {
                    container.setVisibility(View.GONE);
                    statusTv.setText(getString(R.string.insp_status_normal));
                    statusTv.setBackgroundResource(R.drawable.bg_status_normal);
                    statusTv.setTextColor(getColor(R.color.insp_green_800));
                }
            }
        });
    }
}
