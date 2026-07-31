package cn.akhzz.shiftassistant.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import cn.akhzz.shiftassistant.data.entity.ShiftGroupSchedule;

@Dao
public interface ShiftGroupScheduleDao {

    @Query("SELECT * FROM shift_group_schedule WHERE groupId = :groupId ORDER BY dayIndex ASC")
    LiveData<List<ShiftGroupSchedule>> getByGroupIdLive(long groupId);

    @Query("SELECT * FROM shift_group_schedule WHERE groupId = :groupId ORDER BY dayIndex ASC")
    List<ShiftGroupSchedule> getByGroupId(long groupId);

    @Query("SELECT * FROM shift_group_schedule WHERE groupId = :groupId AND dayIndex = :dayIndex")
    ShiftGroupSchedule getByGroupAndDay(long groupId, int dayIndex);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ShiftGroupSchedule schedule);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ShiftGroupSchedule> schedules);

    @Update
    void update(ShiftGroupSchedule schedule);

    @Delete
    void delete(ShiftGroupSchedule schedule);

    @Query("DELETE FROM shift_group_schedule WHERE groupId = :groupId")
    void deleteByGroupId(long groupId);
}
