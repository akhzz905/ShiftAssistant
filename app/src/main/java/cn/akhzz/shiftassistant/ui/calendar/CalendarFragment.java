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
    }

    private void setupContinuousCalendar() {
        continuousAdapter = new ContinuousCalendarAdapter(date -> showDayDetail(date));
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        continuousCalendarRv.setLayoutManager(layoutManager);
        continuousCalendarRv.setAdapter(continuousAdapter);
        continuousCalendarRv.scrollToPosition(ContinuousCalendarAdapter.CENTER_POSITION);

        continuousCalendarRv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
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
                        continuousAdapter.setFocusedMonth(year, month + 1);
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
                if (continuousCalendarRv.getVisibility() != View.VISIBLE) {
                    continuousCalendarRv.setAlpha(0f);
                    continuousCalendarRv.setVisibility(View.VISIBLE);
                    continuousCalendarRv.animate().alpha(1f).setDuration(200).start();
                }
            }
        });
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
        continuousCalendarRv.scrollToPosition(continuousAdapter.getPositionForDate(
                LocalDate.of(viewModel.getCurrentYear(), viewModel.getCurrentMonth() + 1, 15)));
    }

    private void goToToday() {
        continuousCalendarRv.scrollToPosition(ContinuousCalendarAdapter.CENTER_POSITION);
        Calendar cal = Calendar.getInstance();
        viewModel.setCurrentYearMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH));
        updateMonthTitle(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH));
        continuousAdapter.setFocusedMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);
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
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && viewModel != null) {
            viewModel.refresh();
        }
    }
}
