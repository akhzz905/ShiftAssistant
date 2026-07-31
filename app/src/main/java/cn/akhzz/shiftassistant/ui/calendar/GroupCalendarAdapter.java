package cn.akhzz.shiftassistant.ui.calendar;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import cn.akhzz.shiftassistant.R;
import cn.akhzz.shiftassistant.data.entity.ShiftGroup;
import cn.akhzz.shiftassistant.data.entity.ShiftGroupSchedule;
import cn.akhzz.shiftassistant.data.entity.ShiftType;
import cn.akhzz.shiftassistant.util.ColorUtils;
import cn.akhzz.shiftassistant.util.ShiftCalculator;

public class GroupCalendarAdapter extends RecyclerView.Adapter<GroupCalendarAdapter.DayViewHolder> {

    private final List<DayInfo> days = new ArrayList<>();
    private final MonthView.OnDayClickListener clickListener;

    public GroupCalendarAdapter(MonthView.OnDayClickListener listener) {
        this.clickListener = listener;
    }

    public void setData(CalendarViewModel.CalendarData data, int year, int month) {
        days.clear();
        if (data == null || data.groups == null || data.groups.isEmpty()) {
            notifyDataSetChanged();
            return;
        }

        YearMonth yearMonth = YearMonth.of(year, month + 1);
        int daysInMonth = yearMonth.lengthOfMonth();

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = LocalDate.of(year, month + 1, day);
            DayInfo info = new DayInfo();
            info.date = date;
            info.entries = new ArrayList<>();

            for (ShiftGroup group : data.groups) {
                if (group.anchorDate == null) continue;
                List<ShiftGroupSchedule> schedules = data.scheduleMap.get(group.id);
                if (schedules == null) continue;
                ShiftType shift = ShiftCalculator.getShiftForDate(
                        group, date, schedules, data.shiftTypes);
                info.entries.add(new GroupShiftEntry(group, shift));
            }

            // Sort by shift start time (rest days at the end)
            info.entries.sort((a, b) -> {
                int aTime = a.shift != null ? a.shift.getEffectiveStartMinutes() : Integer.MAX_VALUE;
                int bTime = b.shift != null ? b.shift.getEffectiveStartMinutes() : Integer.MAX_VALUE;
                return Integer.compare(aTime, bTime);
            });

            days.add(info);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout layout = new LinearLayout(parent.getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        int pad = dp(parent, 12);
        layout.setPadding(pad, pad / 2, pad, pad / 2);
        return new DayViewHolder(layout);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        DayInfo info = days.get(position);
        LinearLayout container = (LinearLayout) holder.itemView;
        container.removeAllViews();

        // Date header
        TextView dateHeader = new TextView(container.getContext());
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINESE);
        dateHeader.setText(info.date.format(fmt));
        dateHeader.setTextSize(15);
        dateHeader.setTypeface(Typeface.DEFAULT_BOLD);
        dateHeader.setPadding(0, dp(container, 4), 0, dp(container, 6));

        // Highlight today
        if (info.date.equals(LocalDate.now())) {
            dateHeader.setTextColor(getThemeColor(container, com.google.android.material.R.attr.colorPrimary));
        } else {
            dateHeader.setTextColor(getThemeColor(container, com.google.android.material.R.attr.colorOnSurface));
        }
        container.addView(dateHeader);

        // Group shift entries
        for (GroupShiftEntry entry : info.entries) {
            LinearLayout row = new LinearLayout(container.getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, dp(container, 2), 0, dp(container, 2));
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);

            // Color dot
            View dot = new View(container.getContext());
            int dotSize = dp(container, 10);
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dotSize, dotSize);
            dotParams.setMarginEnd(dp(container, 8));
            dot.setLayoutParams(dotParams);
            GradientDrawable dotBg = new GradientDrawable();
            dotBg.setShape(GradientDrawable.OVAL);
            dotBg.setColor(entry.group.color);
            dot.setBackground(dotBg);
            row.addView(dot);

            // Group name
            TextView tvGroup = new TextView(container.getContext());
            tvGroup.setText(entry.group.name);
            tvGroup.setTextSize(14);
            tvGroup.setTextColor(entry.group.color);
            tvGroup.setTypeface(Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    dp(container, 60), ViewGroup.LayoutParams.WRAP_CONTENT);
            tvGroup.setLayoutParams(nameParams);
            row.addView(tvGroup);

            // Shift name + time
            TextView tvShift = new TextView(container.getContext());
            if (entry.shift != null) {
                tvShift.setText(entry.shift.name + "  " + entry.shift.getFullTimeDescription());
            } else {
                tvShift.setText(R.string.rest);
                tvShift.setTextColor(0xFF9E9E9E);
            }
            tvShift.setTextSize(13);
            row.addView(tvShift);

            container.addView(row);
        }

        // Divider
        View divider = new View(container.getContext());
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(getThemeColor(container, com.google.android.material.R.attr.colorOutline) & 0x33FFFFFF);
        container.addView(divider);

        // Click listener
        container.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onDayClick(info.date);
            }
        });
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    private int dp(View view, int dp) {
        return (int) (dp * view.getContext().getResources().getDisplayMetrics().density);
    }

    private int getThemeColor(View view, int attr) {
        TypedValue typedValue = new TypedValue();
        view.getContext().getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        DayViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    static class DayInfo {
        LocalDate date;
        List<GroupShiftEntry> entries;
    }

    static class GroupShiftEntry {
        final ShiftGroup group;
        final ShiftType shift;

        GroupShiftEntry(ShiftGroup group, ShiftType shift) {
            this.group = group;
            this.shift = shift;
        }
    }
}
