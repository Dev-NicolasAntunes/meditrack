// AI assistance: written with help from Claude (Anthropic); reviewed and adapted by Dimitri.
package com.meditrack.reminder;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Os alarmes do AlarmManager são apagados ao reiniciar o aparelho, ao atualizar o app ou ao mudar
 * a permissão de alarme exato; aqui eles são agendados de novo a partir da cópia local.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED.equals(action)) {
            ReminderManager.rescheduleAll(context);
        }
    }
}
