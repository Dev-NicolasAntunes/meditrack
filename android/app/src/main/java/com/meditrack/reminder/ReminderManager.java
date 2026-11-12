// AI assistance: written with help from Claude (Anthropic); reviewed and adapted by Dimitri.
package com.meditrack.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.meditrack.history.DoseTimes;
import com.meditrack.medication.Medication;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Agenda os lembretes com AlarmManager a partir dos horários de cada medicamento.
 * Uma cópia dos medicamentos agendados fica em SharedPreferences (não é um banco: os dados vêm
 * do backend) para que as notificações e o BootReceiver funcionem mesmo sem conexão.
 */
public final class ReminderManager {
    static final String ACTION_REMINDER = "com.meditrack.reminder.ACTION_REMINDER";
    private static final String PREFS = "meditrack_reminders";
    private static final String KEY_PREFIX = "medication_";
    private static final int SNOOZE_MINUTES = 10;
    private static final int TEST_DELAY_SECONDS = 60;

    private ReminderManager() {
    }

    /** Agenda (ou reagenda, após edição) todos os horários de um medicamento. */
    public static synchronized void schedule(Context context, Medication medication) {
        if (medication == null || medication.getId() == null) {
            return;
        }
        Context app = context.getApplicationContext();
        Medication previous = cached(app, medication.getId());
        if (previous != null) {
            cancelGrid(app, previous);
        }
        prefs(app).edit().putString(KEY_PREFIX + medication.getId(), medication.toJson().toString()).apply();
        LocalDateTime now = LocalDateTime.now();
        for (String time : medication.getDoseTimes()) {
            LocalDateTime next = nextOccurrence(medication, time, now);
            if (next != null) {
                setAlarm(app, gridReminder(medication, time, next), next);
            }
        }
    }

    /** Cancela os lembretes de um medicamento excluído. */
    public static synchronized void cancel(Context context, String medicationId) {
        Context app = context.getApplicationContext();
        Medication previous = cached(app, medicationId);
        if (previous != null) {
            cancelGrid(app, previous);
        }
        prefs(app).edit().remove(KEY_PREFIX + medicationId).apply();
    }

    /** Alinha os alarmes com a lista atual do backend: agenda todos e remove os excluídos. */
    public static synchronized void syncAll(Context context, List<Medication> medications) {
        Context app = context.getApplicationContext();
        Set<String> current = new HashSet<>();
        for (Medication medication : medications) {
            current.add(medication.getId());
            schedule(app, medication);
        }
        for (Medication cachedMedication : cachedAll(app)) {
            if (!current.contains(cachedMedication.getId())) {
                cancel(app, cachedMedication.getId());
            }
        }
    }

    /** Reagenda a partir da cópia local (após reiniciar o aparelho, por exemplo). */
    public static synchronized void rescheduleAll(Context context) {
        Context app = context.getApplicationContext();
        for (Medication medication : cachedAll(app)) {
            schedule(app, medication);
        }
    }

    /** Depois que um lembrete da grade dispara, agenda o mesmo horário no próximo dia de tratamento. */
    static synchronized void scheduleNext(Context context, Reminder fired) {
        if (fired.kind != Reminder.Kind.GRID || fired.doseTime == null) {
            return;
        }
        Context app = context.getApplicationContext();
        Medication medication = cached(app, fired.medicationId);
        if (medication == null || !medication.getDoseTimes().contains(fired.doseTime)) {
            return;
        }
        LocalDateTime after = LocalDateTime.now();
        try {
            LocalDateTime scheduled = DoseTimes.parse(fired.scheduledAt);
            if (scheduled.isAfter(after)) {
                after = scheduled;
            }
        } catch (RuntimeException ignored) {
            // Usa o horário atual como referência.
        }
        LocalDateTime next = nextOccurrence(medication, fired.doseTime, after);
        if (next != null) {
            setAlarm(app, gridReminder(medication, fired.doseTime, next), next);
        }
    }

    /** "Adiar 10 min": novo alarme para a mesma dose prevista. */
    static void snooze(Context context, Reminder reminder) {
        LocalDateTime at = LocalDateTime.now().plusMinutes(SNOOZE_MINUTES);
        setAlarm(context.getApplicationContext(), reminder.withKind(Reminder.Kind.SNOOZE), at);
    }

