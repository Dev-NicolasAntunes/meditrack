// AI assistance: written with help from Claude (Anthropic); reviewed and adapted by Dimitri.
package com.meditrack.history;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meditrack.medication.MedicationUi;

/** Componentes compartilhados das telas de histórico, no mesmo visual do MedicationUi. */
public final class HistoryUi {
    public interface PeriodListener {
        void onPeriodSelected(String period);
    }

    private static final String[] PERIODS = {
            HistoryRepository.PERIOD_DAY, HistoryRepository.PERIOD_WEEK, HistoryRepository.PERIOD_MONTH
    };
    private static final String[] LABELS = {"Dia", "Semana", "Mês"};

    private HistoryUi() {
    }

    /** Linha com os filtros Dia, Semana e Mês. */
    public static LinearLayout periodSelector(Context context, String initial, PeriodListener listener) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button[] buttons = new Button[PERIODS.length];
        for (int i = 0; i < PERIODS.length; i++) {
            Button button = MedicationUi.button(context, LABELS[i]);
            buttons[i] = button;
            String period = PERIODS[i];
            button.setOnClickListener(view -> {
                highlight(buttons, period);
                listener.onPeriodSelected(period);
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            if (i > 0) {
                params.leftMargin = MedicationUi.dp(context, 6);
            }
            row.addView(button, params);
        }
        highlight(buttons, initial);
        return row;
    }

    private static void highlight(Button[] buttons, String selected) {
        for (int i = 0; i < buttons.length; i++) {
            boolean active = PERIODS[i].equals(selected);
            buttons[i].setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL);
            buttons[i].setTextColor(active ? Color.rgb(25, 47, 63) : Color.rgb(110, 122, 132));
        }
    }

    public static LinearLayout card(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        int padding = MedicationUi.dp(context, 14);
        card.setPadding(padding, padding, padding, padding);
        card.setBackground(MedicationUi.roundedBackground(context, Color.WHITE));
        return card;
    }

    public static TextView statusLabel(Context context, DoseStatus status) {
        TextView view = MedicationUi.body(context, status.getLabel());
        view.setTextColor(status.getColor());
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setTextSize(14);
        return view;
    }

    public static TextView section(Context context, String title) {
        TextView text = MedicationUi.heading(context, title);
        text.setTextSize(17);
        text.setPadding(0, MedicationUi.dp(context, 18), 0, MedicationUi.dp(context, 2));
        return text;
    }
}
