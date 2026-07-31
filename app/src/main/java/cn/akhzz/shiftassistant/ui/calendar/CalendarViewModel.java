package cn.akhzz.shiftassistant.ui.calendar;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.akhzz.shiftassistant.data.AppDatabase;
import cn.akhzz.shiftassistant.data.entity.ShiftGroup;
import cn.akhzz.shiftassistant.data.entity.ShiftGroupSchedule;
import cn.akhzz.shiftassistant.data.entity.ShiftType;
import cn.akhzz.shiftassistant.data.repository.ShiftRepository;

public class CalendarViewModel extends AndroidViewModel {

    private final ShiftRepository repository;
    private final MutableLiveData<CalendarData> calendarData = new MutableLiveData<>();
    private int currentYear, currentMonth; // month is 0-based

    public CalendarViewModel(@NonNull Application application) {
        super(application);
        repository = new ShiftRepository(application);
        Calendar cal = Calendar.getInstance();
        currentYear = cal.get(Calendar.YEAR);
        currentMonth = cal.get(Calendar.MONTH);
    }

    public LiveData<CalendarData> getCalendarData() {
        return calendarData;
    }

    public void setCurrentYearMonth(int year, int month) {
        this.currentYear = year;
        this.currentMonth = month;
    }

    public int getCurrentYear() {
        return currentYear;
    }

    public int getCurrentMonth() {
        return currentMonth;
    }

    public void refresh() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            CalendarData data = new CalendarData();
            data.groups = repository.getAllGroupsSync();
            data.shiftTypes = repository.getAllShiftTypesSync();
            data.defaultGroup = repository.getDefaultGroupSync();
            data.scheduleMap = new HashMap<>();
            for (ShiftGroup group : data.groups) {
                List<ShiftGroupSchedule> schedules =
                        repository.getSchedulesByGroupIdSync(group.id);
                data.scheduleMap.put(group.id, schedules);
            }
            calendarData.postValue(data);
        });
    }

    public static class CalendarData {
        public List<ShiftGroup> groups;
        public List<ShiftType> shiftTypes;
        public ShiftGroup defaultGroup;
        public Map<Long, List<ShiftGroupSchedule>> scheduleMap;
    }
}
