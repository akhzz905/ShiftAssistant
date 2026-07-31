package cn.akhzz.shiftassistant.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

import cn.akhzz.shiftassistant.data.AppDatabase;
import cn.akhzz.shiftassistant.data.dao.ShiftGroupDao;
import cn.akhzz.shiftassistant.data.dao.ShiftGroupScheduleDao;
import cn.akhzz.shiftassistant.data.dao.ShiftTypeDao;
import cn.akhzz.shiftassistant.data.entity.ShiftGroup;
import cn.akhzz.shiftassistant.data.entity.ShiftGroupSchedule;
import cn.akhzz.shiftassistant.data.entity.ShiftType;

public class ShiftRepository {

    private final ShiftTypeDao shiftTypeDao;
    private final ShiftGroupDao shiftGroupDao;
    private final ShiftGroupScheduleDao scheduleDao;

    public ShiftRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        shiftTypeDao = db.shiftTypeDao();
        shiftGroupDao = db.shiftGroupDao();
        scheduleDao = db.shiftGroupScheduleDao();
    }

    // ======================== ShiftType ========================

    public LiveData<List<ShiftType>> getAllShiftTypesLive() {
        return shiftTypeDao.getAllLive();
    }

    public List<ShiftType> getAllShiftTypesSync() {
        return shiftTypeDao.getAll();
    }

    public ShiftType getShiftTypeById(long id) {
        return shiftTypeDao.getById(id);
    }

    public void insertShiftType(ShiftType type) {
        AppDatabase.databaseWriteExecutor.execute(() -> shiftTypeDao.insert(type));
    }

    public void updateShiftType(ShiftType type) {
        AppDatabase.databaseWriteExecutor.execute(() -> shiftTypeDao.update(type));
    }

    public void deleteShiftType(ShiftType type) {
        AppDatabase.databaseWriteExecutor.execute(() -> shiftTypeDao.delete(type));
    }

    // ======================== ShiftGroup ========================

    public LiveData<List<ShiftGroup>> getAllGroupsLive() {
        return shiftGroupDao.getAllLive();
    }

    public List<ShiftGroup> getAllGroupsSync() {
        return shiftGroupDao.getAll();
    }

    public ShiftGroup getGroupById(long id) {
        return shiftGroupDao.getById(id);
    }

    public ShiftGroup getDefaultGroupSync() {
        return shiftGroupDao.getDefault();
    }

    public LiveData<ShiftGroup> getDefaultGroupLive() {
        return shiftGroupDao.getDefaultLive();
    }

    public long insertGroupSync(ShiftGroup group) {
        return shiftGroupDao.insert(group);
    }

    public void insertGroup(ShiftGroup group) {
        AppDatabase.databaseWriteExecutor.execute(() -> shiftGroupDao.insert(group));
    }

    public void updateGroup(ShiftGroup group) {
        AppDatabase.databaseWriteExecutor.execute(() -> shiftGroupDao.update(group));
    }

    public void deleteGroup(ShiftGroup group) {
        AppDatabase.databaseWriteExecutor.execute(() -> shiftGroupDao.delete(group));
    }

    public void setDefaultGroup(long groupId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            shiftGroupDao.clearAllDefault();
            shiftGroupDao.setDefault(groupId);
        });
    }

    // ======================== Schedule ========================

    public LiveData<List<ShiftGroupSchedule>> getSchedulesByGroupIdLive(long groupId) {
        return scheduleDao.getByGroupIdLive(groupId);
    }

    public List<ShiftGroupSchedule> getSchedulesByGroupIdSync(long groupId) {
        return scheduleDao.getByGroupId(groupId);
    }

    public void insertSchedules(List<ShiftGroupSchedule> schedules) {
        AppDatabase.databaseWriteExecutor.execute(() -> scheduleDao.insertAll(schedules));
    }

    public void deleteSchedulesByGroupId(long groupId) {
        AppDatabase.databaseWriteExecutor.execute(() -> scheduleDao.deleteByGroupId(groupId));
    }

    public void replaceSchedules(long groupId, List<ShiftGroupSchedule> newSchedules) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            scheduleDao.deleteByGroupId(groupId);
            scheduleDao.insertAll(newSchedules);
        });
    }
}
