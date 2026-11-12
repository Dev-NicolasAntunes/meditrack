// AI assistance: written with help from Claude (Anthropic); reviewed and adapted by Dimitri.
package com.meditrack.history;

import android.graphics.Color;

public enum DoseStatus {
    TOMADO("Tomado", Color.rgb(48, 113, 78)),
    ATRASADO("Tomado com atraso", Color.rgb(176, 112, 20)),
    ADIADO("Adiado", Color.rgb(52, 98, 150)),
    NAO_TOMADO("Não tomado", Color.rgb(170, 57, 35));

    private final String label;
    private final int color;

    DoseStatus(String label, int color) {
        this.label = label;
        this.color = color;
    }

    public String getLabel() { return label; }
    public int getColor() { return color; }

    public boolean isTaken() {
        return this == TOMADO || this == ATRASADO;
    }

    public static DoseStatus parse(String value) {
        try {
            return valueOf(value);
        } catch (RuntimeException exception) {
            return NAO_TOMADO;
        }
    }
}
