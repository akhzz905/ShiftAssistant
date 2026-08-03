package cn.akhzz.shiftassistant.ui.calendar;

import android.app.Dialog;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import cn.akhzz.shiftassistant.R;
import cn.akhzz.shiftassistant.data.AppDatabase;
import cn.akhzz.shiftassistant.data.entity.ShiftGroup;
import cn.akhzz.shiftassistant.data.entity.ShiftGroupSchedule;
import cn.akhzz.shiftassistant.data.entity.ShiftType;
import cn.akhzz.shiftassistant.data.repository.ShiftRepository;
import cn.akhzz.shiftassistant.util.ShiftCalculator;

public class DayDetailDialog extends DialogFragment {

    private static final String ARG_DATE = "date";

    public static DayDetailDialog newInstance(LocalDate date) {
        DayDetailDialog dialog = new DayDetailDialog();
        Bundle args = new Bundle();
        args.putString(ARG_DATE, date.toString());
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        String dateStr = requireArguments().getString(ARG_DATE);
        LocalDate date = LocalDate.parse(dateStr);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.CHINESE);
        String title = date.format(fmt);

        // Build content dynamically
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        container.setPadding(pad, pad, pad, pad);

        // Loading text (will be replaced)
        TextView loadingText = new TextView(requireContext());
        loadingText.setText("加载中...");
        loadingText.setGravity(Gravity.CENTER);
        loadingText.setPadding(0, pad, 0, pad);
        container.addView(loadingText);

        // Load data on background thread
        AppDatabase.databaseWriteExecutor.execute(() -> {
            ShiftRepository repo = new ShiftRepository(requireActivity().getApplication());
            List<ShiftGroup> groups = repo.getAllGroupsSync();
            List<ShiftType> types = repo.getAllShiftTypesSync();

            if (getActivity() == null) return;

            class GroupShift {
                ShiftGroup group;
                ShiftType shift;
                GroupShift(ShiftGroup g, ShiftType s) { group = g; shift = s; }
            }
            List<GroupShift> data = new java.util.ArrayList<>();
            for (ShiftGroup group : groups) {
                if (group.anchorDate == null) continue;
                List<ShiftGroupSchedule> schedules = repo.getSchedulesByGroupIdSync(group.id);
                ShiftType shift = ShiftCalculator.getShiftForDate(group, date, schedules, types);
                data.add(new GroupShift(group, shift));
            }

            data.sort((a, b) -> {
                int aTime = a.shift != null ? a.shift.getEffectiveStartMinutes() : Integer.MAX_VALUE;
                int bTime = b.shift != null ? b.shift.getEffectiveStartMinutes() : Integer.MAX_VALUE;
                if (aTime != bTime) {
                    return Integer.compare(aTime, bTime);
                }
                return Long.compare(a.group.id, b.group.id);
            });

            requireActivity().runOnUiThread(() -> {
                container.removeAllViews();

                if (data.isEmpty()) {
                    TextView empty = new TextView(requireContext());
                    empty.setText(R.string.no_groups);
                    empty.setGravity(Gravity.CENTER);
                    empty.setPadding(0, pad, 0, pad);
                    container.addView(empty);
                    return;
                }

                for (GroupShift gs : data) {
                    ShiftGroup group = gs.group;
                    ShiftType shift = gs.shift;

                    LinearLayout row = new LinearLayout(requireContext());
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(0, dp(8), 0, dp(8));

                    // Color dot
                    View dot = new View(requireContext());
                    int dotSize = dp(12);
                    LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dotSize, dotSize);
                    dotParams.setMarginEnd(dp(12));
                    dot.setLayoutParams(dotParams);
                    GradientDrawable dotBg = new GradientDrawable();
                    dotBg.setShape(GradientDrawable.OVAL);
                    dotBg.setColor(group.color);
                    dot.setBackground(dotBg);
                    row.addView(dot);

                    // Group name
                    TextView tvName = new TextView(requireContext());
                    tvName.setText(group.name);
                    tvName.setTextSize(15);
                    tvName.setTextColor(group.color);
                    tvName.setTypeface(Typeface.DEFAULT_BOLD);
                    LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                            dp(70), ViewGroup.LayoutParams.WRAP_CONTENT);
                    tvName.setLayoutParams(nameParams);
                    row.addView(tvName);

                    // Shift info
                    TextView tvShift = new TextView(requireContext());
                    if (shift != null) {
                        tvShift.setText(shift.name + "\n" + shift.getFullTimeDescription());
                    } else {
                        tvShift.setText(R.string.rest);
                        tvShift.setTextColor(0xFF9E9E9E);
                    }
                    tvShift.setTextSize(14);
                    tvShift.setLineSpacing(dp(2), 1f);
                    row.addView(tvShift);

                    container.addView(row);
                }
            });
        });

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setView(container)
                .setPositiveButton(R.string.cancel, null)
                .create();
    }

    private int dp(int dp) {
        return (int) (dp * requireContext().getResources().getDisplayMetrics().density);
    }
}
