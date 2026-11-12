// AI assistance: written with help from Claude (Anthropic); reviewed and adapted by Dimitri.
package com.meditrack.reminder;

import android.content.Intent;
import android.net.Uri;

/**
 * Um lembrete de dose: medicamento + horário previsto (data/hora local AAAA-MM-DDTHH:mm).
 * GRID vem dos horários cadastrados, SNOOZE é o "Adiar 10 min" e TEST é o lembrete de demonstração.
 */
public class Reminder {
    public enum Kind { GRID, SNOOZE, TEST }

    private static final String EXTRA_MEDICATION_ID = "com.meditrack.reminder.MEDICATION_ID";
    private static final String EXTRA_NAME = "com.meditrack.reminder.NAME";
    private static final String EXTRA_DOSAGE = "com.meditrack.reminder.DOSAGE";
    private static final String EXTRA_SCHEDULED_AT = "com.meditrack.reminder.SCHEDULED_AT";
    private static final String EXTRA_DOSE_TIME = "com.meditrack.reminder.DOSE_TIME";
    private static final String EXTRA_KIND = "com.meditrack.reminder.KIND";

    public final String medicationId;
    public final String medicationName;
    public final String dosage;
    public final String scheduledAt;
    /** Horário da grade (HH:mm) para GRID; null nos demais tipos. */
    public final String doseTime;
    public final Kind kind;

    public Reminder(String medicationId, String medicationName, String dosage,
                    String scheduledAt, String doseTime, Kind kind) {
        this.medicationId = medicationId;
        this.medicationName = medicationName;
        this.dosage = dosage;
        this.scheduledAt = scheduledAt;
        this.doseTime = doseTime;
        this.kind = kind;
    }

    public Reminder withKind(Kind newKind) {
        return new Reminder(medicationId, medicationName, dosage, scheduledAt, doseTime, newKind);
    }

    public Reminder withDetails(String name, String newDosage) {
        return new Reminder(medicationId, name, newDosage, scheduledAt, doseTime, kind);
    }

    public Intent putInto(Intent intent) {
        intent.putExtra(EXTRA_MEDICATION_ID, medicationId);
        intent.putExtra(EXTRA_NAME, medicationName);
        intent.putExtra(EXTRA_DOSAGE, dosage);
        intent.putExtra(EXTRA_SCHEDULED_AT, scheduledAt);
        intent.putExtra(EXTRA_DOSE_TIME, doseTime);
        intent.putExtra(EXTRA_KIND, kind.name());
        return intent;
    }

    public static Reminder fromIntent(Intent intent) {
        if (intent == null || intent.getStringExtra(EXTRA_MEDICATION_ID) == null
                || intent.getStringExtra(EXTRA_SCHEDULED_AT) == null) {
            return null;
        }
        Kind kind;
        try {
            kind = Kind.valueOf(intent.getStringExtra(EXTRA_KIND));
        } catch (RuntimeException exception) {
            kind = Kind.GRID;
        }
        return new Reminder(
                intent.getStringExtra(EXTRA_MEDICATION_ID),
                intent.getStringExtra(EXTRA_NAME),
                intent.getStringExtra(EXTRA_DOSAGE),
                intent.getStringExtra(EXTRA_SCHEDULED_AT),
                intent.getStringExtra(EXTRA_DOSE_TIME),
                kind
        );
    }

    /** Identifica o alarme: na grade é medicamento + horário; nos outros, medicamento + horário previsto. */
    public Uri alarmUri() {
        String suffix = kind == Kind.GRID ? doseTime : scheduledAt;
        return new Uri.Builder().scheme("meditrack").authority("reminder")
                .appendPath(kind.name().toLowerCase(java.util.Locale.ROOT))
                .appendPath(medicationId)
                .appendPath(suffix)
                .build();
    }

    /** requestCode único por medicamento e horário. */
    public int alarmRequestCode() {
        return alarmUri().toString().hashCode();
    }

    /** Uma notificação por dose prevista (medicamento + horário previsto). */
    public int notificationId() {
        return (medicationId + "|" + scheduledAt).hashCode();
    }

    public Uri doseUri(String action) {
        return new Uri.Builder().scheme("meditrack").authority("dose")
                .appendPath(action)
                .appendPath(medicationId)
                .appendPath(scheduledAt)
                .build();
    }
}
