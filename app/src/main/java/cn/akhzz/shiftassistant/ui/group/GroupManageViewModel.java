package cn.akhzz.shiftassistant.ui.group;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import cn.akhzz.shiftassistant.data.entity.ShiftGroup;
import cn.akhzz.shiftassistant.data.repository.ShiftRepository;

public class GroupManageViewModel extends AndroidViewModel {

    private final ShiftRepository repository;
    private final LiveData<List<ShiftGroup>> allGroups;

    public GroupManageViewModel(@NonNull Application application) {
        super(application);
        repository = new ShiftRepository(application);
        allGroups = repository.getAllGroupsLive();
    }

    public LiveData<List<ShiftGroup>> getAllGroups() {
        return allGroups;
    }

    public void deleteGroup(ShiftGroup group) {
        repository.deleteGroup(group);
    }

    public void toggleDefault(ShiftGroup group) {
        if (group.isDefault) {
            // Clear default
            repository.setDefaultGroup(-1);
        } else {
            repository.setDefaultGroup(group.id);
        }
    }
}