    /** Lembrete de demonstração para daqui 1 minuto. Retorna o horário previsto. */
    public static LocalDateTime scheduleTest(Context context, Medication medication) {
        Context app = context.getApplicationContext();
        schedule(app, medication);
        LocalDateTime fireAt = LocalDateTime.now().plusSeconds(TEST_DELAY_SECONDS);
        Reminder reminder = new Reminder(medication.getId(), medication.getName(), medication.getDosage(),
                DoseTimes.format(fireAt), null, Reminder.Kind.TEST);
        setAlarm(app, reminder, fireAt);
        return fireAt;
    }

    public static boolean canScheduleExactAlarms(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        return alarmManager != null && alarmManager.canScheduleExactAlarms();
    }

    /** Medicamento como foi agendado pela última vez (cópia local). */
    static Medication cached(Context context, String medicationId) {
        String json = prefs(context).getString(KEY_PREFIX + medicationId, null);
        return parse(json);
    }

    /** Doses previstas de um medicamento em um dia, respeitando início e fim do tratamento. */
    public static List<LocalDateTime> dosesOn(Medication medication, LocalDate day) {
        List<LocalDateTime> doses = new ArrayList<>();
        LocalDate start = parseDate(medication.getStartDate());
        LocalDate end = parseDate(medication.getEndDate());
        if (start == null || end == null || day.isBefore(start) || day.isAfter(end)) {
            return doses;
        }
        for (String time : medication.getDoseTimes()) {
            LocalTime parsed = parseTime(time);
            if (parsed != null) {
                doses.add(day.atTime(parsed));
            }
        }
        Collections.sort(doses);
        return doses;
    }

    /** Próximo horário estritamente depois de "after", dentro do período do tratamento. */
    static LocalDateTime nextOccurrence(Medication medication, String time, LocalDateTime after) {
        LocalTime parsed = parseTime(time);
        LocalDate start = parseDate(medication.getStartDate());
        LocalDate end = parseDate(medication.getEndDate());
        if (parsed == null || start == null || end == null) {
            return null;
        }
        LocalDate day = after.toLocalDate();
        if (!day.atTime(parsed).isAfter(after)) {
            day = day.plusDays(1);
        }
        if (day.isBefore(start)) {
            day = start;
        }
        return day.isAfter(end) ? null : day.atTime(parsed);
    }

    private static Reminder gridReminder(Medication medication, String time, LocalDateTime at) {
        return new Reminder(medication.getId(), medication.getName(), medication.getDosage(),
                DoseTimes.format(at), time.trim(), Reminder.Kind.GRID);
    }

    private static void setAlarm(Context context, Reminder reminder, LocalDateTime at) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager == null) {
            return;
        }
        long triggerAt = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, reminder.alarmRequestCode(),
                alarmIntent(context, reminder),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            if (canScheduleExactAlarms(context)) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            }
        } catch (SecurityException exception) {
            // A permissão de alarme exato pode ter sido revogada entre a verificação e o agendamento.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        }
    }

    private static void cancelGrid(Context context, Medication medication) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        for (String time : medication.getDoseTimes()) {
            Reminder reminder = new Reminder(medication.getId(), medication.getName(), medication.getDosage(),
                    "", time.trim(), Reminder.Kind.GRID);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, reminder.alarmRequestCode(),
                    alarmIntent(context, reminder),
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pendingIntent != null) {
                if (alarmManager != null) {
                    alarmManager.cancel(pendingIntent);
                }
                pendingIntent.cancel();
            }
        }
    }

    private static Intent alarmIntent(Context context, Reminder reminder) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ACTION_REMINDER);
        intent.setData(reminder.alarmUri());
        return reminder.putInto(intent);
    }

    private static List<Medication> cachedAll(Context context) {
        List<Medication> medications = new ArrayList<>();
        for (Map.Entry<String, ?> entry : prefs(context).getAll().entrySet()) {
            if (entry.getKey().startsWith(KEY_PREFIX) && entry.getValue() instanceof String) {
                Medication medication = parse((String) entry.getValue());
                if (medication != null) {
                    medications.add(medication);
                }
            }
        }
        return medications;
    }

    private static Medication parse(String json) {
        if (json == null) {
            return null;
        }
        try {
            return Medication.fromJson(new JSONObject(json));
        } catch (JSONException exception) {
            return null;
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static LocalTime parseTime(String value) {
        try {
            return LocalTime.parse(value.trim());
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
