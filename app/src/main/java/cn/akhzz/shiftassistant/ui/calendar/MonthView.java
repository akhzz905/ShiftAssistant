package cn.akhzz.shiftassistant.ui.calendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Map;

public class MonthView extends View {

    private int year, month; // month is 0-based (Calendar.MONTH)
    private int daysInMonth;
    private int firstDayOfWeek; // 0=Sunday
    private int prevMonthDays; // days in previous month
    private int todayDay = -1;
    private Map<Integer, DayShiftInfo> dayShifts;

    private final Paint dayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint todayBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shiftPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint otherMonthPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint monthLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int cellWidth, cellHeight;
    private int monthLabelHeight;

    private OnDayClickListener listener;

    // Theme colors
    private int textColor = 0xFF1C1B1F;
    private int todayColor = 0xFF1565C0;
    private int weekendColor = 0xFFE91E63;
    private int otherMonthColor = 0x40808080;

    public MonthView(Context context) {
        super(context);
        init();
    }

    private void init() {
        // Resolve theme colors
        try {
            int[] attrs = {android.R.attr.textColorPrimary};
            android.content.res.TypedArray ta = getContext().obtainStyledAttributes(attrs);
            textColor = ta.getColor(0, textColor);
            ta.recycle();

            TypedValue tv = new TypedValue();
            getContext().getTheme().resolveAttribute(
                    com.google.android.material.R.attr.colorPrimary, tv, true);
            todayColor = tv.data;
        } catch (Exception ignored) {}

        // Other month paint - lighter version of text
        otherMonthColor = (textColor & 0x00FFFFFF) | 0x40000000;

        dayPaint.setTextAlign(Paint.Align.CENTER);
        dayPaint.setColor(textColor);

        todayBgPaint.setColor(todayColor);
        todayBgPaint.setStyle(Paint.Style.FILL);

        shiftPaint.setTextAlign(Paint.Align.CENTER);

        otherMonthPaint.setTextAlign(Paint.Align.CENTER);
        otherMonthPaint.setColor(otherMonthColor);

        monthLabelPaint.setTextAlign(Paint.Align.LEFT);
        monthLabelPaint.setColor(textColor);
        monthLabelPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
    }

    public void setYearMonth(int year, int month) {
        this.year = year;
        this.month = month;

        Calendar cal = Calendar.getInstance();
        cal.set(year, month, 1);
        daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0=Sunday

        // Previous month days
        cal.add(Calendar.MONTH, -1);
        prevMonthDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Check if current month
        Calendar today = Calendar.getInstance();
        if (today.get(Calendar.YEAR) == year && today.get(Calendar.MONTH) == month) {
            todayDay = today.get(Calendar.DAY_OF_MONTH);
        } else {
            todayDay = -1;
        }

        requestLayout();
        invalidate();
    }

    public void setDayShifts(Map<Integer, DayShiftInfo> shifts) {
        this.dayShifts = shifts;
        invalidate();
    }

    public void setOnDayClickListener(OnDayClickListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        cellWidth = width / 7;
        cellHeight = (int) (cellWidth * 1.3f);
        monthLabelHeight = (int) (cellWidth * 0.6f);
        int rows = getRowCount();
        int totalHeight = monthLabelHeight + rows * cellHeight;
        setMeasuredDimension(width, totalHeight);
    }

    private int getRowCount() {
        return (firstDayOfWeek + daysInMonth + 6) / 7;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw month label (e.g. "2026年8月")
        monthLabelPaint.setTextSize(cellWidth * 0.32f);
        float labelX = cellWidth * 0.2f;
        float labelY = monthLabelHeight * 0.72f;
        canvas.drawText((month + 1) + "月", labelX, labelY, monthLabelPaint);

        // Draw day cells
        dayPaint.setTextSize(cellWidth * 0.32f);
        shiftPaint.setTextSize(cellWidth * 0.22f);
        otherMonthPaint.setTextSize(cellWidth * 0.32f);

        int rows = getRowCount();
        int totalCells = rows * 7;

        for (int cell = 0; cell < totalCells; cell++) {
            int row = cell / 7;
            int col = cell % 7;

            float left = col * cellWidth;
            float top = monthLabelHeight + row * cellHeight;
            float centerX = left + cellWidth / 2f;

            int dayInMonth = cell - firstDayOfWeek + 1;

            if (dayInMonth < 1) {
                // Previous month trailing day
                int prevDay = prevMonthDays + dayInMonth;
                canvas.drawText(String.valueOf(prevDay), centerX, top + cellHeight * 0.38f, otherMonthPaint);
            } else if (dayInMonth > daysInMonth) {
                // Next month leading day
                int nextDay = dayInMonth - daysInMonth;
                canvas.drawText(String.valueOf(nextDay), centerX, top + cellHeight * 0.38f, otherMonthPaint);
            } else {
                // Current month day
                if (dayInMonth == todayDay) {
                    float radius = cellWidth * 0.28f;
                    canvas.drawCircle(centerX, top + cellHeight * 0.3f, radius, todayBgPaint);
                    dayPaint.setColor(Color.WHITE);
                } else {
                    if (col == 0 || col == 6) {
                        dayPaint.setColor(weekendColor);
                    } else {
                        dayPaint.setColor(textColor);
                    }
                }

                canvas.drawText(String.valueOf(dayInMonth), centerX, top + cellHeight * 0.38f, dayPaint);

                // Draw shift info
                if (dayShifts != null && dayShifts.containsKey(dayInMonth)) {
                    DayShiftInfo info = dayShifts.get(dayInMonth);
                    if (info != null && info.shiftName != null) {
                        shiftPaint.setColor(info.color);
                        shiftPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                        canvas.drawText(info.shiftName, centerX, top + cellHeight * 0.68f, shiftPaint);

                        Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                        dotPaint.setColor(info.color);
                        float dotRadius = cellWidth * 0.04f;
                        canvas.drawCircle(centerX, top + cellHeight * 0.82f, dotRadius, dotPaint);
                    }
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP && listener != null) {
            float x = event.getX();
            float y = event.getY();

            if (y < monthLabelHeight) return true;

            int col = (int) (x / cellWidth);
            int row = (int) ((y - monthLabelHeight) / cellHeight);

            if (col >= 0 && col < 7 && row >= 0) {
                int day = row * 7 + col - firstDayOfWeek + 1;
                if (day >= 1 && day <= daysInMonth) {
                    listener.onDayClick(LocalDate.of(year, month + 1, day));
                }
            }
        }
        return true;
    }

    // ======================== Data classes ========================

    public interface OnDayClickListener {
        void onDayClick(LocalDate date);
    }

    public static class DayShiftInfo {
        public final String shiftName;
        public final int color;

        public DayShiftInfo(String shiftName, int color) {
            this.shiftName = shiftName;
            this.color = color;
        }
    }
}
