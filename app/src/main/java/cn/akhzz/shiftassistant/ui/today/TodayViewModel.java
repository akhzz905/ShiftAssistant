package cn.akhzz.shiftassistant.ui.today;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import cn.akhzz.shiftassistant.data.AppDatabase;
import cn.akhzz.shiftassistant.data.entity.ShiftGroup;
import cn.akhzz.shiftassistant.data.entity.ShiftGroupSchedule;
import cn.akhzz.shiftassistant.data.entity.ShiftType;
import cn.akhzz.shiftassistant.data.repository.ShiftRepository;
import cn.akhzz.shiftassistant.util.ShiftCalculator;

public class TodayViewModel extends AndroidViewModel {

    private final ShiftRepository repository;
    private final MutableLiveData<TodayData> todayData = new MutableLiveData<>();

    public TodayViewModel(@NonNull Application application) {
        super(application);
        repository = new ShiftRepository(application);
    }

    public LiveData<TodayData> getTodayData() {
        return todayData;
    }

    public void refresh() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            LocalDate today = LocalDate.now();
            TodayData data = new TodayData();

            // Get all shift types
            List<ShiftType> allTypes = repository.getAllShiftTypesSync();

            // Get default group
            data.defaultGroup = repository.getDefaultGroupSync();

            // Compute default group's shift
            if (data.defaultGroup != null && data.defaultGroup.anchorDate != null) {
                List<ShiftGroupSchedule> schedules =
                        repository.getSchedulesByGroupIdSync(data.defaultGroup.id);
                data.defaultShift = ShiftCalculator.getShiftForDate(
                        data.defaultGroup, today, schedules, allTypes);
            }

            // Compute all groups' shifts
            List<ShiftGroup> allGroups = repository.getAllGroupsSync();
            data.allGroupShifts = new ArrayList<>();
            for (ShiftGroup group : allGroups) {
                if (group.anchorDate == null) continue;
                List<ShiftGroupSchedule> schedules =
                        repository.getSchedulesByGroupIdSync(group.id);
                ShiftType shift = ShiftCalculator.getShiftForDate(
                        group, today, schedules, allTypes);
                data.allGroupShifts.add(new GroupShiftInfo(group, shift));
            }

            // Sort by shift start time, placing rest (shift == null) at the end
            data.allGroupShifts.sort((a, b) -> {
                int aTime = a.shift != null ? a.shift.getEffectiveStartMinutes() : Integer.MAX_VALUE;
                int bTime = b.shift != null ? b.shift.getEffectiveStartMinutes() : Integer.MAX_VALUE;
                if (aTime != bTime) {
                    return Integer.compare(aTime, bTime);
                }
                return Long.compare(a.group.id, b.group.id);
            });

            todayData.postValue(data);
        });
    }

    // ======================== Data classes ========================

    public static class TodayData {
        public ShiftGroup defaultGroup;
        public ShiftType defaultShift;
        public List<GroupShiftInfo> allGroupShifts;
    }

    public static class GroupShiftInfo {
        public final ShiftGroup group;
        public final ShiftType shift; // null = rest

        public GroupShiftInfo(ShiftGroup group, ShiftType shift) {
            this.group = group;
            this.shift = shift;
        }
    }
}
