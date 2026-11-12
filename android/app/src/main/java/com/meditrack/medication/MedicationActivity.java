// AI assistance: ChatGPT (OpenAI) assisted with implementation; reviewed and adapted by Nicolas.
package com.meditrack.medication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

public class MedicationActivity extends Activity {
    private final MedicationRepository repository = new MedicationRepository();
    private MedicationAdapter adapter;
    private TextView stockNotice;
    private TextView emptyMessage;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("MediTrack");

        LinearLayout root = MedicationUi.vertical(this);
        TextView title = MedicationUi.heading(this, "Meus medicamentos");
        TextView subtitle = MedicationUi.body(this, "Organize tratamentos e acompanhe seu estoque.");
        subtitle.setTextSize(14);
        root.addView(title);
        root.addView(subtitle, MedicationUi.fullWidth(this, 6));

        stockNotice = MedicationUi.body(this, "");
        stockNotice.setPadding(
                MedicationUi.dp(this, 12), MedicationUi.dp(this, 10),
                MedicationUi.dp(this, 12), MedicationUi.dp(this, 10)
        );
        stockNotice.setBackground(MedicationUi.roundedBackground(this, Color.rgb(255, 239, 229)));
        root.addView(stockNotice, MedicationUi.fullWidth(this, 18));

        Button addButton = MedicationUi.button(this, "＋  Cadastrar medicamento");
        root.addView(addButton, MedicationUi.fullWidth(this, 14));
        addButton.setOnClickListener(view -> openForm());

        progress = new ProgressBar(this);
        progress.setIndeterminate(true);
        root.addView(progress, MedicationUi.fullWidth(this, 12));

        emptyMessage = MedicationUi.body(this, "Nenhum medicamento cadastrado.");
        emptyMessage.setGravity(android.view.Gravity.CENTER);
        root.addView(emptyMessage, MedicationUi.fullWidth(this, 20));

        ListView listView = new ListView(this);
        listView.setDivider(null);
        listView.setDividerHeight(MedicationUi.dp(this, 10));
        adapter = new MedicationAdapter(this, new MedicationAdapter.MedicationActions() {
            @Override public void onEdit(Medication medication) { openEditForm(medication); }
            @Override public void onDelete(Medication medication) { confirmDelete(medication); }
        });
        listView.setAdapter(adapter);
        root.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1
        ));
        listView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            Medication medication = adapter.getItem(position);
            Intent intent = new Intent(this, MedicationDetailsActivity.class);
            intent.putExtra(MedicationDetailsActivity.EXTRA_ID, medication.getId());
            startActivity(intent);
        });

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshMedications();
    }

    private void openForm() {
        startActivity(new Intent(this, MedicationFormActivity.class));
    }

    private void openEditForm(Medication medication) {
        Intent intent = new Intent(this, MedicationFormActivity.class);
        intent.putExtra(MedicationFormActivity.EXTRA_MEDICATION, medication.toJson().toString());
        startActivity(intent);
    }

    private void confirmDelete(Medication medication) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir medicamento")
                .setMessage("Excluir " + medication.getName() + "? Essa ação não pode ser desfeita.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Excluir", (dialog, which) -> repository.delete(
                        medication.getId(), new MedicationRepository.Callback<Void>() {
                            @Override public void onSuccess(Void result) {
                                com.meditrack.reminder.ReminderManager.cancel(MedicationActivity.this, medication.getId());
                                MedicationUi.toast(MedicationActivity.this, "Medicamento excluído.");
                                refreshMedications();
                            }
                            @Override public void onError(String message) {
                                MedicationUi.toast(MedicationActivity.this, message);
                            }
                        }))
                .show();
    }

    private void refreshMedications() {
        progress.setVisibility(View.VISIBLE);
        emptyMessage.setVisibility(View.GONE);
        repository.getAll(new MedicationRepository.Callback<List<Medication>>() {
            @Override
            public void onSuccess(List<Medication> medications) {
                progress.setVisibility(View.GONE);
                adapter.replaceAll(medications);
                com.meditrack.reminder.ReminderManager.syncAll(MedicationActivity.this, medications);
                emptyMessage.setVisibility(medications.isEmpty() ? View.VISIBLE : View.GONE);
                int lowStockCount = 0;
                for (Medication medication : medications) {
                    if (medication.isLowStock()) {
                        lowStockCount++;
                    }
                }
                if (lowStockCount == 0) {
                    stockNotice.setText("Estoque em dia");
                    stockNotice.setTextColor(Color.rgb(43, 102, 76));
                    stockNotice.setBackground(MedicationUi.roundedBackground(
                            MedicationActivity.this, Color.rgb(231, 245, 237)));
                } else {
                    stockNotice.setText(lowStockCount + (lowStockCount == 1
                            ? " medicamento com estoque baixo"
                            : " medicamentos com estoque baixo"));
                    stockNotice.setTextColor(Color.rgb(156, 62, 39));
                    stockNotice.setBackground(MedicationUi.roundedBackground(
                            MedicationActivity.this, Color.rgb(255, 239, 229)));
                }
            }

            @Override
            public void onError(String message) {
                progress.setVisibility(View.GONE);
                stockNotice.setText("Não foi possível conectar ao backend.");
                stockNotice.setTextColor(Color.rgb(156, 62, 39));
                MedicationUi.toast(MedicationActivity.this, message
                        + " Verifique se o backend está ligado e se o endereço da API está correto.");
            }
        });
    }
}
