package cn.akhzz.shiftassistant.ui.group;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import cn.akhzz.shiftassistant.R;
import cn.akhzz.shiftassistant.util.ColorUtils;

public class ColorPickerDialog {

    public interface OnColorSelectedListener {
        void onColorSelected(int color);
    }

    private final Dialog dialog;

    public ColorPickerDialog(Context context, OnColorSelectedListener listener) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.dialog_color_picker, null);

        GridLayout grid = view.findViewById(R.id.color_grid);
        grid.setColumnCount(4);
        grid.setRowCount(4);

        float density = context.getResources().getDisplayMetrics().density;
        int size = (int) (56 * density);
        int margin = (int) (8 * density);

        dialog = new MaterialAlertDialogBuilder(context)
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .create();

        for (int color : ColorUtils.GROUP_COLORS) {
            View colorItem = new View(context);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = size;
            params.height = size;
            params.setMargins(margin, margin, margin, margin);
            params.setGravity(Gravity.CENTER);
            colorItem.setLayoutParams(params);

            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(color);
            bg.setStroke((int) (2 * density), 0x20000000);
            colorItem.setBackground(bg);

            colorItem.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onColorSelected(color);
                }
                dialog.dismiss();
            });

            grid.addView(colorItem);
        }
    }

    public void show() {
        dialog.show();
    }
}
