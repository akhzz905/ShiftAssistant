package cn.akhzz.shiftassistant;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import cn.akhzz.shiftassistant.ui.calendar.CalendarFragment;
import cn.akhzz.shiftassistant.ui.group.GroupManageFragment;
import cn.akhzz.shiftassistant.ui.today.TodayFragment;

public class MainActivity extends AppCompatActivity {

    private final TodayFragment todayFragment = new TodayFragment();
    private final CalendarFragment calendarFragment = new CalendarFragment();
    private final GroupManageFragment groupManageFragment = new GroupManageFragment();
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);

        // Add all fragments, only show todayFragment initially
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, groupManageFragment, "group")
                .hide(groupManageFragment)
                .add(R.id.fragment_container, calendarFragment, "calendar")
                .hide(calendarFragment)
                .add(R.id.fragment_container, todayFragment, "today")
                .commit();
        activeFragment = todayFragment;

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selected;
            int itemId = item.getItemId();
            if (itemId == R.id.nav_today) {
                selected = todayFragment;
            } else if (itemId == R.id.nav_calendar) {
                selected = calendarFragment;
            } else if (itemId == R.id.nav_group) {
                selected = groupManageFragment;
            } else {
                return false;
            }

            if (selected != activeFragment) {
                getSupportFragmentManager().beginTransaction()
                        .hide(activeFragment)
                        .show(selected)
                        .commit();
                activeFragment = selected;
            }
            return true;
        });
    }

    /**
     * Switch to the group management tab programmatically.
     * Called from TodayFragment's empty state button.
     */
    public void switchToGroupManage() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_group);
    }
}
