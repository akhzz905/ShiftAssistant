package cn.akhzz.shiftassistant.ui.group;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cn.akhzz.shiftassistant.R;
import cn.akhzz.shiftassistant.data.entity.ShiftType;

public class ShiftTypeListAdapter extends RecyclerView.Adapter<ShiftTypeListAdapter.ViewHolder> {

    public interface OnClickListener {
        void onClick(ShiftType type);
    }

    public interface OnLongClickListener {
        void onLongClick(ShiftType type);
    }

    private List<ShiftType> items = new ArrayList<>();
    private final OnClickListener clickListener;
    private final OnLongClickListener longClickListener;

    public ShiftTypeListAdapter(OnClickListener clickListener,
                                OnLongClickListener longClickListener) {
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    public void setData(List<ShiftType> data) {
        this.items = data != null ? data : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<ShiftType> getCurrentList() {
        return items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shift_type, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShiftType type = items.get(position);

        holder.tvName.setText(type.name);
        holder.tvTime.setText(type.getFullTimeDescription());

        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        bg.setColor(type.color != 0 ? type.color : cn.akhzz.shiftassistant.util.ColorUtils.GROUP_COLORS[1]);
        holder.colorDot.setBackground(bg);

        if (type.startDayOffset != 0 || type.endDayOffset != 0) {
            holder.tvCrossDayNote.setVisibility(View.VISIBLE);
            if (type.startDayOffset == -1) {
                holder.tvCrossDayNote.setText(holder.itemView.getContext().getString(R.string.cross_day_note_prev, type.name, type.getStartTimeString(), type.getEndTimeString()));
            } else {
                holder.tvCrossDayNote.setText(holder.itemView.getContext().getString(R.string.cross_day_note_next, type.name, type.getStartTimeString(), type.getEndTimeString()));
            }
        } else {
            holder.tvCrossDayNote.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onClick(type);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) longClickListener.onLongClick(type);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvTime, tvCrossDayNote;
        View colorDot;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvCrossDayNote = itemView.findViewById(R.id.tv_cross_day_note);
            colorDot = itemView.findViewById(R.id.color_dot);
        }
    }
}
