package cn.akhzz.shiftassistant.ui.group;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.List;

import cn.akhzz.shiftassistant.R;
import cn.akhzz.shiftassistant.data.entity.ShiftType;
import cn.akhzz.shiftassistant.data.repository.ShiftRepository;

public class ShiftTypeManageActivity extends AppCompatActivity {

    private ShiftRepository repository;
    private ShiftTypeListAdapter adapter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shift_type_manage);

        repository = new ShiftRepository(getApplication());

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rv_shift_types);
        tvEmpty = findViewById(R.id.tv_empty);
        FloatingActionButton fab = findViewById(R.id.fab_add);

        adapter = new ShiftTypeListAdapter(
                type -> showEditDialog(type),     // click: edit
                type -> showDeleteDialog(type)    // long click: delete
        );
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        fab.setOnClickListener(v -> showEditDialog(null));

        // Observe shift types
        LiveData<List<ShiftType>> liveData = repository.getAllShiftTypesLive();
        liveData.observe(this, types -> {
            adapter.setData(types);
            tvEmpty.setVisibility(types == null || types.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void showEditDialog(ShiftType existing) {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_edit_shift_type, null);

        TextInputEditText etName = dialogView.findViewById(R.id.et_name);
        View colorPreview = dialogView.findViewById(R.id.color_preview);
        MaterialButton btnPickColor = dialogView.findViewById(R.id.btn_pick_color);
        MaterialButton btnStartTime = dialogView.findViewById(R.id.btn_start_time);
        MaterialButton btnEndTime = dialogView.findViewById(R.id.btn_end_time);
        Chip chipStartPrev = dialogView.findViewById(R.id.chip_start_prev);
        Chip chipStartCurrent = dialogView.findViewById(R.id.chip_start_current);
        Chip chipEndCurrent = dialogView.findViewById(R.id.chip_end_current);
        Chip chipEndNext = dialogView.findViewById(R.id.chip_end_next);
        TextView tvCrossDayNote = dialogView.findViewById(R.id.tv_cross_day_note);

        // Time and Color state holders
        final int[] startMinutes = {480};  // default 08:00
        final int[] endMinutes = {960};    // default 16:00
        final int[] startDayOffset = {0};  // 0 = current day
        final int[] endDayOffset = {0};    // 0 = current day
        final int[] selectedColor = {cn.akhzz.shiftassistant.util.ColorUtils.GROUP_COLORS[0]};

        // Populate if editing
        if (existing != null) {
            etName.setText(existing.name);
            startMinutes[0] = existing.startTimeMinutes;
            endMinutes[0] = existing.endTimeMinutes;
            startDayOffset[0] = existing.startDayOffset;
            endDayOffset[0] = existing.endDayOffset;
            selectedColor[0] = existing.color;

            btnStartTime.setText(existing.getStartTimeString());
            btnEndTime.setText(existing.getEndTimeString());

            if (existing.startDayOffset == -1) {
                chipStartPrev.setChecked(true);
            } else {
                chipStartCurrent.setChecked(true);
            }
            if (existing.endDayOffset == 1) {
                chipEndNext.setChecked(true);
            } else {
                chipEndCurrent.setChecked(true);
            }
        } else {
            java.util.List<Integer> usedColors = new java.util.ArrayList<>();
            if (adapter.getItemCount() > 0) {
                for (ShiftType type : adapter.getCurrentList()) {
                    usedColors.add(type.color);
                }
            }
            selectedColor[0] = cn.akhzz.shiftassistant.util.ColorUtils.getNextAvailableColor(usedColors);
        }

        Runnable updateColorPreview = () -> {
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            bg.setColor(selectedColor[0]);
            colorPreview.setBackground(bg);
        };
        updateColorPreview.run();

        btnPickColor.setOnClickListener(v -> {
            ColorPickerDialog dialog = new ColorPickerDialog(this, color -> {
                selectedColor[0] = color;
                updateColorPreview.run();
            });
            dialog.show();
        });

        Runnable updateCrossDayNote = () -> {
            boolean startIsPrev = startDayOffset[0] != 0;
            boolean endIsNext = endDayOffset[0] != 0;
            if (startIsPrev || endIsNext) {
                tvCrossDayNote.setVisibility(View.VISIBLE);
                String name = etName.getText().toString().trim();
                if (name.isEmpty()) name = "该班次";
                String startTime = btnStartTime.getText().toString();
                String endTime = btnEndTime.getText().toString();
                if (startIsPrev) {
                    tvCrossDayNote.setText(getString(R.string.cross_day_note_prev, name, startTime, endTime));
                } else {
                    tvCrossDayNote.setText(getString(R.string.cross_day_note_next, name, startTime, endTime));
                }
            } else {
                tvCrossDayNote.setVisibility(View.GONE);
            }
        };
        updateCrossDayNote.run();

        etName.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                updateCrossDayNote.run();
            }
        });

        // Chip listeners
        chipStartPrev.setOnCheckedChangeListener((v, checked) -> {
            if (checked) {
                startDayOffset[0] = -1;
                if (chipEndNext.isChecked()) {
                    chipEndCurrent.setChecked(true);
                }
                updateCrossDayNote.run();
            }
        });
        chipStartCurrent.setOnCheckedChangeListener((v, checked) -> {
            if (checked) {
                startDayOffset[0] = 0;
                updateCrossDayNote.run();
            }
        });
        chipEndCurrent.setOnCheckedChangeListener((v, checked) -> {
            if (checked) {
                endDayOffset[0] = 0;
                updateCrossDayNote.run();
            }
        });
        chipEndNext.setOnCheckedChangeListener((v, checked) -> {
            if (checked) {
                endDayOffset[0] = 1;
                if (chipStartPrev.isChecked()) {
                    chipStartCurrent.setChecked(true);
                }
                updateCrossDayNote.run();
            }
        });

        // Time picker buttons
        btnStartTime.setOnClickListener(v -> {
            int h = startMinutes[0] / 60;
            int m = startMinutes[0] % 60;
            MaterialTimePicker picker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(h).setMinute(m)
                    .setTitleText(R.string.start_time)
                    .build();
            picker.addOnPositiveButtonClickListener(dialog -> {
                startMinutes[0] = picker.getHour() * 60 + picker.getMinute();
                btnStartTime.setText(String.format("%02d:%02d",
                        picker.getHour(), picker.getMinute()));
                updateCrossDayNote.run();
            });
            picker.show(getSupportFragmentManager(), "start_time");
        });

        btnEndTime.setOnClickListener(v -> {
            int h = endMinutes[0] / 60;
            int m = endMinutes[0] % 60;
            MaterialTimePicker picker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(h).setMinute(m)
                    .setTitleText(R.string.end_time)
                    .build();
            picker.addOnPositiveButtonClickListener(dialog -> {
                endMinutes[0] = picker.getHour() * 60 + picker.getMinute();
                btnEndTime.setText(String.format("%02d:%02d",
                        picker.getHour(), picker.getMinute()));
                updateCrossDayNote.run();
            });
            picker.show(getSupportFragmentManager(), "end_time");
        });

        // Build dialog
        String title = existing != null ?
                getString(R.string.edit_shift_type) : getString(R.string.add_shift_type);

        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(dialogView)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String name = etName.getText() != null ?
                            etName.getText().toString().trim() : "";
                    if (name.isEmpty()) return;

                    if (existing != null) {
                        existing.name = name;
                        existing.startTimeMinutes = startMinutes[0];
                        existing.endTimeMinutes = endMinutes[0];
                        existing.startDayOffset = startDayOffset[0];
                        existing.endDayOffset = endDayOffset[0];
                        existing.color = selectedColor[0];
                        repository.updateShiftType(existing);
                    } else {
                        ShiftType newType = new ShiftType(name,
                                startMinutes[0], endMinutes[0],
                                startDayOffset[0], endDayOffset[0], selectedColor[0]);
                        repository.insertShiftType(newType);
                    }
                })
                .show();
    }

    private void showDeleteDialog(ShiftType type) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_delete)
                .setMessage(getString(R.string.confirm_delete_shift_type, type.name))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, w) -> repository.deleteShiftType(type))
                .show();
    }
}
