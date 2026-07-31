package cn.akhzz.shiftassistant.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import cn.akhzz.shiftassistant.data.entity.ShiftGroup;

@Dao
public interface ShiftGroupDao {

    @Query("SELECT * FROM shift_group ORDER BY id ASC")
    LiveData<List<ShiftGroup>> getAllLive();

    @Query("SELECT * FROM shift_group ORDER BY id ASC")
    List<ShiftGroup> getAll();

    @Query("SELECT * FROM shift_group WHERE id = :id")
    ShiftGroup getById(long id);

    @Query("SELECT * FROM shift_group WHERE isDefault = 1 LIMIT 1")
    ShiftGroup getDefault();

    @Query("SELECT * FROM shift_group WHERE isDefault = 1 LIMIT 1")
    LiveData<ShiftGroup> getDefaultLive();

    @Insert
    long insert(ShiftGroup group);

    @Update
    void update(ShiftGroup group);

    @Delete
    void delete(ShiftGroup group);

    @Query("UPDATE shift_group SET isDefault = 0")
    void clearAllDefault();

    @Query("UPDATE shift_group SET isDefault = 1 WHERE id = :id")
    void setDefault(long id);
}
