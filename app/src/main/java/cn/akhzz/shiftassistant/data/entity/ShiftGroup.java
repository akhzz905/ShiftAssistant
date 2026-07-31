package cn.akhzz.shiftassistant.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "shift_group")
public class ShiftGroup {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;           // e.g. 甲班, 乙班
    public int cycleDays;         // cycle length in days (1-32)
    public int color;             // ARGB color int
    public String anchorDate;     // yyyy-MM-dd, the date when anchorDayIndex was set
    public int anchorDayIndex;    // 0-based index in the cycle for the anchor date
    public boolean isDefault;     // whether this is the user's default group (shown on today page)

    public ShiftGroup() {}
}
