package cn.akhzz.shiftassistant.ui.today;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import cn.akhzz.shiftassistant.MainActivity;
import cn.akhzz.shiftassistant.R;
import cn.akhzz.shiftassistant.data.entity.ShiftType;
import cn.akhzz.shiftassistant.ui.share.ImportConfirmDialog;
import cn.akhzz.shiftassistant.ui.share.PortraitCaptureActivity;
import cn.akhzz.shiftassistant.ui.share.ShareQrDialog;
import cn.akhzz.shiftassistant.util.QrShareManager;
import cn.akhzz.shiftassistant.util.ShiftCalculator;

public class TodayFragment extends Fragment {

    private TodayViewModel viewModel;
    private View contentLayout, emptyLayout, cardToday;
    private View colorBar;
    private TextView tvDate, tvEmptyDate, tvGroupName, tvShiftName, tvShiftTime, tvCycleDay;
    private RecyclerView rvAllShifts;
    private TodayShiftAdapter adapter;

    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<ScanOptions> barcodeLauncher;
    private ActivityResultLauncher<String> selectAlbumImageLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startBarcodeScan();
                    } else {
                        Toast.makeText(getContext(), R.string.camera_permission_needed, Toast.LENGTH_LONG).show();
                    }
                }
        );

        barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result != null && result.getContents() != null) {
                handleScannedQrContent(result.getContents());
            }
        });

        selectAlbumImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        handleAlbumImageSelected(uri);
                    }
                }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_today, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.inflateMenu(R.menu.menu_today);
            toolbar.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_import) {
                    showImportMethodDialog();
                    return true;
                } else if (itemId == R.id.action_share) {
                    handleShareQrClick();
                    return true;
                }
                return false;
            });
        }

        contentLayout = view.findViewById(R.id.content_layout);
        emptyLayout = view.findViewById(R.id.empty_layout);
        cardToday = view.findViewById(R.id.card_today);
        colorBar = view.findViewById(R.id.color_bar);
        tvDate = view.findViewById(R.id.tv_date);
        tvEmptyDate = view.findViewById(R.id.tv_empty_date);
        tvGroupName = view.findViewById(R.id.tv_group_name);
        tvShiftName = view.findViewById(R.id.tv_shift_name);
        tvShiftTime = view.findViewById(R.id.tv_shift_time);
        tvCycleDay = view.findViewById(R.id.tv_cycle_day);
        rvAllShifts = view.findViewById(R.id.rv_all_shifts);

        adapter = new TodayShiftAdapter();
        rvAllShifts.setLayoutManager(new LinearLayoutManager(getContext()));
        rvAllShifts.setAdapter(adapter);

        view.findViewById(R.id.btn_go_manage).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToGroupManage();
            }
        });

        viewModel = new ViewModelProvider(this).get(TodayViewModel.class);
        observeData();
    }

    private void showImportMethodDialog() {
        Context context = getContext();
        if (context == null) return;

        String[] options = new String[]{
                getString(R.string.import_option_scan),
                getString(R.string.import_option_album)
        };

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.import_option_title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        handleScanQrClick();
                    } else if (which == 1) {
                        handleSelectAlbumClick();
                    }
                })
                .show();
    }

    private void handleScanQrClick() {
        Context context = getContext();
        if (context == null) return;
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startBarcodeScan();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void handleSelectAlbumClick() {
        selectAlbumImageLauncher.launch("image/*");
    }

    private void startBarcodeScan() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("将排班二维码放入取景框内扫描");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(PortraitCaptureActivity.class);
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        barcodeLauncher.launch(options);
    }

    private void handleScannedQrContent(String content) {
        Context context = getContext();
        if (context == null) return;
        try {
            QrShareManager.ShareData shareData = QrShareManager.decodeAndDecompress(content);
            ImportConfirmDialog.show(context, shareData, () -> {
                if (viewModel != null) {
                    viewModel.refresh();
                }
            });
        } catch (Exception e) {
            Toast.makeText(context, getString(R.string.scan_failed, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    private void handleAlbumImageSelected(Uri uri) {
        Context context = getContext();
        if (context == null) return;
        try {
            String decodedContent = QrShareManager.decodeQrCodeFromUri(context, uri);
            if (decodedContent != null && !decodedContent.isEmpty()) {
                handleScannedQrContent(decodedContent);
            } else {
                Toast.makeText(context, R.string.decode_no_qr, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(context, getString(R.string.scan_failed, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    private void handleShareQrClick() {
        Context context = getContext();
        if (context == null) return;

        QrShareManager.prepareShareData(context, new QrShareManager.ShareDataCallback() {
            @Override
            public void onSuccess(String qrContent) {
                new ShareQrDialog(context, qrContent).show();
            }

            @Override
            public void onError(Throwable error) {
                Toast.makeText(context, "准备分享数据失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refresh();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && viewModel != null) {
            viewModel.refresh();
        }
    }

    private void observeData() {
        viewModel.getTodayData().observe(getViewLifecycleOwner(), data -> {
            if (data == null) return;

            contentLayout.setVisibility(View.VISIBLE);

            // Set today's date
            LocalDate today = LocalDate.now();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.CHINESE);
            String dateStr = today.format(fmt);
            if (tvDate != null) tvDate.setText(dateStr);
            if (tvEmptyDate != null) tvEmptyDate.setText(dateStr);

            if (data.defaultGroup == null) {
                if (cardToday != null) cardToday.setVisibility(View.GONE);
                emptyLayout.setVisibility(View.VISIBLE);
            } else {
                if (cardToday != null) cardToday.setVisibility(View.VISIBLE);
                emptyLayout.setVisibility(View.GONE);

                // Color bar
                GradientDrawable barBg = new GradientDrawable();
                barBg.setShape(GradientDrawable.RECTANGLE);
                barBg.setCornerRadius(4f);
                barBg.setColor(data.defaultGroup.color);
                colorBar.setBackground(barBg);

                // Group name
                tvGroupName.setText(data.defaultGroup.name);
                tvGroupName.setTextColor(data.defaultGroup.color);

                // Shift info
                ShiftType shift = data.defaultShift;
                if (shift != null) {
                    tvShiftName.setText(shift.name);
                    tvShiftTime.setText(shift.getFullTimeDescription());
                    tvShiftTime.setVisibility(View.VISIBLE);
                } else {
                    tvShiftName.setText(R.string.rest);
                    tvShiftTime.setVisibility(View.GONE);
                }

                // Cycle day info
                int dayIndex = ShiftCalculator.getDayIndexForDate(data.defaultGroup, today);
                tvCycleDay.setText(getString(R.string.cycle_day_info,
                        dayIndex + 1, data.defaultGroup.cycleDays));
            }

            // All groups list
            if (data.allGroupShifts != null) {
                adapter.setData(data.allGroupShifts);
            }
        });
    }
}
