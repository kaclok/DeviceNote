package com.smlj.nfcpatrol.logic.activity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smlj.nfcpatrol.R;
import com.smlj.nfcpatrol.logic.network.NFCPatrol.LineInfo;

import java.text.SimpleDateFormat;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LineAdapter extends RecyclerView.Adapter<LineAdapter.ViewHolder> {
    private List<LineInfo> list;

    // ① 创建 ViewHolder
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_patrol_line, parent, false);

        return new ViewHolder(view);
    }

    // ② 绑定数据（你问的重点就在这）
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LineInfo line = list.get(position);

        // 左侧文字
        holder.tv_title.setText(line.getLine().getLinename());

        var sdf = new SimpleDateFormat("yyyy-MM-dd HH");
        holder.tv_time.setText(sdf.format(line.getTime()) + "时");

        var finishCnt = line.getFinishCnt();
        var pointIds = line.getLine().getPointids();
        var totalCnt = pointIds == null ? 0 : pointIds.length;
        String status = "已完成";
        int resid = R.drawable.bg_status_done;
        if (finishCnt == 0) {
            status = "待巡检";
            resid = R.drawable.bg_status_pending;
        } else if (finishCnt < totalCnt) {
            status = "巡检中";
            resid = R.drawable.bg_status_pending;
        }
        holder.tv_status.setText(status);
        holder.tv_status.setBackgroundResource(resid);

        // item 点击（进入 NFC 扫描）
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(line);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    // ③ ViewHolder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tv_title;
        TextView tv_time;
        TextView tv_status;

        ViewHolder(@NonNull View v) {
            super(v);

            tv_title = v.findViewById(R.id.tv_title);
            tv_time = v.findViewById(R.id.tv_time);
            tv_status = v.findViewById(R.id.tv_status);
        }
    }

    // ④ 点击回调（推荐）
    public interface OnItemClickListener {
        void onItemClick(LineInfo point);
    }

    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
}
