// AI assistance: ChatGPT (OpenAI) assisted with implementation; reviewed and adapted by Nicolas.
package com.meditrack.medication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class MedicationDetailsActivity extends Activity {
    public static final String EXTRA_ID = "medication_id";
    private static final int EDIT_REQUEST = 21;

    private final MedicationRepository repository = new MedicationRepository();
    private String medicationId;
    private LinearLayout content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        medicationId = getIntent().getStringExtra(EXTRA_ID);
        content = MedicationUi.vertical(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);
        loadMedication();
    }

    private void loadMedication() {
        content.removeAllViews();
        content.addView(MedicationUi.body(this, "Carregando medicamento…"));
        repository.getById(medicationId, new MedicationRepository.Callback<Medication>() {
            @Override
            public void onSuccess(Medication medication) {
                render(medication);
            }

            @Override
            public void onError(String message) {
                content.removeAllViews();
                content.addView(MedicationUi.heading(MedicationDetailsActivity.this, "Detalhes"));
                content.addView(MedicationUi.body(MedicationDetailsActivity.this, message),
                        MedicationUi.fullWidth(MedicationDetailsActivity.this, 12));
                android.widget.Button retry = MedicationUi.button(
                        MedicationDetailsActivity.this, "Tentar novamente");
                content.addView(retry, MedicationUi.fullWidth(MedicationDetailsActivity.this, 8));
                retry.setOnClickListener(view -> loadMedication());
            }
        });
    }

    private void render(Medication medication) {
        setTitle(medication.getName());
        content.removeAllViews();
        content.addView(MedicationUi.heading(this, medication.getName()));
        TextView dose = MedicationUi.body(this, medication.getDosage() + " · " + medication.getFrequency());
        dose.setTextSize(17);
        content.addView(dose, MedicationUi.fullWidth(this, 8));

        content.addView(section("Estoque"));
        content.addView(MedicationUi.body(this, medication.getQuantity() + " unidade(s) disponível(is)"),
                MedicationUi.fullWidth(this, 4));
        TextView alert = MedicationUi.body(this, medication.isLowStock()
                ? "Estoque baixo: reponha em breve."
                : "Estoque suficiente.");
        alert.setTextColor(medication.isLowStock() ? Color.rgb(170, 57, 35) : Color.rgb(48, 113, 78));
        content.addView(alert, MedicationUi.fullWidth(this, 4));
        content.addView(MedicationUi.body(this,
                "Previsão aproximada: " + medication.getEstimatedDaysRemaining() + " dia(s) restantes."),
                MedicationUi.fullWidth(this, 4));

        content.addView(section("Tratamento"));
        content.addView(MedicationUi.body(this, "Horários: " + android.text.TextUtils.join(", ", medication.getDoseTimes())),
                MedicationUi.fullWidth(this, 4));
        content.addView(MedicationUi.body(this, "Início: " + medication.getStartDate()),
                MedicationUi.fullWidth(this, 4));
        content.addView(MedicationUi.body(this, "Término: " + medication.getEndDate()),
                MedicationUi.fullWidth(this, 4));
        content.addView(MedicationUi.body(this,
                "Alerta de estoque baixo em " + medication.getLowStockThreshold() + " unidade(s)."),
                MedicationUi.fullWidth(this, 4));

        android.widget.Button adjust = MedicationUi.button(this, "Ajustar estoque");
        content.addView(adjust, MedicationUi.fullWidth(this, 18));
        adjust.setOnClickListener(view -> showStockDialog());

        android.widget.Button confirmDose = MedicationUi.button(this, "Registrar dose utilizada");
        content.addView(confirmDose, MedicationUi.fullWidth(this, 4));
        confirmDose.setOnClickListener(view -> confirmDose());

        android.widget.Button edit = MedicationUi.button(this, "Editar medicamento");
        content.addView(edit, MedicationUi.fullWidth(this, 4));
        edit.setOnClickListener(view -> {
            Intent intent = new Intent(this, MedicationFormActivity.class);
            intent.putExtra(MedicationFormActivity.EXTRA_MEDICATION, medication.toJson().toString());
            startActivityForResult(intent, EDIT_REQUEST);
        });

        android.widget.Button delete = MedicationUi.button(this, "Excluir medicamento");
        content.addView(delete, MedicationUi.fullWidth(this, 4));
        delete.setTextColor(Color.rgb(170, 57, 35));
        delete.setOnClickListener(view -> confirmDelete(medication));
    }

    private TextView section(String title) {
        TextView text = MedicationUi.heading(this, title);
        text.setTextSize(17);
        text.setPadding(0, MedicationUi.dp(this, 18), 0, MedicationUi.dp(this, 2));
        return text;
    }

    private void showStockDialog() {
        EditText amount = new EditText(this);
        amount.setInputType(InputType.TYPE_CLASS_NUMBER);
        amount.setHint("Quantidade de unidades");
        int padding = MedicationUi.dp(this, 18);
        amount.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle("Ajustar estoque")
                .setMessage("Informe quantas unidades deseja adicionar ou retirar.")
                .setView(amount)
                .setNeutralButton("Cancelar", null)
                .setPositiveButton("Adicionar", (dialog, which) ->
                        changeStock(amount.getText().toString(), 1))
                .setNegativeButton("Retirar", (dialog, which) ->
                        changeStock(amount.getText().toString(), -1))
                .show();
    }

    private void changeStock(String text, int sign) {
        int amount;
        try {
            amount = Integer.parseInt(text.trim());
            if (amount <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException exception) {
            MedicationUi.toast(this, "Informe uma quantidade maior que zero.");
            return;
        }
        repository.adjustStock(medicationId, sign * amount, new MedicationRepository.Callback<Medication>() {
            @Override
            public void onSuccess(Medication medication) {
                MedicationUi.toast(MedicationDetailsActivity.this, medication.isLowStock()
                        ? "Estoque atualizado. Atenção: estoque baixo."
                        : "Estoque atualizado.");
                render(medication);
            }

            @Override
            public void onError(String message) {
                showError(message);
            }
        });
    }

    private void confirmDose() {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar dose")
                .setMessage("Confirma que utilizou uma dose? Uma unidade será descontada do estoque.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Confirmar", (dialog, which) -> {
                        // Integrante 2: a dose passa pelo histórico (POST /api/history), que desconta o estoque.
                        String now = com.meditrack.history.DoseTimes.format(com.meditrack.history.DoseTimes.now());
                        new com.meditrack.history.HistoryRepository().record(medicationId, now, now,
                                com.meditrack.history.DoseStatus.TOMADO,
                                new MedicationRepository.Callback<com.meditrack.history.DoseRecordResult>() {
                            @Override
                            public void onSuccess(com.meditrack.history.DoseRecordResult result) {
                                Medication medication = result.medication;
                                MedicationUi.toast(MedicationDetailsActivity.this, result.duplicate
                                        ? "Uma dose já foi registrada neste minuto."
                                        : medication.isLowStock() ? "Dose registrada. Estoque baixo."
                                                : "Dose registrada e estoque atualizado.");
                                render(medication);
                            }

                            @Override
                            public void onError(String message) {
                                showError(message);
                            }
                        });
                })
                .show();
    }

    private void confirmDelete(Medication medication) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir medicamento")
                .setMessage("Excluir " + medication.getName() + "? Essa ação não pode ser desfeita.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Excluir", (dialog, which) ->
                        repository.delete(medicationId, new MedicationRepository.Callback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                com.meditrack.reminder.ReminderManager.cancel(MedicationDetailsActivity.this, medicationId);
                                setResult(RESULT_OK);
                                finish();
                            }

                            @Override
                            public void onError(String message) {
                                showError(message);
                            }
                        }))
                .show();
    }

    private void showError(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Não foi possível concluir")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_REQUEST && resultCode == RESULT_OK) {
            loadMedication();
        }
    }
}
