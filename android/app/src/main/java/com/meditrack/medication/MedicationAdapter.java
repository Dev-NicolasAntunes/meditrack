package com.meditrack.medication;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MedicationAdapter extends BaseAdapter {
    public interface MedicationActions {
        void onEdit(Medication medication);
        void onDelete(Medication medication);
    }
    private final Context context;
    private final MedicationActions actions;
    private final List<Medication> medications = new ArrayList<>();

    public MedicationAdapter(Context context, MedicationActions actions) {
        this.context = context;
        this.actions = actions;
    }

    public void replaceAll(List<Medication> values) {
        medications.clear();
        medications.addAll(values);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() { return medications.size(); }

    @Override
    public Medication getItem(int position) { return medications.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View recycled, ViewGroup parent) {
        Medication medication = getItem(position);
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(cardBackground());
        card.setElevation(dp(2));

        TextView title = text(medication.getName(), 18, Color.rgb(24, 40, 56), true);
        TextView dosage = text(medication.getDosage() + " · " + medication.getFrequency(),
                14, Color.rgb(92, 105, 118), false);
        TextView stock = text("Estoque: " + medication.getQuantity() + " unidade(s)",
                14, Color.rgb(37, 65, 82), true);
        TextView status = text(
                medication.isLowStock()
                        ? "Estoque baixo · reponha em breve"
                        : "Previsão: " + medication.getEstimatedDaysRemaining() + " dia(s)",
                13,
                medication.isLowStock() ? Color.rgb(174, 63, 42) : Color.rgb(52, 112, 84),
                true
        );

        card.addView(title);
        card.addView(dosage, params(0, 6));
        card.addView(stock, params(0, 10));
        card.addView(status);
        LinearLayout actionsRow = new LinearLayout(context);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setGravity(Gravity.CENTER_VERTICAL);
        actionsRow.setWeightSum(2f);
        card.addView(actionsRow, params(0, 8));

        Button edit = new Button(context);
        edit.setText("Editar");
        edit.setAllCaps(false);
        actionsRow.addView(edit, new LinearLayout.LayoutParams(0, dp(48), 1f));
        edit.setOnClickListener(view -> actions.onEdit(medication));
        Button delete = new Button(context);
        delete.setText("Excluir");
        delete.setAllCaps(false);
        delete.setTextColor(Color.rgb(174, 63, 42));
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        deleteParams.leftMargin = dp(8);
        actionsRow.addView(delete, deleteParams);
        delete.setOnClickListener(view -> actions.onDelete(medication));
        edit.setFocusable(false);
        delete.setFocusable(false);
        card.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        return card;
    }

    private GradientDrawable cardBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(12));
        background.setStroke(dp(1), Color.rgb(224, 231, 235));
        return background;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(sp);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        view.setGravity(Gravity.START);
        return view;
    }

    private LinearLayout.LayoutParams params(int width, int topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(topMargin);
        return params;
    }

    private int dp(int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
