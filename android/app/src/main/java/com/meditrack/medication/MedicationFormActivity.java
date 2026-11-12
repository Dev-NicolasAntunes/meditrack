// AI assistance: ChatGPT (OpenAI) assisted with implementation; reviewed and adapted by Nicolas.
package com.meditrack.medication;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class MedicationFormActivity extends Activity {
    public static final String EXTRA_MEDICATION = "medication_json";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private final MedicationRepository repository = new MedicationRepository();
    private EditText name, dosageAmount, quantity, lowStockThreshold;
    private Spinner dosageUnit;
    private TextView frequency, doseTimes, startDate, endDate;
    private android.widget.Button saveButton;
    private String medicationId;
    private final List<String> selectedTimes = new ArrayList<>();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Medication existing = parseMedication(getIntent().getStringExtra(EXTRA_MEDICATION));
        medicationId = existing == null ? null : existing.getId();
        if (existing != null && existing.getDoseTimes() != null) selectedTimes.addAll(existing.getDoseTimes());
        setTitle(existing == null ? "Cadastrar medicamento" : "Editar medicamento");

        LinearLayout content = MedicationUi.vertical(this);
        content.addView(MedicationUi.heading(this, existing == null ? "Novo medicamento" : "Editar medicamento"));
        TextView help = MedicationUi.body(this, "Informe os dados do tratamento. Horários e datas são escolhidos nos seletores.");
        help.setTextSize(14);
        content.addView(help, MedicationUi.fullWidth(this, 8));

        name = field(content, "Nome", InputType.TYPE_CLASS_TEXT, "Ex.: Paracetamol", existing == null ? "" : existing.getName());
        String[] dosageUnits = {"mg", "g", "mcg", "mL", "L", "gotas", "comprimido(s)", "cápsula(s)"};
        String existingAmount = "";
        String existingUnit = "mg";
        if (existing != null && existing.getDosage() != null) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("^\\s*([0-9]+(?:[.,][0-9]+)?)\\s*(.*)$").matcher(existing.getDosage());
            if (matcher.matches()) {
                existingAmount = matcher.group(1).replace(',', '.');
                String unit = matcher.group(2).trim();
                for (String candidate : dosageUnits) if (candidate.equalsIgnoreCase(unit)) existingUnit = candidate;
            }
        }
        dosageAmount = field(content, "Dosagem", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, "Ex.: 500", existingAmount);
        dosageUnit = new Spinner(this);
        dosageUnit.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, dosageUnits));
        dosageUnit.setSelection(java.util.Arrays.asList(dosageUnits).indexOf(existingUnit));
        content.addView(dosageUnit, MedicationUi.fullWidth(this, 4));
        quantity = field(content, "Quantidade em estoque", InputType.TYPE_CLASS_NUMBER, "Ex.: 30", existing == null ? "" : String.valueOf(existing.getQuantity()));
        addLabel(content, "Frequência");
        frequency = MedicationUi.body(this, frequencyFor(selectedTimes.size()));
        content.addView(frequency, MedicationUi.fullWidth(this, 4));
        doseTimes = pickerField(content, "Horários das doses", existing == null ? "" : join(selectedTimes), this::chooseTime);
        startDate = pickerField(content, "Início do tratamento", existing == null ? "" : existing.getStartDate(), () -> chooseDate(true));
        endDate = pickerField(content, "Término do tratamento", existing == null ? "" : existing.getEndDate(), () -> chooseDate(false));
        lowStockThreshold = field(content, "Alertar quando restarem", InputType.TYPE_CLASS_NUMBER, "Unidades (padrão: 5)", existing == null ? "5" : String.valueOf(existing.getLowStockThreshold()));

        saveButton = MedicationUi.button(this, "Salvar medicamento");
        content.addView(saveButton, MedicationUi.fullWidth(this, 16));
        saveButton.setOnClickListener(v -> saveMedication());
        android.widget.Button cancel = MedicationUi.button(this, "Cancelar");
        content.addView(cancel, MedicationUi.fullWidth(this, 4));
        cancel.setOnClickListener(v -> finish());

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(247, 249, 250));
        scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);
    }

    private EditText field(LinearLayout parent, String label, int type, String hint, String value) {
        addLabel(parent, label);
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(type);
        input.setHint(hint);
        input.setText(value);
        input.setPadding(MedicationUi.dp(this, 12), MedicationUi.dp(this, 8), MedicationUi.dp(this, 12), MedicationUi.dp(this, 8));
        input.setBackground(MedicationUi.roundedBackground(this, Color.WHITE));
        parent.addView(input, MedicationUi.fullWidth(this, 4));
        return input;
    }

    private TextView pickerField(LinearLayout parent, String label, String value, Runnable onClick) {
        addLabel(parent, label);
        TextView picker = MedicationUi.body(this, value.isEmpty() ? "Toque para escolher" : value);
        picker.setFocusable(true);
        picker.setClickable(true);
        picker.setPadding(MedicationUi.dp(this, 12), MedicationUi.dp(this, 14), MedicationUi.dp(this, 12), MedicationUi.dp(this, 14));
        picker.setBackground(MedicationUi.roundedBackground(this, Color.WHITE));
        parent.addView(picker, MedicationUi.fullWidth(this, 4));
        picker.setOnClickListener(v -> onClick.run());
        if (label.startsWith("Horários")) doseTimes = picker;
        else if (label.startsWith("Início")) startDate = picker;
        else endDate = picker;
        return picker;
    }

    private void addLabel(LinearLayout parent, String label) {
        TextView view = MedicationUi.body(this, label);
        view.setTextSize(14);
        view.setTextColor(Color.rgb(42, 58, 70));
        parent.addView(view, MedicationUi.fullWidth(this, 14));
    }

    private void chooseTime() {
        TimePickerDialog dialog = new TimePickerDialog(this, (view, hour, minute) -> {
            String time = String.format(Locale.ROOT, "%02d:%02d", hour, minute);
            if (selectedTimes.contains(time)) {
                MedicationUi.toast(this, "Esse horário já foi adicionado.");
                return;
            }
            selectedTimes.add(time);
            Collections.sort(selectedTimes);
            doseTimes.setText(join(selectedTimes));
            frequency.setText(frequencyFor(selectedTimes.size()));
        }, 8, 0, true);
        dialog.setTitle("Escolha o horário");
        dialog.show();
    }

    private void chooseDate(boolean isStart) {
        LocalDate initial = parseDate(isStart ? startDate.getText().toString() : endDate.getText().toString());
        LocalDate today = LocalDate.now();
        DatePickerDialog dialog = new DatePickerDialog(this, (picker, year, month, day) -> {
            String formatted = LocalDate.of(year, month + 1, day).format(DATE_FORMAT);
            (isStart ? startDate : endDate).setText(formatted);
        }, initial == null ? today.getYear() : initial.getYear(), initial == null ? today.getMonthValue() - 1 : initial.getMonthValue() - 1, initial == null ? today.getDayOfMonth() : initial.getDayOfMonth());
        dialog.setTitle(isStart ? "Início do tratamento" : "Término do tratamento");
        dialog.show();
    }

    private void saveMedication() {
        String n = value(name), amount = value(dosageAmount), f = frequencyFor(selectedTimes.size());
        String q = value(quantity), threshold = value(lowStockThreshold);
        if (n.isEmpty() || amount.isEmpty() || q.isEmpty() || threshold.isEmpty()
                || parseDate(startDate.getText().toString()) == null || parseDate(endDate.getText().toString()) == null || selectedTimes.isEmpty()) {
            showError("Preencha todos os campos e escolha ao menos um horário e as duas datas.");
            return;
        }
        int stock, lowThreshold;
        double doseValue;
        try {
            stock = Integer.parseInt(q);
            lowThreshold = Integer.parseInt(threshold);
            doseValue = Double.parseDouble(amount.replace(',', '.'));
        } catch (NumberFormatException exception) {
            showError("Informe quantidades numéricas válidas.");
            return;
        }
        if (stock < 0 || lowThreshold < 0 || !Double.isFinite(doseValue) || doseValue <= 0) {
            showError("A dosagem deve ser maior que zero e o estoque/limite não podem ser negativos.");
            return;
        }
        LocalDate start = parseDate(startDate.getText().toString()), end = parseDate(endDate.getText().toString());
        if (end.isBefore(start)) {
            showError("A data de término deve ser igual ou posterior à data de início.");
            return;
        }
        String normalizedAmount = new BigDecimal(amount.replace(',', '.')).stripTrailingZeros().toPlainString();
        String d = normalizedAmount + " " + dosageUnit.getSelectedItem();
        Medication medication = new Medication(medicationId, n, d, stock, f, new ArrayList<>(selectedTimes), start.format(DATE_FORMAT), end.format(DATE_FORMAT), lowThreshold);
        saveButton.setEnabled(false);
        saveButton.setText("Salvando…");
        repository.save(medicationId, medication.toRequestJson(), new MedicationRepository.Callback<>() {
            @Override public void onSuccess(Medication result) { com.meditrack.reminder.ReminderManager.schedule(MedicationFormActivity.this, result); setResult(RESULT_OK, new Intent()); finish(); }
            @Override public void onError(String message) { saveButton.setEnabled(true); saveButton.setText("Salvar medicamento"); showError(message); }
        });
    }

    private LocalDate parseDate(String raw) {
        try { return LocalDate.parse(raw); } catch (RuntimeException exception) { return null; }
    }
    private String value(EditText input) { return input.getText().toString().trim(); }
    private String frequencyFor(int count) { return count == 1 ? "1 vez ao dia" : count + " vezes ao dia"; }
    private String join(List<String> values) { return android.text.TextUtils.join(", ", values); }
    private Medication parseMedication(String serialized) {
        if (serialized == null) return null;
        try { return Medication.fromJson(new JSONObject(serialized)); } catch (JSONException exception) { return null; }
    }
    private void showError(String message) {
        new AlertDialog.Builder(this).setTitle("Não foi possível salvar").setMessage(message).setPositiveButton("OK", null).show();
    }
}
