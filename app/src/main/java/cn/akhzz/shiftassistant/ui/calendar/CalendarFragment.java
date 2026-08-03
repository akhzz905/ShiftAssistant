package cn.akhzz.shiftassistant.ui.calendar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.time.LocalDate;
import java.util.Calendar;

import cn.akhzz.shiftassistant.R;

public class CalendarFragment extends Fragment {

    private CalendarViewModel viewModel;
    private RecyclerView continuousCalendarRv;
    private ContinuousCalendarAdapter continuousAdapter;
    private TextView tvMonthTitle;
    private MaterialButton btnToggle;
    private View weekdayHeader;
    private boolean isMonthView = true;
    private boolean hasInitialPositioned = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CalendarViewModel.class);

        tvMonthTitle = view.findViewById(R.id.tv_month_title);
        continuousCalendarRv = view.findViewById(R.id.continuous_calendar_rv);
        btnToggle = view.findViewById(R.id.btn_toggle_view);
        weekdayHeader = view.findViewById(R.id.weekday_header);

        btnToggle.setOnClickListener(v -> toggleView());
        view.findViewById(R.id.btn_today).setOnClickListener(v -> goToToday());

        setupContinuousCalendar();
        observeData();

        // Set initial month title
        Calendar cal = Calendar.getInstance();
        updateMonthTitle(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH));

        tryPerformInitialPositioning();
    }

    private void setupContinuousCalendar() {
        continuousAdapter = new ContinuousCalendarAdapter(date -> showDayDetail(date));
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        continuousCalendarRv.setLayoutManager(layoutManager);
        continuousCalendarRv.setAdapter(continuousAdapter);
        // Rough scroll to get near today (RV not yet laid out, just sets target area)
        continuousCalendarRv.scrollToPosition(continuousAdapter.getPositionForDate(getTargetDateForToday()));

        continuousCalendarRv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!hasInitialPositioned) return;
                int firstVisible = layoutManager.findFirstVisibleItemPosition();
                int lastVisible = layoutManager.findLastVisibleItemPosition();
                if (firstVisible != RecyclerView.NO_POSITION && lastVisible != RecyclerView.NO_POSITION) {
                    int middleItem = firstVisible + (lastVisible - firstVisible) / 2;
                    LocalDate weekStart = continuousAdapter.getWeekStart(middleItem);
                    // Use Thursday to represent the week's month
                    LocalDate midWeek = weekStart.plusDays(3);
                    int year = midWeek.getYear();
                    int month = midWeek.getMonthValue() - 1; // 0-based for title and viewmodel
                    
                    if (year != viewModel.getCurrentYear() || month != viewModel.getCurrentMonth()) {
                        updateMonthTitle(year, month);
                        viewModel.setCurrentYearMonth(year, month);
                        continuousCalendarRv.post(() -> continuousAdapter.setFocusedMonth(
                                viewModel.getCurrentYear(), viewModel.getCurrentMonth() + 1));
                    }
                }
            }
        });
    }

    private void observeData() {
        continuousCalendarRv.setVisibility(View.INVISIBLE);
        viewModel.getCalendarData().observe(getViewLifecycleOwner(), data -> {
            if (data != null) {
                continuousAdapter.setCalendarData(data);
                if (hasInitialPositioned) {
                    showCalendarView();
                } else {
                    tryPerformInitialPositioning();
                }
            }
        });
    }

    /**
     * Ensures initial centering to today only occurs when the fragment is actually
     * visible to the user (!isHidden()) and the RecyclerView has completed valid layout.
     * Keeps RecyclerView invisible until centered positioning finishes to avoid jumping/flashing on opening.
     */
    private void tryPerformInitialPositioning() {
        if (hasInitialPositioned) return;
        if (isHidden() || getView() == null) return;
        if (viewModel.getCalendarData().getValue() == null) return;

        int rvHeight = continuousCalendarRv.getHeight();
        if (rvHeight > 0 && continuousCalendarRv.getWidth() > 0) {
            hasInitialPositioned = true;
            goToToday();
            continuousCalendarRv.post(this::showCalendarView);
        } else {
            continuousCalendarRv.getViewTreeObserver().addOnGlobalLayoutListener(
                    new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            if (isHidden() || getView() == null) return;
                            int h = continuousCalendarRv.getHeight();
                            int w = continuousCalendarRv.getWidth();
                            if (h > 0 && w > 0) {
                                continuousCalendarRv.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                if (!hasInitialPositioned) {
                                    hasInitialPositioned = true;
                                    goToToday();
                                    continuousCalendarRv.post(() -> showCalendarView());
                                }
                            }
                        }
                    });
        }
    }

    private void showCalendarView() {
        if (isHidden() || getView() == null) return;
        if (continuousCalendarRv.getVisibility() != View.VISIBLE) {
            continuousCalendarRv.setAlpha(0f);
            continuousCalendarRv.setVisibility(View.VISIBLE);
            continuousCalendarRv.animate().alpha(1f).setDuration(150).start();
        }
    }

    private void toggleView() {
        isMonthView = !isMonthView;
        continuousAdapter.setGroupView(!isMonthView);
        if (isMonthView) {
            btnToggle.setText(R.string.group_view);
        } else {
            btnToggle.setText(R.string.month_view);
        }
        // Restore scroll position since cell height changed significantly
        scrollToDate(LocalDate.of(viewModel.getCurrentYear(), viewModel.getCurrentMonth() + 1, 15));
    }

    private void goToToday() {
        LocalDate targetDate = getTargetDateForToday();
        scrollToDate(targetDate);
        Calendar cal = Calendar.getInstance();
        viewModel.setCurrentYearMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH));
        updateMonthTitle(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH));
        continuousAdapter.setFocusedMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);
    }

    /**
     * Calculates the best week row to center on when jumping to today.
     * If today is at the very beginning or end of a month (like Aug 1st), centering today's row
     * would cause onScrolled to see Wednesday (Jul 29th) in the previous month and overwrite the month title.
     * Shifting the scroll target by 1 week keeps today near the center while ensuring onScrolled sees today's month.
     */
    private LocalDate getTargetDateForToday() {
        LocalDate today = LocalDate.now();
        int todayMonth = today.getMonthValue();

        int dow = today.getDayOfWeek().getValue(); // 1=Mon ... 7=Sun
        int daysSinceSunday = (dow == 7) ? 0 : dow;
        LocalDate weekStart = today.minusDays(daysSinceSunday);
        LocalDate midWeek = weekStart.plusDays(3); // Wednesday — matches onScrolled logic

        if (midWeek.getMonthValue() != todayMonth) {
            if (today.getDayOfMonth() <= 7) {
                return today.plusWeeks(1);
            } else {
                return today.minusWeeks(1);
            }
        }
        return today;
    }

    private void scrollToDate(LocalDate date) {
        int position = continuousAdapter.getPositionForDate(date);
        LinearLayoutManager lm = (LinearLayoutManager) continuousCalendarRv.getLayoutManager();

        int rvHeight = continuousCalendarRv.getHeight();
        if (rvHeight > 0) {
            // RecyclerView already laid out — center directly
            int offset = calculateCenterOffset(rvHeight);
            lm.scrollToPositionWithOffset(position, offset);
        } else {
            // Not yet laid out — rough scroll only
            continuousCalendarRv.scrollToPosition(position);
        }
    }

    private int calculateCenterOffset(int rvHeight) {
        int cellWidth = continuousCalendarRv.getWidth() / 7;
        int cellHeight;
        if (continuousAdapter.isGroupView()) {
            CalendarViewModel.CalendarData data = viewModel.getCalendarData().getValue();
            int groupCount = (data != null && data.groups != null) ? data.groups.size() : 0;
            float baseHeight = cellWidth * 0.6f;
            float itemHeight = cellWidth * 0.28f;
            cellHeight = (int) (baseHeight + Math.max(1, groupCount) * itemHeight);
        } else {
            cellHeight = (int) (cellWidth * 1.2f);
        }
        return (rvHeight - cellHeight) / 2;
    }

    private void updateMonthTitle(int year, int month) {
        tvMonthTitle.setText(year + "年" + (month + 1) + "月");
    }

    private void showDayDetail(LocalDate date) {
        continuousAdapter.setSelectedDate(date);
        DayDetailDialog dialog = DayDetailDialog.newInstance(date);
        dialog.show(getParentFragmentManager(), "day_detail");
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.refresh();
        tryPerformInitialPositioning();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && viewModel != null) {
            viewModel.refresh();
            tryPerformInitialPositioning();
        }
    }
}
