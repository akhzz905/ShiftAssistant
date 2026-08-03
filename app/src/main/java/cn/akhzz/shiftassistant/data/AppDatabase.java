package cn.akhzz.shiftassistant.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.akhzz.shiftassistant.data.dao.ShiftGroupDao;
import cn.akhzz.shiftassistant.data.dao.ShiftGroupScheduleDao;
import cn.akhzz.shiftassistant.data.dao.ShiftTypeDao;
import cn.akhzz.shiftassistant.data.entity.ShiftGroup;
import cn.akhzz.shiftassistant.data.entity.ShiftGroupSchedule;
import cn.akhzz.shiftassistant.data.entity.ShiftType;

import androidx.room.migration.Migration;

@Database(entities = {ShiftType.class, ShiftGroup.class, ShiftGroupSchedule.class},
        version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract ShiftTypeDao shiftTypeDao();
    public abstract ShiftGroupDao shiftGroupDao();
    public abstract ShiftGroupScheduleDao shiftGroupScheduleDao();

    private static volatile AppDatabase INSTANCE;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(4);

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE shift_type ADD COLUMN color INTEGER NOT NULL DEFAULT " + 0xFF1976D2);
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "shift_assistant_db")
                            .addMigrations(MIGRATION_1_2)
                            .addCallback(sRoomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Callback to insert default shift types when the database is first created.
     * Default types: 白班 (08:00-16:00), 中班 (15:00-23:30), 夜班 (前一日23:30-08:00)
     */
    private static final RoomDatabase.Callback sRoomDatabaseCallback =
            new RoomDatabase.Callback() {
                @Override
                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                    super.onCreate(db);
                    databaseWriteExecutor.execute(() -> {
                        ShiftTypeDao dao = INSTANCE.shiftTypeDao();
                        // 白班: 08:00 - 16:00, same day
                        dao.insert(new ShiftType("白班", 480, 960, 0, 0, cn.akhzz.shiftassistant.util.ColorUtils.GROUP_COLORS[1])); // Blue
                        // 中班: 15:00 - 23:30, same day
                        dao.insert(new ShiftType("中班", 900, 1410, 0, 0, cn.akhzz.shiftassistant.util.ColorUtils.GROUP_COLORS[2])); // Orange
                        // 夜班: previous day 23:30 - current day 08:00
                        dao.insert(new ShiftType("夜班", 1410, 480, -1, 0, cn.akhzz.shiftassistant.util.ColorUtils.GROUP_COLORS[3])); // Purple
                    });
                }
            };
}
