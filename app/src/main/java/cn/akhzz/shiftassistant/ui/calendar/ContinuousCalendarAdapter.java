package cn.akhzz.shiftassistant.ui.calendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import cn.akhzz.shiftassistant.R;
import cn.akhzz.shiftassistant.data.entity.ShiftGroup;
import cn.akhzz.shiftassistant.data.entity.ShiftGroupSchedule;
import cn.akhzz.shiftassistant.data.entity.ShiftType;
import cn.akhzz.shiftassistant.util.ShiftCalculator;

/**
 * Continuous calendar adapter where each item is one week row (7 days).
 * Months flow seamlessly into each other.
 */
public class ContinuousCalendarAdapter extends RecyclerView.Adapter<ContinuousCalendarAdapter.WeekViewHolder> {

    public static final int TOTAL_WEEKS = 10400; // ~200 years
    public static final int CENTER_POSITION = TOTAL_WEEKS / 2;

    private CalendarViewModel.CalendarData calendarData;
    private int focusedYear;
    private int focusedMonth; // 1-based (matches LocalDate.getMonthValue)
    private final MonthView.OnDayClickListener clickListener;
    private final LocalDate referenceDate; // Sunday of the week containing today
    private boolean isGroupView;
    private LocalDate selectedDate;

    public ContinuousCalendarAdapter(MonthView.OnDayClickListener listener) {
        this.clickListener = listener;
        this.isGroupView = false;
        LocalDate today = LocalDate.now();
        int dow = today.getDayOfWeek().getValue(); // 1=Mon ... 7=Sun
        int daysSinceSunday = dow == 7 ? 0 : dow;
        this.referenceDate = today.minusDays(daysSinceSunday);
        this.focusedYear = today.getYear();
        this.focusedMonth = today.getMonthValue();
    }

    public LocalDate getWeekStart(int position) {
        long offset = (long) (position - CENTER_POSITION);
        return referenceDate.plusWeeks(offset);
    }

    public void setCalendarData(CalendarViewModel.CalendarData data) {
        this.calendarData = data;
        notifyDataSetChanged();
    }

    public void setGroupView(boolean isGroupView) {
        if (this.isGroupView != isGroupView) {
            this.isGroupView = isGroupView;
            notifyDataSetChanged();
        }
    }

    public boolean isGroupView() {
        return isGroupView;
    }

    public void setSelectedDate(LocalDate date) {
        LocalDate oldDate = this.selectedDate;
        this.selectedDate = date;
        if (oldDate != null) {
            notifyItemChanged(getPositionForDate(oldDate));
        }
        if (date != null) {
            notifyItemChanged(getPositionForDate(date));
        }
    }

    public void setFocusedMonth(int year, int month) {
        if (this.focusedYear != year || this.focusedMonth != month) {
            this.focusedYear = year;
            this.focusedMonth = month;
            notifyDataSetChanged();
        }
    }

    public int getFocusedYear() {
        return focusedYear;
    }

    public int getFocusedMonth() {
        return focusedMonth;
    }

    public int getPositionForDate(LocalDate date) {
        int dow = date.getDayOfWeek().getValue();
        int daysSinceSunday = dow == 7 ? 0 : dow;
        LocalDate weekStart = date.minusDays(daysSinceSunday);
        long weeks = java.time.temporal.ChronoUnit.WEEKS.between(referenceDate, weekStart);
        return CENTER_POSITION + (int) weeks;
    }

