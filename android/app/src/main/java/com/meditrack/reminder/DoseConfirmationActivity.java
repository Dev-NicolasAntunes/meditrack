// AI assistance: written with help from Claude (Anthropic); reviewed and adapted by Dimitri.
package com.meditrack.reminder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.meditrack.history.DoseTimes;
import com.meditrack.history.HistoryActivity;
import com.meditrack.medication.MedicationUi;

/** Abre ao tocar na notificação: confirma, adia ou marca a dose como não tomada. */
public class DoseConfirmationActivity extends Activity {
    private Reminder reminder;
    private LinearLayout content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Confirmar dose");
        content = MedicationUi.vertical(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);
        show(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        show(intent);
    }

    private void show(Intent intent) {
        reminder = Reminder.fromIntent(intent);
        if (reminder == null) {
            finish();
            return;
        }
        content.removeAllViews();
        content.addView(MedicationUi.heading(this, reminder.medicationName));
        TextView dosage = MedicationUi.body(this, reminder.dosage);
        dosage.setTextSize(18);
        content.addView(dosage, MedicationUi.fullWidth(this, 8));
        content.addView(MedicationUi.body(this, "Horário previsto: " + DoseTimes.display(reminder.scheduledAt)),
                MedicationUi.fullWidth(this, 4));
        TextView question = MedicationUi.body(this, "Você tomou esta dose?");
        content.addView(question, MedicationUi.fullWidth(this, 18));

        Button taken = MedicationUi.button(this, "Tomei");
        Button snooze = MedicationUi.button(this, "Adiar 10 min");
        Button missed = MedicationUi.button(this, "Não tomei");
        content.addView(taken, MedicationUi.fullWidth(this, 12));
        content.addView(snooze, MedicationUi.fullWidth(this, 4));
        content.addView(missed, MedicationUi.fullWidth(this, 4));
        taken.setOnClickListener(view -> act(DoseActions.Action.TAKEN, taken, snooze, missed));
        snooze.setOnClickListener(view -> act(DoseActions.Action.SNOOZE, taken, snooze, missed));
        missed.setOnClickListener(view -> act(DoseActions.Action.MISSED, taken, snooze, missed));
    }

    private void act(DoseActions.Action action, Button... buttons) {
        for (Button button : buttons) {
            button.setEnabled(false);
        }
        DoseActions.perform(this, reminder, action, (result, error) -> {
            if (isFinishing()) {
                return;
            }
            if (result == null) {
                for (Button button : buttons) {
                    button.setEnabled(true);
                }
                MedicationUi.toast(this, "Não foi possível registrar: " + error);
                return;
            }
            MedicationUi.toast(this, DoseActions.describe(result, action));
            startActivity(new Intent(this, HistoryActivity.class));
            finish();
        });
    }
}
