// AI assistance: written with help from Claude (Anthropic); reviewed and adapted by Dimitri.
package com.meditrack.history;

import org.json.JSONException;
import org.json.JSONObject;

public class HistoryStats {
    public final String startDate;
    public final String endDate;
    public final int expectedDoses;
    public final int takenDoses;
    public final int adherencePercent;
    public final int tomado;
    public final int atrasado;
    public final int adiado;
    public final int naoTomado;

    private HistoryStats(JSONObject json) throws JSONException {
        startDate = json.getString("startDate");
        endDate = json.getString("endDate");
        expectedDoses = json.getInt("expectedDoses");
        takenDoses = json.getInt("takenDoses");
        adherencePercent = json.getInt("adherencePercent");
        tomado = json.getInt("tomado");
        atrasado = json.getInt("atrasado");
        adiado = json.getInt("adiado");
        naoTomado = json.getInt("naoTomado");
    }

    public static HistoryStats fromJson(JSONObject json) throws JSONException {
        return new HistoryStats(json);
    }
}
