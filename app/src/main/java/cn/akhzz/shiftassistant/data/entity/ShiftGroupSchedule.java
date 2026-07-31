package cn.akhzz.shiftassistant.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "shift_group_schedule",
        foreignKeys = {
                @ForeignKey(entity = ShiftGroup.class,
                        parentColumns = "id",
                        childColumns = "groupId",
                        onDelete = ForeignKey.CASCADE)
        },
        indices = {
                @Index(value = {"groupId", "dayIndex"}, unique = true)
        })
public class ShiftGroupSchedule {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long groupId;       // FK to ShiftGroup
    public int dayIndex;       // 0-based index within the cycle
    public long shiftTypeId;   // FK to ShiftType, -1 means rest day

    public ShiftGroupSchedule() {}
}
