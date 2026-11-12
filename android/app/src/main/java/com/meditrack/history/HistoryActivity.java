// AI assistance: written with help from Claude (Anthropic); reviewed and adapted by Dimitri.
package com.meditrack.history;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.meditrack.medication.MedicationRepository;
import com.meditrack.medication.MedicationUi;

import java.util.List;

/** Histórico de uso com filtro por dia, semana e mês (pelo horário previsto da dose). */
public class HistoryActivity extends Activity {
    private final HistoryRepository repository = new HistoryRepository();
    private String period = HistoryRepository.PERIOD_DAY;
    private LinearLayout list;
    private TextView summary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Histórico");

        LinearLayout root = MedicationUi.vertical(this);
        root.addView(MedicationUi.heading(this, "Histórico de uso"));
        TextView subtitle = MedicationUi.body(this, "Doses tomadas, adiadas e esquecidas.");
        subtitle.setTextSize(14);
        root.addView(subtitle, MedicationUi.fullWidth(this, 6));

        root.addView(HistoryUi.periodSelector(this, period, selected -> {
            period = selected;
            load();
        }), MedicationUi.fullWidth(this, 14));

        Button stats = MedicationUi.button(this, "Ver estatísticas do período");
        root.addView(stats, MedicationUi.fullWidth(this, 4));
        stats.setOnClickListener(view -> startActivity(
                new Intent(this, StatsActivity.class).putExtra(StatsActivity.EXTRA_PERIOD, period)));

        summary = MedicationUi.body(this, "");
        summary.setTextSize(14);
        root.addView(summary, MedicationUi.fullWidth(this, 12));

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list, MedicationUi.fullWidth(this, 4));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        summary.setText("Carregando…");
        list.removeAllViews();
        String requested = period;
        repository.findByPeriod(requested, new MedicationRepository.Callback<List<MedicationHistory>>() {
            @Override
            public void onSuccess(List<MedicationHistory> entries) {
                if (!requested.equals(period)) {
                    return;
                }
                render(entries);
            }

            @Override
            public void onError(String message) {
                summary.setText("Não foi possível carregar o histórico. " + message);
            }
        });
    }

    private void render(List<MedicationHistory> entries) {
        list.removeAllViews();
        summary.setText(entries.isEmpty()
                ? "Nenhum registro neste período."
                : entries.size() + (entries.size() == 1 ? " registro" : " registros"));
        for (MedicationHistory entry : entries) {
            LinearLayout card = HistoryUi.card(this);
            TextView name = MedicationUi.body(this, entry.getMedicationName() + " · " + entry.getDosage());
            name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            card.addView(name);
            TextView times = MedicationUi.body(this, "Previsto: " + DoseTimes.display(entry.getScheduledAt())
                    + "   Ação: " + DoseTimes.display(entry.getActionAt()));
            times.setTextSize(14);
            card.addView(times, MedicationUi.fullWidth(this, 4));
            card.addView(HistoryUi.statusLabel(this, entry.getStatus()), MedicationUi.fullWidth(this, 4));
            list.addView(card, MedicationUi.fullWidth(this, 10));
        }
    }
}
