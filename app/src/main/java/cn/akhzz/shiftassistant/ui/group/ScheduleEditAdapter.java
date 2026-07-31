package cn.akhzz.shiftassistant.ui.group;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cn.akhzz.shiftassistant.R;
import cn.akhzz.shiftassistant.data.entity.ShiftGroupSchedule;
import cn.akhzz.shiftassistant.data.entity.ShiftType;

public class ScheduleEditAdapter extends RecyclerView.Adapter<ScheduleEditAdapter.ViewHolder> {

    public interface OnItemCountChangeListener {
        void onItemCountChanged(int count);
    }

    private List<ShiftType> shiftTypes;
    private final List<Long> selectedTypeIds; // -1 = rest
    private int todayIndex = 0; // which row is "today"
    private final OnItemCountChangeListener countChangeListener;

    public ScheduleEditAdapter(int initialCycleDays, List<ShiftType> shiftTypes,
                               List<ShiftGroupSchedule> existingSchedules,
                               int todayIndex,
                               OnItemCountChangeListener listener) {
        this.shiftTypes = shiftTypes;
        this.countChangeListener = listener;
        this.selectedTypeIds = new ArrayList<>();

        if (existingSchedules != null && !existingSchedules.isEmpty()) {
            for (ShiftGroupSchedule s : existingSchedules) {
                while (selectedTypeIds.size() <= s.dayIndex) {
                    selectedTypeIds.add(-1L);
                }
                selectedTypeIds.set(s.dayIndex, s.shiftTypeId);
            }
        } else {
            for (int i = 0; i < initialCycleDays; i++) {
                selectedTypeIds.add(-1L);
            }
        }

        this.todayIndex = Math.min(todayIndex, selectedTypeIds.size() - 1);
        if (this.todayIndex < 0) this.todayIndex = 0;
    }

    public void updateShiftTypes(List<ShiftType> newTypes) {
        this.shiftTypes = newTypes;
        notifyDataSetChanged();
    }

    public void addDay() {
        selectedTypeIds.add(-1L);
        notifyDataSetChanged();
        if (countChangeListener != null) {
            countChangeListener.onItemCountChanged(selectedTypeIds.size());
        }
    }

    public void removeDay(int position) {
        if (selectedTypeIds.size() <= 1) return;
        selectedTypeIds.remove(position);
        // Adjust todayIndex if needed
        if (todayIndex >= selectedTypeIds.size()) {
            todayIndex = selectedTypeIds.size() - 1;
        } else if (todayIndex > position) {
            todayIndex--;
        }
        notifyDataSetChanged();
        if (countChangeListener != null) {
            countChangeListener.onItemCountChanged(selectedTypeIds.size());
        }
    }

    /** Returns 0-based index of which day is "today" */
    public int getTodayIndex() {
        return todayIndex;
    }

    public int getCycleDays() {
        return selectedTypeIds.size();
    }

    public List<ShiftGroupSchedule> getSchedules(long groupId) {
        List<ShiftGroupSchedule> result = new ArrayList<>();
        for (int i = 0; i < selectedTypeIds.size(); i++) {
            ShiftGroupSchedule s = new ShiftGroupSchedule();
            s.groupId = groupId;
            s.dayIndex = i;
            s.shiftTypeId = selectedTypeIds.get(i);
            result.add(s);
        }
        return result;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Day label
        holder.tvDayLabel.setText(holder.itemView.getContext()
                .getString(R.string.day_label, position + 1));

        // Radio button for "today"
        holder.rbToday.setOnCheckedChangeListener(null); // prevent loop
        holder.rbToday.setChecked(position == todayIndex);
        holder.rbToday.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) {
                int oldIndex = todayIndex;
                todayIndex = holder.getAdapterPosition();
                if (oldIndex != RecyclerView.NO_POSITION && oldIndex < getItemCount()) {
                    notifyItemChanged(oldIndex); // uncheck the old one
                }
            }
        });

        // Build dropdown options
        List<String> options = new ArrayList<>();
        options.add(holder.itemView.getContext().getString(R.string.rest));
        List<Long> optionIds = new ArrayList<>();
        optionIds.add(-1L);

        for (ShiftType type : shiftTypes) {
            options.add(type.name + " (" + type.getFullTimeDescription() + ")");
            optionIds.add(type.id);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                holder.itemView.getContext(),
                android.R.layout.simple_dropdown_item_1line,
                options);
        holder.spinnerShiftType.setAdapter(adapter);

        // Set current selection
        long currentId = selectedTypeIds.get(position);
        int selectedIndex = 0;
        for (int i = 0; i < optionIds.size(); i++) {
            if (optionIds.get(i) == currentId) {
                selectedIndex = i;
                break;
            }
        }
        holder.spinnerShiftType.setText(options.get(selectedIndex), false);

        // Listen for changes
        holder.spinnerShiftType.setOnItemClickListener((parent, view, idx, id) -> {
            int actualPos = holder.getAdapterPosition();
            if (actualPos != RecyclerView.NO_POSITION) {
                selectedTypeIds.set(actualPos, optionIds.get(idx));
            }
        });

        // Remove button
        holder.btnRemove.setVisibility(selectedTypeIds.size() > 1 ? View.VISIBLE : View.INVISIBLE);
        holder.btnRemove.setOnClickListener(v -> {
            int actualPos = holder.getAdapterPosition();
            if (actualPos != RecyclerView.NO_POSITION) {
                removeDay(actualPos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return selectedTypeIds.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        RadioButton rbToday;
        TextView tvDayLabel;
        AutoCompleteTextView spinnerShiftType;
        View btnRemove;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            rbToday = itemView.findViewById(R.id.rb_today);
            tvDayLabel = itemView.findViewById(R.id.tv_day_label);
            spinnerShiftType = itemView.findViewById(R.id.spinner_shift_type);
            btnRemove = itemView.findViewById(R.id.btn_remove);
        }
    }
}
