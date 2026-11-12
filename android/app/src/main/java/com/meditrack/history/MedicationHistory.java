// AI assistance: written with help from Claude (Anthropic); reviewed and adapted by Dimitri.
package com.meditrack.history;

import org.json.JSONException;
import org.json.JSONObject;

public class MedicationHistory {
    private final long id;
    private final String medicationId;
    private final String medicationName;
    private final String dosage;
    private final String scheduledAt;
    private final String actionAt;
    private final DoseStatus status;

    public MedicationHistory(long id, String medicationId, String medicationName, String dosage,
                             String scheduledAt, String actionAt, DoseStatus status) {
        this.id = id;
        this.medicationId = medicationId;
        this.medicationName = medicationName;
        this.dosage = dosage;
        this.scheduledAt = scheduledAt;
        this.actionAt = actionAt;
        this.status = status;
    }

    public static MedicationHistory fromJson(JSONObject json) throws JSONException {
        return new MedicationHistory(
                json.getLong("id"),
                json.getString("medicationId"),
                json.getString("medicationName"),
                json.getString("dosage"),
                json.getString("scheduledAt"),
                json.getString("actionAt"),
                DoseStatus.parse(json.getString("status"))
        );
    }

    public long getId() { return id; }
    public String getMedicationId() { return medicationId; }
    public String getMedicationName() { return medicationName; }
    public String getDosage() { return dosage; }
    /** Data/hora local no formato AAAA-MM-DDTHH:mm. */
    public String getScheduledAt() { return scheduledAt; }
    public String getActionAt() { return actionAt; }
    public DoseStatus getStatus() { return status; }
}
