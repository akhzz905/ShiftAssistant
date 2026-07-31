package cn.akhzz.shiftassistant.ui.calendar;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.akhzz.shiftassistant.data.entity.ShiftGroup;
import cn.akhzz.shiftassistant.data.entity.ShiftGroupSchedule;
import cn.akhzz.shiftassistant.data.entity.ShiftType;
import cn.akhzz.shiftassistant.util.ShiftCalculator;

public class MonthPagerAdapter extends RecyclerView.Adapter<MonthPagerAdapter.MonthViewHolder> {

    public static final int TOTAL_MONTHS = 12000; // ~1000 years
    public static final int CENTER_POSITION = TOTAL_MONTHS / 2;

    private final int centerYear, centerMonth;
    private CalendarViewModel.CalendarData data;
    private final MonthView.OnDayClickListener dayClickListener;

    public MonthPagerAdapter(MonthView.OnDayClickListener listener) {
        this.dayClickListener = listener;
        Calendar cal = Calendar.getInstance();
        centerYear = cal.get(Calendar.YEAR);
        centerMonth = cal.get(Calendar.MONTH);
    }

    /**
     * Convert adapter position to year and month (0-based).
     */
    public int[] getYearMonth(int position) {
        int offset = position - CENTER_POSITION;
        int totalMonths = centerYear * 12 + centerMonth + offset;
        int year = totalMonths / 12;
        int month = totalMonths % 12;
        if (month < 0) {
            month += 12;
            year -= 1;
        }
        return new int[]{year, month};
    }

    public void setData(CalendarViewModel.CalendarData data) {
        this.data = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MonthViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        MonthView monthView = new MonthView(parent.getContext());
        monthView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        return new MonthViewHolder(monthView);
    }

    @Override
    public void onBindViewHolder(@NonNull MonthViewHolder holder, int position) {
        int[] ym = getYearMonth(position);
        holder.monthView.setYearMonth(ym[0], ym[1]);
        holder.monthView.setOnDayClickListener(dayClickListener);

        // Compute shift data for this month based on default group
        if (data != null && data.defaultGroup != null && data.defaultGroup.anchorDate != null) {
            Map<Integer, MonthView.DayShiftInfo> dayShifts = computeMonthShifts(
                    ym[0], ym[1], data.defaultGroup, data);
            holder.monthView.setDayShifts(dayShifts);
        } else {
            holder.monthView.setDayShifts(null);
        }
    }

    private Map<Integer, MonthView.DayShiftInfo> computeMonthShifts(
            int year, int month, ShiftGroup group, CalendarViewModel.CalendarData data) {
        Map<Integer, MonthView.DayShiftInfo> result = new HashMap<>();
        YearMonth yearMonth = YearMonth.of(year, month + 1);
        int daysInMonth = yearMonth.lengthOfMonth();

        List<ShiftGroupSchedule> schedules = data.scheduleMap.get(group.id);
        if (schedules == null) return result;

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = LocalDate.of(year, month + 1, day);
            ShiftType shift = ShiftCalculator.getShiftForDate(
                    group, date, schedules, data.shiftTypes);
            if (shift != null) {
                result.put(day, new MonthView.DayShiftInfo(shift.name, group.color));
            }
        }
        return result;
    }

    @Override
    public int getItemCount() {
        return TOTAL_MONTHS;
    }

    static class MonthViewHolder extends RecyclerView.ViewHolder {
        final MonthView monthView;

        MonthViewHolder(MonthView view) {
            super(view);
            this.monthView = view;
        }
    }
}
