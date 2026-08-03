package cn.akhzz.shiftassistant.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "shift_type")
public class ShiftType {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;
    public int startTimeMinutes; // 0-1439, e.g. 480 = 08:00
    public int endTimeMinutes;   // 0-1439, e.g. 960 = 16:00
    public int startDayOffset;   // -1 = previous day, 0 = current day
    public int endDayOffset;     // 0 = current day, 1 = next day
    public int color;            // Color for the shift type

    public ShiftType() {}

    @androidx.room.Ignore
    public ShiftType(String name, int startTimeMinutes, int endTimeMinutes,
                     int startDayOffset, int endDayOffset, int color) {
        this.name = name;
        this.startTimeMinutes = startTimeMinutes;
        this.endTimeMinutes = endTimeMinutes;
        this.startDayOffset = startDayOffset;
        this.endDayOffset = endDayOffset;
        this.color = color;
    }

    public String getStartTimeString() {
        return String.format("%02d:%02d", startTimeMinutes / 60, startTimeMinutes % 60);
    }

    public String getEndTimeString() {
        return String.format("%02d:%02d", endTimeMinutes / 60, endTimeMinutes % 60);
    }

    public String getFullTimeDescription() {
        StringBuilder sb = new StringBuilder();
        if (startDayOffset == -1) {
            sb.append("前一日");
        }
        sb.append(getStartTimeString());
        sb.append(" — ");
        if (endDayOffset == 1) {
            sb.append("后一日");
        }
        sb.append(getEndTimeString());
        return sb.toString();
    }

    public boolean isCrossDay() {
        return startDayOffset != 0 || endDayOffset != 0;
    }

    /**
     * Get the effective start minutes for sorting purposes.
     * Considers day offset: previous day shifts have lower effective time.
     */
    public int getEffectiveStartMinutes() {
        return startDayOffset * 1440 + startTimeMinutes;
    }
}
