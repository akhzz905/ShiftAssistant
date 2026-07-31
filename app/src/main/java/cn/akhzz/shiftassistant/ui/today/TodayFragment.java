package cn.akhzz.shiftassistant.ui.today;

import android.graphics.drawable.GradientDrawable;
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
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import cn.akhzz.shiftassistant.MainActivity;
import cn.akhzz.shiftassistant.R;
import cn.akhzz.shiftassistant.data.entity.ShiftType;
import cn.akhzz.shiftassistant.util.ShiftCalculator;

public class TodayFragment extends Fragment {

    private TodayViewModel viewModel;
    private View contentLayout, emptyLayout;
    private View colorBar;
    private TextView tvDate, tvGroupName, tvShiftName, tvShiftTime, tvCycleDay;
    private RecyclerView rvAllShifts;
    private TodayShiftAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_today, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        contentLayout = view.findViewById(R.id.content_layout);
        emptyLayout = view.findViewById(R.id.empty_layout);
        colorBar = view.findViewById(R.id.color_bar);
        tvDate = view.findViewById(R.id.tv_date);
        tvGroupName = view.findViewById(R.id.tv_group_name);
        tvShiftName = view.findViewById(R.id.tv_shift_name);
        tvShiftTime = view.findViewById(R.id.tv_shift_time);
        tvCycleDay = view.findViewById(R.id.tv_cycle_day);
        rvAllShifts = view.findViewById(R.id.rv_all_shifts);

        adapter = new TodayShiftAdapter();
        rvAllShifts.setLayoutManager(new LinearLayoutManager(getContext()));
        rvAllShifts.setAdapter(adapter);

        view.findViewById(R.id.btn_go_manage).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToGroupManage();
            }
        });

        viewModel = new ViewModelProvider(this).get(TodayViewModel.class);
        observeData();
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

    private void observeData() {
        viewModel.getTodayData().observe(getViewLifecycleOwner(), data -> {
            if (data == null) return;

            // Set today's date
            LocalDate today = LocalDate.now();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.CHINESE);
            tvDate.setText(today.format(fmt));

            if (data.defaultGroup == null) {
                contentLayout.setVisibility(View.GONE);
                emptyLayout.setVisibility(View.VISIBLE);
                return;
            }

            contentLayout.setVisibility(View.VISIBLE);
            emptyLayout.setVisibility(View.GONE);

            // Color bar
            GradientDrawable barBg = new GradientDrawable();
            barBg.setShape(GradientDrawable.RECTANGLE);
            barBg.setCornerRadius(4f);
            barBg.setColor(data.defaultGroup.color);
            colorBar.setBackground(barBg);

            // Group name
            tvGroupName.setText(data.defaultGroup.name);
            tvGroupName.setTextColor(data.defaultGroup.color);

            // Shift info
            ShiftType shift = data.defaultShift;
            if (shift != null) {
                tvShiftName.setText(shift.name);
                tvShiftTime.setText(shift.getFullTimeDescription());
                tvShiftTime.setVisibility(View.VISIBLE);
            } else {
                tvShiftName.setText(R.string.rest);
                tvShiftTime.setVisibility(View.GONE);
            }

            // Cycle day info
            int dayIndex = ShiftCalculator.getDayIndexForDate(data.defaultGroup, today);
            tvCycleDay.setText(getString(R.string.cycle_day_info,
                    dayIndex + 1, data.defaultGroup.cycleDays));

            // All groups list
            if (data.allGroupShifts != null && !data.allGroupShifts.isEmpty()) {
                adapter.setData(data.allGroupShifts);
            }
        });
    }
}
