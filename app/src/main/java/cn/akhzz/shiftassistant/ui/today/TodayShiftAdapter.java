package cn.akhzz.shiftassistant.ui.today;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cn.akhzz.shiftassistant.R;

public class TodayShiftAdapter extends RecyclerView.Adapter<TodayShiftAdapter.ViewHolder> {

    private List<TodayViewModel.GroupShiftInfo> items = new ArrayList<>();

    public void setData(List<TodayViewModel.GroupShiftInfo> data) {
        this.items = data != null ? data : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_today_shift, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TodayViewModel.GroupShiftInfo info = items.get(position);

        // Color dot
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(info.group.color);
        holder.colorDot.setBackground(dotBg);

        // Group name
        holder.tvGroupName.setText(info.group.name);

        // Shift info
        if (info.shift != null) {
            holder.tvShiftInfo.setText(info.shift.name + "  " + info.shift.getFullTimeDescription());
        } else {
            holder.tvShiftInfo.setText(R.string.rest);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View colorDot;
        TextView tvGroupName, tvShiftInfo;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            colorDot = itemView.findViewById(R.id.color_dot);
            tvGroupName = itemView.findViewById(R.id.tv_group_name);
            tvShiftInfo = itemView.findViewById(R.id.tv_shift_info);
        }
    }
}
