// AI assistance: written with help from Claude (Anthropic); reviewed and adapted by Dimitri.
package com.meditrack.home;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.meditrack.history.DoseStatus;
import com.meditrack.history.DoseTimes;
import com.meditrack.history.HistoryActivity;
import com.meditrack.history.HistoryRepository;
import com.meditrack.history.HistoryUi;
import com.meditrack.history.MedicationHistory;
import com.meditrack.history.StatsActivity;
import com.meditrack.medication.Medication;
import com.meditrack.medication.MedicationActivity;
import com.meditrack.medication.MedicationRepository;
import com.meditrack.medication.MedicationUi;
import com.meditrack.reminder.DoseActions;
import com.meditrack.reminder.NotificationHelper;
import com.meditrack.reminder.Reminder;
import com.meditrack.reminder.ReminderManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Tela inicial: doses de hoje, confirmação rápida, avisos de permissão e acesso às demais telas. */
public class HomeActivity extends Activity {
    private static final int REQUEST_NOTIFICATIONS = 31;

    private final MedicationRepository medicationRepository = new MedicationRepository();
    private final HistoryRepository historyRepository = new HistoryRepository();
    private final List<Medication> medications = new ArrayList<>();
    private LinearLayout warnings;
    private LinearLayout doses;
    private TextView dosesStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("MediTrack");
        NotificationHelper.ensureChannels(this);

