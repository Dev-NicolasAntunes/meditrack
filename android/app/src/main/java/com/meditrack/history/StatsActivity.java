// AI assistance: written with help from Claude (Anthropic); reviewed and adapted by Dimitri.
package com.meditrack.history;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.meditrack.medication.MedicationRepository;
import com.meditrack.medication.MedicationUi;

/** Percentual de adesão e quantidade de doses por status no período escolhido. */
public class StatsActivity extends Activity {
    public static final String EXTRA_PERIOD = "com.meditrack.history.PERIOD";
    /** Data (AAAA-MM-DD) dentro do período a exibir; sem ela, mostra o período atual. */
    public static final String EXTRA_REFERENCE = "com.meditrack.history.REFERENCE";

    private static final int NO_RESPONSE_COLOR = Color.rgb(110, 122, 132);

    private final HistoryRepository repository = new HistoryRepository();
    private HistoryPeriod period;
    private LinearLayout content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Estatísticas");
        String type = getIntent().getStringExtra(EXTRA_PERIOD);
        if (type == null) {
            type = HistoryRepository.PERIOD_WEEK;
        }
        period = HistoryPeriod.of(type, getIntent().getStringExtra(EXTRA_REFERENCE));

        LinearLayout root = MedicationUi.vertical(this);
        root.addView(MedicationUi.heading(this, "Estatísticas"));
        root.addView(HistoryUi.periodSelector(this, period.getType(), selected -> {
            period = period.withType(selected);
            load();
        }), MedicationUi.fullWidth(this, 14));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content, MedicationUi.fullWidth(this, 8));

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
        content.removeAllViews();
        content.addView(MedicationUi.body(this, "Carregando…"));
        HistoryPeriod requested = period;
        repository.stats(requested, new MedicationRepository.Callback<HistoryStats>() {
            @Override
            public void onSuccess(HistoryStats stats) {
                if (requested == period) {
                    render(stats);
                }
            }

            @Override
            public void onError(String message) {
                if (requested != period) {
                    return;
                }
                content.removeAllViews();
                content.addView(MedicationUi.body(StatsActivity.this,
                        "Não foi possível carregar as estatísticas. " + message));
            }
        });
    }

    private void render(HistoryStats stats) {
        content.removeAllViews();
        TextView range = MedicationUi.body(this, stats.startDate.equals(stats.endDate)
                ? "Período: " + formatDate(stats.startDate)
                : "Período: " + formatDate(stats.startDate) + " – " + formatDate(stats.endDate));
        range.setTextSize(14);
        content.addView(range);

        LinearLayout card = HistoryUi.card(this);
        TextView percent = MedicationUi.heading(this, stats.adherencePercent + "%");
        percent.setTextSize(40);
        card.addView(percent);
        card.addView(MedicationUi.body(this, "de adesão ao tratamento"));
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(stats.adherencePercent);
        card.addView(bar, MedicationUi.fullWidth(this, 10));
        TextView detail = MedicationUi.body(this, stats.takenDoses + " de " + stats.expectedDoses
                + " dose(s) prevista(s) foram tomadas.");
        detail.setTextSize(14);
        card.addView(detail, MedicationUi.fullWidth(this, 6));
        content.addView(card, MedicationUi.fullWidth(this, 12));

        content.addView(HistoryUi.section(this, "Doses no período"));
        addCount(DoseStatus.TOMADO, stats.tomado);
        addCount(DoseStatus.ATRASADO, stats.atrasado);
        addCount(DoseStatus.ADIADO, stats.adiado);
        addCount(DoseStatus.NAO_TOMADO, stats.naoTomado);
        addCount("Sem resposta", NO_RESPONSE_COLOR, stats.semResposta);

        TextView note = MedicationUi.body(this, "Doses previstas incluem os horários cadastrados até agora "
                + "e as doses registradas fora deles. Doses adiadas e depois tomadas contam como tomadas. "
                + "\"Sem resposta\" são doses previstas sem nenhum registro.");
        note.setTextSize(13);
        content.addView(note, MedicationUi.fullWidth(this, 16));
    }

    private void addCount(DoseStatus status, int count) {
        addCount(status.getLabel(), status.getColor(), count);
    }

    private void addCount(String text, int color, int count) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView label = MedicationUi.body(this, text);
        label.setTextColor(color);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setTextSize(14);
        row.addView(label, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView value = MedicationUi.body(this, String.valueOf(count));
        value.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        row.addView(value);
        content.addView(row, MedicationUi.fullWidth(this, 8));
    }

    private String formatDate(String isoDate) {
        String[] parts = isoDate.split("-");
        return parts.length == 3 ? parts[2] + "/" + parts[1] : isoDate;
    }
}
