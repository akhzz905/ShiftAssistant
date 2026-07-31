package cn.akhzz.shiftassistant.ui.group;

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
import cn.akhzz.shiftassistant.data.entity.ShiftGroup;

public class GroupListAdapter extends RecyclerView.Adapter<GroupListAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onClick(ShiftGroup group);
    }

    public interface OnStarClickListener {
        void onStarClick(ShiftGroup group);
    }

    public interface OnLongClickListener {
        void onLongClick(ShiftGroup group);
    }

    private List<ShiftGroup> items = new ArrayList<>();
    private final OnItemClickListener clickListener;
    private final OnStarClickListener starListener;
    private final OnLongClickListener longClickListener;

    public GroupListAdapter(OnItemClickListener clickListener,
                            OnStarClickListener starListener,
                            OnLongClickListener longClickListener) {
        this.clickListener = clickListener;
        this.starListener = starListener;
        this.longClickListener = longClickListener;
    }

    public void setData(List<ShiftGroup> data) {
        this.items = data != null ? data : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShiftGroup group = items.get(position);

        // Color circle
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(group.color);
        holder.colorCircle.setBackground(bg);

        // Name
        holder.tvName.setText(group.name);

        // Cycle info
        holder.tvCycle.setText("循环 " + group.cycleDays + " 天");

        // Default indicator
        holder.tvDefault.setVisibility(group.isDefault ? View.VISIBLE : View.GONE);

        // Star button
        holder.btnStar.setText(group.isDefault ? "★" : "☆");
        holder.btnStar.setTextColor(group.isDefault ? 0xFFFFC107 : 0xFF9E9E9E);
        holder.btnStar.setOnClickListener(v -> {
            if (starListener != null) starListener.onStarClick(group);
        });

        // Click
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onClick(group);
        });

        // Long click
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) longClickListener.onLongClick(group);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View colorCircle;
        TextView tvName, tvCycle, tvDefault, btnStar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            colorCircle = itemView.findViewById(R.id.color_circle);
            tvName = itemView.findViewById(R.id.tv_name);
            tvCycle = itemView.findViewById(R.id.tv_cycle);
            tvDefault = itemView.findViewById(R.id.tv_default);
            btnStar = itemView.findViewById(R.id.btn_star);
        }
    }
}