        LinearLayout root = MedicationUi.vertical(this);
        root.addView(MedicationUi.heading(this, "Hoje"));
        TextView date = MedicationUi.body(this, LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE, dd/MM", new Locale("pt", "BR"))));
        date.setTextSize(14);
        root.addView(date, MedicationUi.fullWidth(this, 4));

        warnings = new LinearLayout(this);
        warnings.setOrientation(LinearLayout.VERTICAL);
        root.addView(warnings, MedicationUi.fullWidth(this, 0));

        root.addView(navButton("Meus medicamentos e estoque", MedicationActivity.class),
                MedicationUi.fullWidth(this, 14));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        row.addView(navButton("Histórico", HistoryActivity.class), half);
        LinearLayout.LayoutParams secondHalf = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        secondHalf.leftMargin = MedicationUi.dp(this, 6);
        row.addView(navButton("Estatísticas", StatsActivity.class), secondHalf);
        root.addView(row, MedicationUi.fullWidth(this, 4));

        Button test = MedicationUi.button(this, "Testar lembrete (1 min)");
        root.addView(test, MedicationUi.fullWidth(this, 4));
        test.setOnClickListener(view -> chooseTestMedication());

        root.addView(HistoryUi.section(this, "Doses de hoje"));
        dosesStatus = MedicationUi.body(this, "");
        dosesStatus.setTextSize(14);
        root.addView(dosesStatus, MedicationUi.fullWidth(this, 4));
        doses = new LinearLayout(this);
        doses.setOrientation(LinearLayout.VERTICAL);
        root.addView(doses, MedicationUi.fullWidth(this, 4));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(247, 249, 250));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);

        requestNotificationPermissionIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshWarnings();
        load();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATIONS) {
            refreshWarnings();
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        }
    }

    private void refreshWarnings() {
        warnings.removeAllViews();
        if (!ReminderManager.canScheduleExactAlarms(this)) {
            addWarning("Os lembretes podem atrasar alguns minutos: o app não tem permissão para alarmes exatos.",
                    "Permitir alarmes exatos", () -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    Uri.parse("package:" + getPackageName())));
                        }
                    });
        }
        if (!NotificationHelper.canPostNotifications(this)) {
            addWarning("As notificações estão desativadas; os lembretes não vão aparecer.",
                    "Permitir notificações", this::openNotificationSettings);
        }
    }

    private void openNotificationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                && shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
            return;
        }
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
        }
        startActivity(intent);
    }

    private void addWarning(String message, String action, Runnable onClick) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int padding = MedicationUi.dp(this, 12);
        box.setPadding(padding, padding, padding, padding);
        box.setBackground(MedicationUi.roundedBackground(this, Color.rgb(255, 239, 229)));
        TextView text = MedicationUi.body(this, message);
        text.setTextColor(Color.rgb(156, 62, 39));
        text.setTextSize(14);
        box.addView(text);
        Button button = MedicationUi.button(this, action);
        button.setOnClickListener(view -> onClick.run());
        box.addView(button, MedicationUi.fullWidth(this, 6));
        warnings.addView(box, MedicationUi.fullWidth(this, 14));
    }

    private Button navButton(String label, Class<? extends Activity> target) {
        Button button = MedicationUi.button(this, label);
        button.setOnClickListener(view -> startActivity(new Intent(this, target)));
        return button;
    }

    private void load() {
        dosesStatus.setText("Carregando…");
        medicationRepository.getAll(new MedicationRepository.Callback<List<Medication>>() {
            @Override
            public void onSuccess(List<Medication> result) {
                medications.clear();
                medications.addAll(result);
                ReminderManager.syncAll(HomeActivity.this, result);
                loadTodayHistory();
            }

            @Override
            public void onError(String message) {
                dosesStatus.setText("Não foi possível conectar ao backend. " + message);
            }
        });
    }

    private void loadTodayHistory() {
        historyRepository.findByPeriod(HistoryRepository.PERIOD_DAY,
                new MedicationRepository.Callback<List<MedicationHistory>>() {
                    @Override
                    public void onSuccess(List<MedicationHistory> entries) {
                        Map<String, DoseStatus> statuses = new HashMap<>();
                        for (MedicationHistory entry : entries) {
                            statuses.put(entry.getMedicationId() + "|" + entry.getScheduledAt(), entry.getStatus());
                        }
                        renderDoses(statuses);
                    }

                    @Override
                    public void onError(String message) {
                        renderDoses(new HashMap<>());
                        dosesStatus.setText("Histórico indisponível: " + message);
                    }
                });
    }

    private void renderDoses(Map<String, DoseStatus> statuses) {
        doses.removeAllViews();
        List<TodayDose> today = new ArrayList<>();
        LocalDate date = LocalDate.now();
        for (Medication medication : medications) {
            for (LocalDateTime time : ReminderManager.dosesOn(medication, date)) {
                today.add(new TodayDose(medication, time));
            }
        }
        Collections.sort(today, (a, b) -> a.time.compareTo(b.time));
        if (medications.isEmpty()) {
            dosesStatus.setText("Nenhum medicamento cadastrado.");
            return;
        }
        int pending = 0;
        LocalDateTime now = LocalDateTime.now();
        for (TodayDose dose : today) {
            String scheduledAt = DoseTimes.format(dose.time);
            DoseStatus status = statuses.get(dose.medication.getId() + "|" + scheduledAt);
            if (status == null || !status.isTaken()) {
                pending++;
            }
            doses.addView(doseCard(dose, scheduledAt, status, now), MedicationUi.fullWidth(this, 10));
        }
        dosesStatus.setText(today.isEmpty() ? "Nenhuma dose prevista para hoje."
                : pending == 0 ? "Todas as doses de hoje foram tomadas."
                : pending + (pending == 1 ? " dose ainda não confirmada." : " doses ainda não confirmadas."));
    }

    private LinearLayout doseCard(TodayDose dose, String scheduledAt, DoseStatus status, LocalDateTime now) {
        LinearLayout card = HistoryUi.card(this);
        TextView title = MedicationUi.body(this, DoseTimes.hour(scheduledAt) + "  ·  " + dose.medication.getName());
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextSize(17);
        card.addView(title);
        card.addView(MedicationUi.body(this, dose.medication.getDosage()), MedicationUi.fullWidth(this, 2));
        if (status != null) {
            card.addView(HistoryUi.statusLabel(this, status), MedicationUi.fullWidth(this, 4));
        } else {
            boolean due = !dose.time.isAfter(now);
            TextView label = MedicationUi.body(this, due ? "Pendente" : "Próxima");
            label.setTextSize(14);
            label.setTextColor(due ? Color.rgb(176, 112, 20) : Color.rgb(110, 122, 132));
            card.addView(label, MedicationUi.fullWidth(this, 4));
        }
        if (status == null || !status.isTaken()) {
            Button taken = MedicationUi.button(this, "Tomei");
            card.addView(taken, MedicationUi.fullWidth(this, 6));
            Reminder reminder = new Reminder(dose.medication.getId(), dose.medication.getName(),
                    dose.medication.getDosage(), scheduledAt,
                    DoseTimes.hour(scheduledAt), Reminder.Kind.GRID);
            taken.setOnClickListener(view -> {
                taken.setEnabled(false);
                DoseActions.perform(this, reminder, DoseActions.Action.TAKEN, (result, error) -> {
                    if (result == null) {
                        taken.setEnabled(true);
                        MedicationUi.toast(this, "Não foi possível registrar: " + error);
                    } else {
                        MedicationUi.toast(this, DoseActions.describe(result, DoseActions.Action.TAKEN));
                        load();
                    }
                });
            });
        }
        return card;
    }

    private void chooseTestMedication() {
        if (medications.isEmpty()) {
            MedicationUi.toast(this, "Cadastre um medicamento antes de testar o lembrete.");
            return;
        }
        if (medications.size() == 1) {
            scheduleTest(medications.get(0));
            return;
        }
        String[] names = new String[medications.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = medications.get(i).getName() + " · " + medications.get(i).getDosage();
        }
        new AlertDialog.Builder(this)
                .setTitle("Testar lembrete de qual medicamento?")
                .setItems(names, (dialog, which) -> scheduleTest(medications.get(which)))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void scheduleTest(Medication medication) {
        LocalDateTime at = ReminderManager.scheduleTest(this, medication);
        String message = "Lembrete de " + medication.getName() + " agendado para "
                + at.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + ".";
        if (!NotificationHelper.canPostNotifications(this)) {
            message += " Ative as notificações para vê-lo.";
        } else if (!ReminderManager.canScheduleExactAlarms(this)) {
            message += " Sem alarme exato, pode atrasar um pouco.";
        }
        MedicationUi.toast(this, message);
    }

    private static class TodayDose {
        final Medication medication;
        final LocalDateTime time;

        TodayDose(Medication medication, LocalDateTime time) {
            this.medication = medication;
            this.time = time;
        }
    }
}
