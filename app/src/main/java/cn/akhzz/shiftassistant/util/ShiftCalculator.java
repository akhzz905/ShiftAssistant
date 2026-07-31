package cn.akhzz.shiftassistant.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import cn.akhzz.shiftassistant.data.entity.ShiftGroup;
import cn.akhzz.shiftassistant.data.entity.ShiftGroupSchedule;
import cn.akhzz.shiftassistant.data.entity.ShiftType;

public class ShiftCalculator {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Calculate the cycle day index for a given date and group.
     *
     * @param group the shift group
     * @param date  the target date
     * @return 0-based index within the cycle
     */
    public static int getDayIndexForDate(ShiftGroup group, LocalDate date) {
        LocalDate anchor = LocalDate.parse(group.anchorDate, DATE_FORMAT);
        long daysDiff = ChronoUnit.DAYS.between(anchor, date);
        int cycleDays = group.cycleDays;
        // Calculate offset, handling negative values correctly
        long rawOffset = daysDiff % cycleDays;
        if (rawOffset < 0) rawOffset += cycleDays;
        return (int) ((group.anchorDayIndex + rawOffset) % cycleDays);
    }

    /**
     * Get the ShiftType for a given date and group.
     *
     * @param group     the shift group
     * @param date      the target date
     * @param schedules the group's schedule entries
     * @param types     all available shift types
     * @return the ShiftType, or null if rest day or no schedule
     */
    public static ShiftType getShiftForDate(ShiftGroup group, LocalDate date,
                                             List<ShiftGroupSchedule> schedules,
                                             List<ShiftType> types) {
        if (group.anchorDate == null || schedules == null || types == null) {
            return null;
        }

        int dayIndex = getDayIndexForDate(group, date);

        // Find schedule entry for this day index
        for (ShiftGroupSchedule schedule : schedules) {
            if (schedule.dayIndex == dayIndex) {
                if (schedule.shiftTypeId <= 0) {
                    return null; // rest day
                }
                // Find the shift type by ID
                for (ShiftType type : types) {
                    if (type.id == schedule.shiftTypeId) {
                        return type;
                    }
                }
                return null; // shift type not found (deleted?)
            }
        }
        return null; // no schedule entry for this day
    }
}
