package com.smlj.nfcpatrol.logic.activity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smlj.nfcpatrol.R;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.RecordInfo;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PointAdapter extends RecyclerView.Adapter<PointAdapter.ViewHolder> {
    private List<RecordInfo> list;

    // ① 创建 ViewHolder
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_patrol_point, parent, false);

        return new ViewHolder(view);
    }

    // ② 绑定数据（你问的重点就在这）
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecordInfo recordInfo = list.get(position);

        // 左侧文字
        var point = recordInfo.getPoint();
        holder.tv_num.setText((position + 1) + ": " + point.getPointnum());
        holder.tv_name.setText(point.getPointname());
        holder.tv_addr.setText(point.getPointaddr());

        var r = recordInfo.getRecord();
        if(r != null) {
            holder.tv_status.setText("已完成");
            holder.tv_status.setBackgroundResource(R.drawable.bg_status_done);
        }
        else {
            holder.tv_status.setText("待巡检");
            holder.tv_status.setBackgroundResource(R.drawable.bg_status_pending);
        }

        // item 点击（进入 NFC 扫描）
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(recordInfo);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    // ③ ViewHolder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tv_num;
        TextView tv_name;
        TextView tv_addr;
        TextView tv_status;

        ViewHolder(@NonNull View v) {
            super(v);

            tv_num = v.findViewById(R.id.tv_num);
            tv_name = v.findViewById(R.id.tv_name);
            tv_addr = v.findViewById(R.id.tv_addr);
            tv_status = v.findViewById(R.id.tv_status);
        }
    }

    // ④ 点击回调（推荐）
    public interface OnItemClickListener {
        void onItemClick(RecordInfo point);
    }

    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
}
