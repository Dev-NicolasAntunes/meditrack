// AI assistance: written with help from Claude (Anthropic); reviewed and adapted by Dimitri.
package com.meditrack.reminder;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.os.Build;

import com.meditrack.history.DoseTimes;
import com.meditrack.medication.Medication;
import com.meditrack.medication.MedicationDetailsActivity;

/** Canais e notificações de lembrete de dose e de estoque baixo (APIs nativas, sem AndroidX). */
public final class NotificationHelper {
    private static final String CHANNEL_REMINDERS = "dose_reminders";
    private static final String CHANNEL_STOCK = "stock_alerts";

    private NotificationHelper() {
    }

    public static void ensureChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel reminders = new NotificationChannel(CHANNEL_REMINDERS,
                "Lembretes de dose", NotificationManager.IMPORTANCE_HIGH);
        reminders.setDescription("Avisa no horário de cada dose cadastrada.");
        NotificationChannel stock = new NotificationChannel(CHANNEL_STOCK,
                "Estoque baixo", NotificationManager.IMPORTANCE_DEFAULT);
        stock.setDescription("Avisa quando um medicamento está acabando.");
        manager.createNotificationChannel(reminders);
        manager.createNotificationChannel(stock);
    }

    public static boolean canPostNotifications(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        return manager != null && manager.areNotificationsEnabled();
    }

    public static void showReminder(Context context, Reminder reminder) {
        showReminder(context, reminder, null);
    }

    /** Notificação da dose com as ações Tomei, Adiar 10 min e Não tomei. */
    public static void showReminder(Context context, Reminder reminder, String errorMessage) {
        if (!canPostNotifications(context)) {
            return;
        }
        ensureChannels(context);
        String text = reminder.dosage + " · previsto para " + DoseTimes.hour(reminder.scheduledAt);
        if (errorMessage != null) {
            text = "Não foi possível registrar: " + errorMessage;
        }
        Intent open = reminder.putInto(new Intent(context, DoseConfirmationActivity.class))
                .setData(reminder.doseUri("open"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content = PendingIntent.getActivity(context, reminder.notificationId(), open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder = builder(context, CHANNEL_REMINDERS)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("Hora do remédio: " + reminder.medicationName)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setCategory(Notification.CATEGORY_REMINDER)
                .setContentIntent(content)
                .setAutoCancel(true)
                .setShowWhen(true)
                .addAction(action(context, reminder, DoseActionReceiver.ACTION_TAKEN, "Tomei"))
                .addAction(action(context, reminder, DoseActionReceiver.ACTION_SNOOZE, "Adiar 10 min"))
                .addAction(action(context, reminder, DoseActionReceiver.ACTION_MISSED, "Não tomei"));
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setPriority(Notification.PRIORITY_HIGH).setDefaults(Notification.DEFAULT_ALL);
        }
        context.getSystemService(NotificationManager.class).notify(reminder.notificationId(), builder.build());
    }

    public static void cancelReminder(Context context, Reminder reminder) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.cancel(reminder.notificationId());
        }
    }

    /** Alerta de estoque baixo, usando o limite definido no cadastro do medicamento. */
    public static void showLowStock(Context context, Medication medication) {
        if (!canPostNotifications(context)) {
            return;
        }
        ensureChannels(context);
        Intent details = new Intent(context, MedicationDetailsActivity.class)
                .putExtra(MedicationDetailsActivity.EXTRA_ID, medication.getId())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        int id = ("stock|" + medication.getId()).hashCode();
        PendingIntent content = PendingIntent.getActivity(context, id, details,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String text = "Restam " + medication.getQuantity() + " unidade(s) de " + medication.getName()
                + ". Reponha em breve.";
        Notification notification = builder(context, CHANNEL_STOCK)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle("Estoque baixo")
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(content)
                .setAutoCancel(true)
                .build();
        context.getSystemService(NotificationManager.class).notify(id, notification);
    }

    private static Notification.Action action(Context context, Reminder reminder, String action, String title) {
        Intent intent = reminder.putInto(new Intent(context, DoseActionReceiver.class))
                .setAction(action)
                .setData(reminder.doseUri(action));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, reminder.notificationId(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Action.Builder(
                Icon.createWithResource(context, android.R.drawable.ic_popup_reminder), title, pendingIntent)
                .build();
    }

    @SuppressWarnings("deprecation")
    private static Notification.Builder builder(Context context, String channel) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, channel)
                : new Notification.Builder(context);
    }
}
