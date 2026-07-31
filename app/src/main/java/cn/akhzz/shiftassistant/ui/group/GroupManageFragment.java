package cn.akhzz.shiftassistant.ui.group;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import cn.akhzz.shiftassistant.R;
import cn.akhzz.shiftassistant.data.entity.ShiftGroup;

public class GroupManageFragment extends Fragment {

    private GroupManageViewModel viewModel;
    private GroupListAdapter adapter;
    private TextView tvEmpty;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_group_manage, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(GroupManageViewModel.class);

        RecyclerView rvGroups = view.findViewById(R.id.rv_groups);
        tvEmpty = view.findViewById(R.id.tv_empty);
        FloatingActionButton fab = view.findViewById(R.id.fab_add);

        // Shift type management button
        view.findViewById(R.id.btn_shift_type_manage).setOnClickListener(v -> {
            startActivity(new Intent(getContext(), ShiftTypeManageActivity.class));
        });

        // Setup adapter
        adapter = new GroupListAdapter(
                // onClick: edit group
                group -> {
                    Intent intent = new Intent(getContext(), EditGroupActivity.class);
                    intent.putExtra("group_id", group.id);
                    startActivity(intent);
                },
                // onStarClick: toggle default
                group -> viewModel.toggleDefault(group),
                // onLongClick: delete
                group -> showDeleteDialog(group)
        );

        rvGroups.setLayoutManager(new LinearLayoutManager(getContext()));
        rvGroups.setAdapter(adapter);

        // FAB: add new group
        fab.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), EditGroupActivity.class));
        });

        // Observe groups
        viewModel.getAllGroups().observe(getViewLifecycleOwner(), groups -> {
            adapter.setData(groups);
            tvEmpty.setVisibility(groups == null || groups.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void showDeleteDialog(ShiftGroup group) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.confirm_delete)
                .setMessage(getString(R.string.confirm_delete_group, group.name))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, w) -> viewModel.deleteGroup(group))
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh when returning from edit activity
        viewModel.getAllGroups().getValue(); // trigger re-observation
    }
}
