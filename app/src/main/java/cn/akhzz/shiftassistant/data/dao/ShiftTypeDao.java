package cn.akhzz.shiftassistant.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import cn.akhzz.shiftassistant.data.entity.ShiftType;

@Dao
public interface ShiftTypeDao {

    @Query("SELECT * FROM shift_type ORDER BY startDayOffset ASC, startTimeMinutes ASC")
    LiveData<List<ShiftType>> getAllLive();

    @Query("SELECT * FROM shift_type ORDER BY startDayOffset ASC, startTimeMinutes ASC")
    List<ShiftType> getAll();

    @Query("SELECT * FROM shift_type WHERE id = :id")
    ShiftType getById(long id);

    @Insert
    long insert(ShiftType shiftType);

    @Update
    void update(ShiftType shiftType);

    @Delete
    void delete(ShiftType shiftType);

    @Query("SELECT COUNT(*) FROM shift_type")
    int getCount();
}
