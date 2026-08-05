package com.smlj.nfcpatrol.logic.widget;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.smlj.nfcpatrol.R;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.TNFCPatrolPosition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Data;

/**
 * 可复用的位号输入行容器。
 * <p>
 * 内部按 tag(pos) 保存已 inflate 的行 View，数据变化时：
 * <ul>
 *   <li>数据量 &gt;= 已有行数 → 复用已有行并更新数据，不足部分新建</li>
 *   <li>数据量 &lt;  已有行数 → 复用前 N 行，多余行设为 GONE 隐藏</li>
 *   <li>隐藏的行不会销毁，同一 tag 再次出现时直接复用，并保留用户已输入内容</li>
 * </ul>
 * 池只增不减，避免反复 inflate / removeView 带来的性能开销。
 * <p>
 * 使用示例：
 * <pre>
 *   RecyclableRowContainer container = new RecyclableRowContainer(linearLayout, inflater);
 *   container.setData(tags, "#378ADD");   // 第一次：5 个 → 创建 5 行
 *   container.setData(tags2, "#378ADD");  // 第二次：4 个 → 复用 4 行，隐藏 1 行
 *   container.setData(tags3, "#378ADD");  // 第三次：6 个 → 复用/恢复已有行，只新建不足的 1 行
 * </pre>
 */
@Data
public class RecyclableRowContainer {
    private final LinearLayout container;
    private final LayoutInflater inflater;

    /**
     * tag(pos) → 行 View，包含当前隐藏的历史行，同一 tag 再次出现时可复用并保留输入
     */
    private final Map<String, View> tagRows = new LinkedHashMap<>();

    public RecyclableRowContainer(LinearLayout container, LayoutInflater inflater) {
        this.container = container;
        this.inflater = inflater;
    }

    /**
     * 用新数据刷新行。复用已有行，不足时新建，多余时隐藏。
     *
     * @param tags        位号列表（可为 null，视为空）
     * @param dotColorHex 圆点颜色 hex 字符串，如 "#378ADD"
     */
    public void setData(List<TNFCPatrolPosition> tags, String dotColorHex) {
        if (tags == null) {
            tags = Collections.emptyList();
        }

        int dotColor = Color.parseColor(dotColorHex);

        Set<String> newTagKeys = new LinkedHashSet<>();
        for (TNFCPatrolPosition tag : tags) {
            newTagKeys.add(keyOf(tag));
        }

        // 1. 本次数据中已不存在的 tag → 隐藏对应行（不销毁、不清空输入，便于之后复用）
        for (Map.Entry<String, View> entry : tagRows.entrySet()) {
            if (!newTagKeys.contains(entry.getKey())) {
                entry.getValue().setVisibility(View.GONE);
            }
        }

        // 2. 按新数据顺序绑定/新建/恢复行
        int index = 0;
        for (TNFCPatrolPosition tag : tags) {
            String key = keyOf(tag);
            View row = tagRows.get(key);
            if (row == null) {
                row = inflater.inflate(R.layout.item_tag_input_row, container, false);
                container.addView(row);
                tagRows.put(key, row);
            }

            bindRow(row, tag, dotColor);
            if (row.getVisibility() != View.VISIBLE) {
                row.setVisibility(View.VISIBLE);
            }

            // 数据顺序可能变化，将行移动到对应位置
            if (container.indexOfChild(row) != index) {
                container.removeView(row);
                container.addView(row, index);
            }
            index++;
        }
    }

    /**
     * @return 当前可见行的 tag → 行 View 映射
     */
    public Map<String, View> getTagRowMap() {
        Map<String, View> result = new LinkedHashMap<>();
        for (Map.Entry<String, View> entry : tagRows.entrySet()) {
            if (entry.getValue().getVisibility() == View.VISIBLE) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * @return 当前可见行的 tag → tvTag(TextView) 映射
     */
    public Map<String, TextView> getTagLabelMap() {
        Map<String, TextView> result = new LinkedHashMap<>();
        for (Map.Entry<String, View> entry : tagRows.entrySet()) {
            if (entry.getValue().getVisibility() == View.VISIBLE) {
                result.put(entry.getKey(), entry.getValue().findViewById(R.id.tvTag));
            }
        }
        return result;
    }

    /**
     * @return 当前可见行的 tag → 输入框(EditText) 映射，供后续上传每个 tag 的手动输入内容
     */
    public Map<String, EditText> getTagInputMap() {
        Map<String, EditText> result = new LinkedHashMap<>();
        for (Map.Entry<String, View> entry : tagRows.entrySet()) {
            if (entry.getValue().getVisibility() == View.VISIBLE) {
                result.put(entry.getKey(), entry.getValue().findViewById(R.id.etInput));
            }
        }
        return result;
    }

    /**
     * 便捷方法：tag → 当前输入内容（trim 后）。
     * 隐藏行不参与，空内容仍保留 key，方便上传时区分“已查看但未填写”。
     */
    public Map<String, String> collectTagInputs() {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, View> entry : tagRows.entrySet()) {
            if (entry.getValue().getVisibility() == View.VISIBLE) {
                EditText input = entry.getValue().findViewById(R.id.etInput);
                CharSequence text = input.getText();
                result.put(entry.getKey(), text == null ? "" : text.toString().trim());
            }
        }
        return result;
    }

    /**
     * @return 当前池中的总行数（包括隐藏的）
     */
    public int getRowCount() {
        return tagRows.size();
    }

    /**
     * @return 当前可见行数
     */
    public int getVisibleRowCount() {
        int count = 0;
        for (View row : tagRows.values()) {
            if (row.getVisibility() == View.VISIBLE) {
                count++;
            }
        }
        return count;
    }

    /**
     * 将数据绑定到单行 View。
     * 复用同一 tag 的行时不会清空输入框，用户已输入内容可随刷新保留。
     */
    private void bindRow(View row, TNFCPatrolPosition tag, int dotColor) {
        TextView tvTag = row.findViewById(R.id.tvTag);
        tvTag.setText(tag.getPos());

        View viewDot = row.findViewById(R.id.viewDot);
        viewDot.setBackgroundTintList(ColorStateList.valueOf(dotColor));
    }

    private static String keyOf(TNFCPatrolPosition tag) {
        return tag == null ? "" : tag.getPos();
    }
}
