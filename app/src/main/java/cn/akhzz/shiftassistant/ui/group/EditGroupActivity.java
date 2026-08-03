package cn.akhzz.shiftassistant.ui.group;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import cn.akhzz.shiftassistant.R;
import cn.akhzz.shiftassistant.data.AppDatabase;
import cn.akhzz.shiftassistant.data.entity.ShiftGroup;
import cn.akhzz.shiftassistant.data.entity.ShiftGroupSchedule;
import cn.akhzz.shiftassistant.data.entity.ShiftType;
import cn.akhzz.shiftassistant.data.repository.ShiftRepository;
import cn.akhzz.shiftassistant.util.ColorUtils;

public class EditGroupActivity extends AppCompatActivity {

    private long groupId = -1;
    private ShiftRepository repository;

    private TextInputEditText etName;
    private android.view.View colorPreview;
    private android.view.View cardNoShiftTypes;
    private RecyclerView rvSchedule;
    private ScheduleEditAdapter scheduleAdapter;

    private int selectedColor;
    private int cycleDays = 4;
    private List<ShiftType> shiftTypes = new ArrayList<>();
    private ShiftGroup existingGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_group);

        repository = new ShiftRepository(getApplication());
        groupId = getIntent().getLongExtra("group_id", -1);

        initViews();
        loadShiftTypes();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(groupId == -1 ? R.string.add_group : R.string.edit_group);
        toolbar.setNavigationOnClickListener(v -> finish());

        etName = findViewById(R.id.et_name);
        colorPreview = findViewById(R.id.color_preview);
        cardNoShiftTypes = findViewById(R.id.card_no_shift_types);
        rvSchedule = findViewById(R.id.rv_schedule);

        // Default color
        selectedColor = ColorUtils.getDefaultColor(0);
        updateColorPreview();

        // Add day button
        findViewById(R.id.btn_add_day).setOnClickListener(v -> {
            if (scheduleAdapter != null) {
                scheduleAdapter.addDay();
                // Force RecyclerView to re-measure inside NestedScrollView
                rvSchedule.post(() -> {
                    rvSchedule.requestLayout();
                    rvSchedule.scrollToPosition(scheduleAdapter.getItemCount() - 1);
                });
            }
        });

        // Color picker button
        findViewById(R.id.btn_pick_color).setOnClickListener(v -> showColorPicker());

        // Go to shift type management
        findViewById(R.id.btn_go_shift_type).setOnClickListener(v -> {
            startActivity(new Intent(this, ShiftTypeManageActivity.class));
        });

        // Schedule RecyclerView
        rvSchedule.setLayoutManager(new LinearLayoutManager(this));
        rvSchedule.setNestedScrollingEnabled(false);

        // Save button
        findViewById(R.id.btn_save).setOnClickListener(v -> save());
    }

    private void loadShiftTypes() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            shiftTypes = repository.getAllShiftTypesSync();

            // Find used colors for next available logic
            List<ShiftGroup> allGroups = repository.getAllGroupsSync();
            List<Integer> usedColors = new ArrayList<>();
            for (ShiftGroup g : allGroups) {
                usedColors.add(g.color);
            }
            final int nextColor = ColorUtils.getNextAvailableColor(usedColors);

            // If editing, load existing group
            if (groupId != -1) {
                existingGroup = repository.getGroupById(groupId);
            }

            runOnUiThread(() -> {
                if (shiftTypes.isEmpty()) {
                    cardNoShiftTypes.setVisibility(android.view.View.VISIBLE);
                    rvSchedule.setVisibility(android.view.View.GONE);
                } else {
                    cardNoShiftTypes.setVisibility(android.view.View.GONE);
                    rvSchedule.setVisibility(android.view.View.VISIBLE);
                }

                if (existingGroup != null) {
                    populateExistingData();
                } else {
                    selectedColor = nextColor;
                    updateColorPreview();
                    updateScheduleList();
                }
            });
        });
    }

    private void populateExistingData() {
        etName.setText(existingGroup.name);
        cycleDays = existingGroup.cycleDays;
        selectedColor = existingGroup.color;
        updateColorPreview();

        // Calculate today's day index
        int todayIndex = 0;
        if (existingGroup.anchorDate != null) {
            todayIndex = cn.akhzz.shiftassistant.util.ShiftCalculator
                    .getDayIndexForDate(existingGroup, LocalDate.now());
        }

        // Load existing schedules
        final int finalTodayIndex = todayIndex;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<ShiftGroupSchedule> schedules =
                    repository.getSchedulesByGroupIdSync(groupId);
            runOnUiThread(() -> {
                scheduleAdapter = new ScheduleEditAdapter(cycleDays, shiftTypes, schedules, finalTodayIndex, this::onAdapterItemCountChanged);
                rvSchedule.setAdapter(scheduleAdapter);
            });
        });
    }




    private void updateScheduleList() {
        if (shiftTypes.isEmpty()) return;
        scheduleAdapter = new ScheduleEditAdapter(cycleDays, shiftTypes, null, 0, this::onAdapterItemCountChanged);
        rvSchedule.setAdapter(scheduleAdapter);
    }

    private void onAdapterItemCountChanged(int count) {
        cycleDays = count;
    }

    private void updateColorPreview() {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(selectedColor);
        colorPreview.setBackground(bg);
    }

    private void showColorPicker() {
        ColorPickerDialog dialog = new ColorPickerDialog(this, color -> {
            selectedColor = color;
            updateColorPreview();
        });
        dialog.show();
    }

    private void save() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        if (TextUtils.isEmpty(name)) {
            etName.setError(getString(R.string.input_required));
            return;
        }

        int todayDayIndex = scheduleAdapter != null ? scheduleAdapter.getTodayIndex() : 0;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            ShiftGroup group;
            if (existingGroup != null) {
                group = existingGroup;
            } else {
                group = new ShiftGroup();
            }

            group.name = name;
            group.cycleDays = cycleDays;
            group.color = selectedColor;
            group.anchorDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            group.anchorDayIndex = todayDayIndex;

            long id;
            if (existingGroup != null) {
                repository.updateGroup(group);
                id = group.id;
            } else {
                id = repository.insertGroupSync(group);
            }

            // Save schedules
            if (scheduleAdapter != null) {
                List<ShiftGroupSchedule> schedules = scheduleAdapter.getSchedules(id);
                repository.replaceSchedules(id, schedules);
            }

            runOnUiThread(this::finish);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload shift types in case user added new ones
        if (scheduleAdapter != null) {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                List<ShiftType> newTypes = repository.getAllShiftTypesSync();
                runOnUiThread(() -> {
                    if (!newTypes.isEmpty() && shiftTypes.isEmpty()) {
                        shiftTypes = newTypes;
                        cardNoShiftTypes.setVisibility(android.view.View.GONE);
                        rvSchedule.setVisibility(android.view.View.VISIBLE);
                        updateScheduleList();
                    } else {
                        shiftTypes = newTypes;
                        if (scheduleAdapter != null) {
                            scheduleAdapter.updateShiftTypes(newTypes);
                        }
                    }
                });
            });
        }
    }
}