    @NonNull
    @Override
    public WeekViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        WeekRowView view = new WeekRowView(parent.getContext());
        view.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return new WeekViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WeekViewHolder holder, int position) {
        WeekRowView view = (WeekRowView) holder.itemView;
        LocalDate weekStart = getWeekStart(position);
        view.bind(weekStart, focusedYear, focusedMonth, calendarData, clickListener, isGroupView, selectedDate);
    }

    @Override
    public int getItemCount() {
        return TOTAL_WEEKS;
    }

    static class WeekViewHolder extends RecyclerView.ViewHolder {
        WeekViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    // ================== Week Row Custom View ==================

    static class WeekRowView extends View {
        private LocalDate weekStart;
        private int focusedYear, focusedMonth;
        private CalendarViewModel.CalendarData data;
        private MonthView.OnDayClickListener clickListener;
        private boolean isGroupView;
        private LocalDate selectedDate;

        private final Paint dayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint otherMonthPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint todayBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint shiftPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint selectedBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private int textColor;
        private int todayColor;
        private int weekendColor = 0xFFE91E63;
        private int otherMonthTextColor;

        private int cellWidth, cellHeight;

        WeekRowView(Context context) {
            super(context);
            initPaints();
        }

        private void initPaints() {
            try {
                int[] attrs = {android.R.attr.textColorPrimary};
                android.content.res.TypedArray ta = getContext().obtainStyledAttributes(attrs);
                textColor = ta.getColor(0, 0xFF1C1B1F);
                ta.recycle();

                TypedValue tv = new TypedValue();
                getContext().getTheme().resolveAttribute(
                        com.google.android.material.R.attr.colorPrimary, tv, true);
                todayColor = tv.data;

                getContext().getTheme().resolveAttribute(
                        com.google.android.material.R.attr.colorOutline, tv, true);
                int outlineColor = tv.data;
                gridPaint.setColor((outlineColor & 0x00FFFFFF) | 0x20000000);
            } catch (Exception e) {
                textColor = 0xFF1C1B1F;
                todayColor = 0xFF1565C0;
                gridPaint.setColor(0x15000000);
            }

            otherMonthTextColor = (textColor & 0x00FFFFFF) | 0x38000000;

            dayPaint.setTextAlign(Paint.Align.CENTER);
            dayPaint.setColor(textColor);

            otherMonthPaint.setTextAlign(Paint.Align.CENTER);
            otherMonthPaint.setColor(otherMonthTextColor);

            todayBgPaint.setColor(todayColor);
            todayBgPaint.setStyle(Paint.Style.FILL);

            shiftPaint.setTextAlign(Paint.Align.CENTER);

            gridPaint.setStrokeWidth(1f);

            selectedBorderPaint.setStyle(Paint.Style.STROKE);
            selectedBorderPaint.setColor(todayColor);
            float density = getContext().getResources().getDisplayMetrics().density;
            selectedBorderPaint.setStrokeWidth(2f * density);
        }

        void bind(LocalDate weekStart, int focusedYear, int focusedMonth,
                  CalendarViewModel.CalendarData data, MonthView.OnDayClickListener listener, boolean isGroupView, LocalDate selectedDate) {
            this.weekStart = weekStart;
            this.focusedYear = focusedYear;
            this.focusedMonth = focusedMonth;
            this.data = data;
            this.clickListener = listener;
            this.isGroupView = isGroupView;
            this.selectedDate = selectedDate;

            invalidate();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int w = MeasureSpec.getSize(widthMeasureSpec);
            cellWidth = w / 7;
            if (isGroupView) {
                int groupCount = (data != null && data.groups != null) ? data.groups.size() : 0;
                float baseHeight = cellWidth * 0.6f;
                float itemHeight = cellWidth * 0.28f;
                cellHeight = (int) (baseHeight + Math.max(1, groupCount) * itemHeight);
            } else {
                cellHeight = (int) (cellWidth * 1.2f);
            }
            setMeasuredDimension(w, cellHeight);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (weekStart == null) return;

            dayPaint.setTextSize(cellWidth * 0.32f);
            shiftPaint.setTextSize(cellWidth * 0.2f);
            otherMonthPaint.setTextSize(cellWidth * 0.32f);

            LocalDate today = LocalDate.now();

            for (int i = 0; i < 7; i++) {
                LocalDate date = weekStart.plusDays(i);
                float left = i * cellWidth;
                float top = 0;
                float centerX = left + cellWidth / 2f;

                boolean isFocusedMonth = (date.getYear() == focusedYear &&
                        date.getMonthValue() == focusedMonth);
                boolean isToday = date.equals(today);
                boolean isWeekend = (i == 0 || i == 6);

                // Draw today background circle
                if (isToday) {
                    float radius = cellWidth * 0.28f;
                    canvas.drawCircle(centerX, top + cellWidth * 0.38f, radius, todayBgPaint);
                }

                // Choose paint based on state
                Paint usePaint;
                if (isToday) {
                    dayPaint.setColor(Color.WHITE);
                    usePaint = dayPaint;
                } else if (!isFocusedMonth) {
                    usePaint = otherMonthPaint;
                } else {
                    dayPaint.setColor(isWeekend ? weekendColor : textColor);
                    usePaint = dayPaint;
                }

                // Draw date number
                canvas.drawText(String.valueOf(date.getDayOfMonth()), centerX,
                        top + cellWidth * 0.5f, usePaint);

                // Draw shift info
                drawShiftInfo(canvas, date, centerX, top, isFocusedMonth);
                
                // Draw selected border
                if (date.equals(selectedDate)) {
                    float radius = cellWidth * 0.28f;
                    canvas.drawCircle(centerX, top + cellWidth * 0.38f, radius, selectedBorderPaint);
                }
            }

            // Draw bottom border line
            canvas.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1, gridPaint);
        }

        private void drawShiftInfo(Canvas canvas, LocalDate date, float centerX, float top,
                                   boolean isFocusedMonth) {
            if (data == null) return;

            if (isGroupView) {
                if (data.groups == null) return;

                List<GroupShiftEntry> entries = new ArrayList<>();
                for (ShiftGroup group : data.groups) {
                    if (group.anchorDate == null) continue;
                    List<ShiftGroupSchedule> schedules = data.scheduleMap != null ?
                            data.scheduleMap.get(group.id) : null;
                    if (schedules == null) continue;
                    ShiftType shift = ShiftCalculator.getShiftForDate(group, date, schedules, data.shiftTypes);
                    entries.add(new GroupShiftEntry(group, shift));
                }

                entries.sort((a, b) -> {
                    int aTime = a.shift != null ? a.shift.getEffectiveStartMinutes() : Integer.MAX_VALUE;
                    int bTime = b.shift != null ? b.shift.getEffectiveStartMinutes() : Integer.MAX_VALUE;
                    return Integer.compare(aTime, bTime);
                });

                float textY = top + cellWidth * 0.85f;
                float itemHeight = cellWidth * 0.28f;
                shiftPaint.setTextSize(cellWidth * 0.20f);

                for (GroupShiftEntry entry : entries) {
                    int color = entry.group.color;
                    if (!isFocusedMonth) {
                        color = (color & 0x00FFFFFF) | 0x50000000;
                    }
                    shiftPaint.setColor(color);
                    shiftPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

                    String text = entry.shift != null ? entry.group.name + " " + entry.shift.name : entry.group.name + " " + getContext().getString(R.string.rest);
                    canvas.drawText(text, centerX, textY, shiftPaint);
                    textY += itemHeight;
                }

            } else {
                if (data.defaultGroup == null || data.defaultGroup.anchorDate == null) return;
                ShiftGroup group = data.defaultGroup;
                List<ShiftGroupSchedule> schedules = data.scheduleMap != null ?
                        data.scheduleMap.get(group.id) : null;
                if (schedules == null) return;

                ShiftType shift = ShiftCalculator.getShiftForDate(group, date, schedules, data.shiftTypes);

                if (shift != null) {
                    int color = group.color;
                    if (!isFocusedMonth) {
                        color = (color & 0x00FFFFFF) | 0x50000000;
                    }
                    shiftPaint.setColor(color);
                    shiftPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    canvas.drawText(shift.name, centerX, top + cellWidth * 0.85f, shiftPaint);
                }
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP && clickListener != null && weekStart != null) {
                float y = event.getY();
                if (y >= 0) {
                    int col = (int) (event.getX() / cellWidth);
                    if (col >= 0 && col < 7) {
                        LocalDate date = weekStart.plusDays(col);
                        clickListener.onDayClick(date);
                    }
                }
            }
            return true;
        }
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
