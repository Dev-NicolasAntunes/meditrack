// AI assistance: written with help from Claude (Anthropic); reviewed and adapted by Dimitri.
package com.meditrack.history;

import com.meditrack.medication.Medication;

import org.json.JSONException;
import org.json.JSONObject;

/** Resposta do POST /api/history: o registro gravado e o medicamento com o estoque atualizado. */
public class DoseRecordResult {
    public final MedicationHistory history;
    public final Medication medication;
    /** true quando a dose já estava registrada como tomada; o estoque não foi descontado de novo. */
    public final boolean duplicate;

    private DoseRecordResult(MedicationHistory history, Medication medication, boolean duplicate) {
        this.history = history;
        this.medication = medication;
        this.duplicate = duplicate;
    }

    public static DoseRecordResult fromJson(JSONObject json) throws JSONException {
        JSONObject medication = json.optJSONObject("medication");
        return new DoseRecordResult(
                MedicationHistory.fromJson(json.getJSONObject("history")),
                medication == null ? null : Medication.fromJson(medication),
                json.optBoolean("duplicate", false)
        );
    }
}
