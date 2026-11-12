// AI assistance: written with help from Claude (Anthropic); reviewed and adapted by Dimitri.
package com.meditrack.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Ações da notificação: Tomei, Adiar 10 min e Não tomei. */
public class DoseActionReceiver extends BroadcastReceiver {
    public static final String ACTION_TAKEN = "com.meditrack.reminder.ACTION_TAKEN";
    public static final String ACTION_SNOOZE = "com.meditrack.reminder.ACTION_SNOOZE";
    public static final String ACTION_MISSED = "com.meditrack.reminder.ACTION_MISSED";

    @Override
    public void onReceive(Context context, Intent intent) {
        Reminder reminder = Reminder.fromIntent(intent);
        DoseActions.Action action = toAction(intent.getAction());
        if (reminder == null || action == null) {
            return;
        }
        // A chamada ao backend é feita fora da thread principal; goAsync mantém o receiver vivo.
        PendingResult pending = goAsync();
        DoseActions.perform(context, reminder, action, (result, error) -> pending.finish());
    }

    private static DoseActions.Action toAction(String value) {
        if (ACTION_TAKEN.equals(value)) {
            return DoseActions.Action.TAKEN;
        }
        if (ACTION_SNOOZE.equals(value)) {
            return DoseActions.Action.SNOOZE;
        }
        if (ACTION_MISSED.equals(value)) {
            return DoseActions.Action.MISSED;
        }
        return null;
    }
}
