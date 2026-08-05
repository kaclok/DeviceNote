package com.smlj.nfcpatrol.logic.activity;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.gson.Gson;
import com.smlj.nfcpatrol.R;
import com.smlj.nfcpatrol.core.network.ActivitySafeCallback;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.TNFCPatrolPoint;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.TNFCPatrolPosition;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.api.NFCPatrolDao;
import com.smlj.nfcpatrol.logic.widget.RecyclableRowContainer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.val;
import retrofit2.Call;

/**
 * NFC 巡检录入页面 (Java 版)
 * <p>
 * 功能:
 * 1. 勾选/取消勾选巡检项 → 展开/收起位号输入区域，切换状态标签
 * 2. 根据后台配置动态添加位号输入行（行 View 可复用：数量变化时只隐藏/新建，不重复创建）
 * 3. 每个卡片组保存：勾选状态 + tag/tvTag 映射（含输入框），供后续上传手动输入内容
 * 4. EditText 设置 textMultiLine，输入内容多时自动增高 (Android 原生行为)
 */
public class InspectionActivity extends AppCompatActivity {
    private TNFCPatrolPoint point;
    private Call<ArrayList<TNFCPatrolPosition>> call;

    /**
     * 每个巡检项卡片组的状态，key 为卡片标识（vibration/temperature/oilLevel/other）
     */
    private final Map<String, CardState> cardStates = new LinkedHashMap<>();
    private final Map<String, CardTp> cards = new LinkedHashMap<>();

    /**
     * 一个卡片组：1 个勾选组件 + 多个 tag/tvTag 映射（位号输入行）。
     * checked 记录勾选状态；rowContainer 保存 tag → 行/输入框的映射。
     */
    private static class CardState {
        final MaterialCheckBox checkBox;
        final TextView statusTv;
        final LinearLayout container;
        final String dotColorHex;
        final RecyclableRowContainer rowContainer;
        boolean checked;

        CardState(MaterialCheckBox checkBox, TextView statusTv, LinearLayout container, String dotColorHex, RecyclableRowContainer rowContainer) {
            this.checkBox = checkBox;
            this.statusTv = statusTv;
            this.container = container;
            this.dotColorHex = dotColorHex;
            this.rowContainer = rowContainer;
        }
    }

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

        var person = getIntent().getStringExtra("person");
        var deptId = getIntent().getStringExtra("deptId");

        val toolbar = ((MaterialToolbar) findViewById(R.id.toolbar));
        toolbar.setSubtitle(point.getPointname());

        initCards();

        // 提交按钮
        findViewById(R.id.btnSubmit).setOnClickListener(v -> {
            // TODO: 将来上传时，遍历 cardStates：
            //   - cardState.checked 判断该卡片组是否勾选
            //   - cardState.rowContainer.getTagLabelMap() 获取 tag → tvTag 映射
            //   - cardState.rowContainer.collectTagInputs() / getTagInputMap() 获取每个 tag 的手动输入内容

            cards.clear();
            for (var entry : cardStates.entrySet()) {
                var key = entry.getKey();
                var val = entry.getValue();

                Map<String, String> ps = null;
                if (val.checked) { // 勾选
                    ps = new LinkedHashMap<>();
                    for (var kvp : val.rowContainer.getTagInputMap().entrySet()) {
                        var content = kvp.getValue().getText().toString().trim();
                        if (!content.isEmpty()) {
                            ps.put(kvp.getKey(), content);
                        }
                    }
                }

                cards.put(key, new CardTp(val.checked, ps));
            }

            String json = new Gson().toJson(cards);
            NFCPatrolDao.instance().addRecord2(point.getRfid(), person, deptId, json).enqueue(new ActivitySafeCallback<Void>(this) {
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

    /**
     * 初始化四个巡检项卡片组：只创建一次行复用容器和勾选监听器，
     * 之后 refresh 只是往容器里 setData，不再重复 inflate。
     */
    private void initCards() {
        registerCard("zd", R.id.cbVibration, R.id.tvStatusVibration, R.id.expandVibration, "#378ADD");
        registerCard("wd", R.id.cbTemperature, R.id.tvStatusTemperature, R.id.expandTemperature, "#D85A30");
        registerCard("yw", R.id.cbOilLevel, R.id.tvStatusOilLevel, R.id.expandOilLevel, "#EF9F27");
        registerCard("qt", R.id.cbOther, R.id.tvStatusOther, R.id.expandOther, "#5F5E5A");
    }

    private void registerCard(String key, int checkBoxId, int statusId, int containerId, String dotColorHex) {
        MaterialCheckBox checkBox = findViewById(checkBoxId);
        TextView statusTv = findViewById(statusId);
        LinearLayout container = findViewById(containerId);

        CardState state = new CardState(checkBox, statusTv, container, dotColorHex, new RecyclableRowContainer(container, LayoutInflater.from(this)));

        // 勾选联动: 展开/收起 + 状态切换；同时记录勾选状态
        checkBox.setOnCheckedChangeListener(new MaterialCheckBox.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                applyChecked(state, isChecked);
            }
        });

        cardStates.put(key, state);
    }

    /**
     * 根据勾选状态更新卡片组：记录 checked、切换展开/收起和状态标签。
     */
    private void applyChecked(CardState state, boolean isChecked) {
        state.checked = isChecked;
        state.container.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        if (isChecked) {
            state.statusTv.setText(getString(R.string.insp_status_abnormal));
            state.statusTv.setBackgroundResource(R.drawable.bg_status_abnormal);
            state.statusTv.setTextColor(getColor(R.color.insp_red_800));
        } else {
            state.statusTv.setText(getString(R.string.insp_status_normal));
            state.statusTv.setBackgroundResource(R.drawable.bg_status_normal);
            state.statusTv.setTextColor(getColor(R.color.insp_green_800));
        }
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

        String rfid = point.getRfid();
        // rfid = "044AA61A647380";
        call = NFCPatrolDao.instance().queryPositions(rfid);
        call.enqueue(new ActivitySafeCallback<ArrayList<TNFCPatrolPosition>>(this) {
            @Override
            protected void onSafeResponse(Activity activity, Call<ArrayList<TNFCPatrolPosition>> call, ArrayList<TNFCPatrolPosition> resp) {
                ArrayList<TNFCPatrolPosition> ls = resp != null ? resp : new ArrayList<>();

                for (CardState state : cardStates.values()) {
                    // 复用已有行：多于当前数据则隐藏，少于则新建；输入内容按 tag 保留
                    state.rowContainer.setData(ls, state.dotColorHex);

                    // 刷新后按记录的勾选状态恢复展开/收起，避免刷新重置界面
                    state.container.setVisibility(state.checked ? View.VISIBLE : View.GONE);
                }
            }
        });
    }
}
