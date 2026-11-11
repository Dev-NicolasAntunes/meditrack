package com.meditrack.medication;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class MedicationUi {
    private MedicationUi() {
    }

    public static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static LinearLayout vertical(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(context, 20), dp(context, 20), dp(context, 20), dp(context, 20));
        layout.setBackgroundColor(Color.rgb(247, 249, 250));
        return layout;
    }

    public static TextView heading(Context context, String value) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(24);
        view.setTextColor(Color.rgb(25, 47, 63));
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    public static TextView body(Context context, String value) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(16);
        view.setTextColor(Color.rgb(57, 72, 84));
        return view;
    }

    public static Button button(Context context, String value) {
        Button button = new Button(context);
        button.setText(value);
        button.setAllCaps(false);
        return button;
    }

    public static LinearLayout.LayoutParams fullWidth(Context context, int topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(context, topMargin);
        return params;
    }

    public static GradientDrawable roundedBackground(Context context, int color) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(context, 12));
        return background;
    }

    public static void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
