// AI assistance: written with help from Claude (Anthropic); reviewed and adapted by Dimitri.
package com.meditrack.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.meditrack.medication.Medication;

/** Recebe o alarme, mostra a notificação da dose e agenda a próxima ocorrência. */
public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ReminderManager.ACTION_REMINDER.equals(intent.getAction())) {
            return;
        }
        Reminder reminder = Reminder.fromIntent(intent);
        if (reminder == null) {
            return;
        }
        Medication medication = ReminderManager.cached(context, reminder.medicationId);
        if (medication == null) {
            return; // Medicamento excluído depois do agendamento.
        }
        if (reminder.kind == Reminder.Kind.GRID && !medication.getDoseTimes().contains(reminder.doseTime)) {
            return; // Horário removido na edição.
        }
        NotificationHelper.showReminder(context,
                reminder.withDetails(medication.getName(), medication.getDosage()));
        ReminderManager.scheduleNext(context, reminder);
    }
}
