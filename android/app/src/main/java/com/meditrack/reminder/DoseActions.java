// AI assistance: written with help from Claude (Anthropic); reviewed and adapted by Dimitri.
package com.meditrack.reminder;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.meditrack.history.DoseRecordResult;
import com.meditrack.history.DoseStatus;
import com.meditrack.history.DoseTimes;
import com.meditrack.history.HistoryApiClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Trata as respostas a um lembrete (da notificação, da tela de confirmação ou da tela inicial):
 * grava no histórico, o backend desconta o estoque (confirmDose do módulo de medicamentos) e,
 * se o estoque ficar baixo, mostra o alerta.
 */
public final class DoseActions {
    public enum Action { TAKEN, SNOOZE, MISSED }

    public interface Listener {
        /** Chamado na thread principal. result é null quando houve erro. */
        void onDone(DoseRecordResult result, String errorMessage);
    }

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private DoseActions() {
    }

    public static void perform(Context context, Reminder reminder, Action action, Listener listener) {
        Context app = context.getApplicationContext();
        IO.execute(() -> {
            DoseRecordResult result = null;
            String error = null;
            try {
                result = apply(app, reminder, action);
            } catch (Exception exception) {
                error = exception.getMessage() == null || exception.getMessage().trim().isEmpty()
                        ? "Não foi possível se comunicar com o servidor."
                        : exception.getMessage();
                if (action != Action.SNOOZE) {
                    // Mantém a notificação com as ações para o usuário tentar de novo.
                    NotificationHelper.showReminder(app, reminder, error);
                }
            }
            DoseRecordResult finalResult = result;
            String finalError = error;
            MAIN.post(() -> listener.onDone(finalResult, finalError));
        });
    }

    private static DoseRecordResult apply(Context context, Reminder reminder, Action action) throws Exception {
        String actionAt = DoseTimes.format(DoseTimes.now());
        HistoryApiClient api = new HistoryApiClient();
        DoseRecordResult result;
        switch (action) {
            case TAKEN:
                // O backend registra ATRASADO se passar de 30 min do horário previsto.
                result = api.record(reminder.medicationId, reminder.scheduledAt, actionAt, DoseStatus.TOMADO);
                NotificationHelper.cancelReminder(context, reminder);
                if (result.medication != null && result.medication.isLowStock()) {
                    NotificationHelper.showLowStock(context, result.medication);
                }
                return result;
            case SNOOZE:
                // Reagenda antes de gravar: o lembrete volta mesmo se o backend estiver fora do ar.
                NotificationHelper.cancelReminder(context, reminder);
                ReminderManager.snooze(context, reminder);
                return api.record(reminder.medicationId, reminder.scheduledAt, actionAt, DoseStatus.ADIADO);
            case MISSED:
            default:
                result = api.record(reminder.medicationId, reminder.scheduledAt, actionAt, DoseStatus.NAO_TOMADO);
                NotificationHelper.cancelReminder(context, reminder);
                return result;
        }
    }

    /** Mensagem curta para mostrar ao usuário depois da ação. */
    public static String describe(DoseRecordResult result, Action action) {
        if (result.duplicate) {
            return "Essa dose já estava registrada como tomada.";
        }
        String message;
        switch (action) {
            case TAKEN:
                message = result.history.getStatus() == DoseStatus.ATRASADO
                        ? "Dose registrada com atraso." : "Dose registrada.";
                if (result.medication != null) {
                    message += " Restam " + result.medication.getQuantity() + " unidade(s).";
                    if (result.medication.isLowStock()) {
                        message += " Atenção: estoque baixo.";
                    }
                }
                return message;
            case SNOOZE:
                return "Lembrete adiado por 10 minutos.";
            case MISSED:
            default:
                return "Dose marcada como não tomada.";
        }
    }
}
