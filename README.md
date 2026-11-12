# MediTrack

#### Video Demo: <URL>

#### Description:

MediTrack is an Android application, written in Java, that helps people follow a medication treatment from the first dose to the last. It is backed by a separate Java/Spring Boot server that stores everything in SQLite.

Poor adherence to treatment is one of the most common reasons why a treatment fails. Patients forget a dose, take it hours later, or simply run out of pills because nobody noticed that the box was almost empty. Most of the time the problem is not a lack of willingness but a lack of structure: there is no reminder at the right time, no record of what was actually taken and no warning before the stock ends. MediTrack tries to provide that structure in a simple way.

With MediTrack the user registers each medication with its name, dosage, quantity in stock, daily dose times and treatment period. From that information the app:

- schedules a reminder notification for every dose time, between the start and end dates of the treatment;
- lets the user answer the reminder directly from the notification with "Tomei" (taken), "Adiar 10 min" (snooze for 10 minutes) or "Não tomei" (not taken);
- records every answer in a history, discounts one unit from the stock when a dose is taken and shows a low-stock warning when the quantity reaches the limit chosen for that medication;
- shows a "Today" screen with the doses of the day, a history screen filtered by day, week or month (with buttons to move to previous periods) and a statistics screen with the adherence percentage and the number of doses in each status, including the doses that received no answer at all.

The user interface is in Portuguese, because the app was built for Brazilian users. This README is in English.

MediTrack was built by two people. **Nicolas** (Integrante 1) built the medications and stock module: registering, editing, deleting, listing and showing the details of medications, stock adjustments, the low-stock warning, the days-remaining estimate and the backend API for medications. **Dimitri** (Integrante 2) built the reminders, notifications and history module: scheduling the alarms, the notifications and their actions, the dose confirmation screen, the "Today" screen, the history and statistics screens and the backend API for history.

## Project structure

### Android app (`android/app/src/`)

**Manifests**

- `main/AndroidManifest.xml` declares the permissions (internet, notifications, exact alarms and boot completed), all the activities and the three broadcast receivers. `HomeActivity` is the launcher, and `BootReceiver` listens to boot, app update, time change and exact-alarm permission change.
- `debug/AndroidManifest.xml` allows plain HTTP traffic only in the debug build, so the emulator can reach the local backend without TLS.

**Package `medication` (Nicolas)**

- `Medication.java` is the medication model on the app side, with conversion from the API JSON and to the JSON sent when saving (and to the copy cached by the reminders).
- `MedicationApiClient.java` is the blocking HTTP client for `/api/medications`: list, find, save, delete, adjust stock and confirm a dose.
- `MedicationRepository.java` runs the API client on a background thread and delivers the result to the screens through a `Callback` on the main thread.
- `MedicationActivity.java` is the "Meus medicamentos" screen: the list, the low-stock banner and the register button. It also keeps the reminders in sync with the list returned by the backend.
- `MedicationAdapter.java` builds each card of the list, with stock, low-stock status and the "Editar" and "Excluir" buttons, and lets a tap on the card open the details.
- `MedicationDetailsActivity.java` shows all the data of one medication and lets the user adjust the stock, record a dose that was used, edit or delete it.
- `MedicationFormActivity.java` is the register and edit form, with validation, time and date pickers and a unit selector for the dosage. After saving it schedules the reminders of that medication.
- `MedicationUi.java` holds small helpers (headings, text, buttons, margins, rounded backgrounds) so every screen has the same look.

**Package `home` (Dimitri)**

- `HomeActivity.java` is the "Hoje" (Today) screen: the doses of the day with their status and a quick "Tomei" button, warnings when notification or exact-alarm permission is missing, shortcuts to the other screens and the "Testar lembrete (1 min)" demo button.

**Package `reminder` (Dimitri)**

- `Reminder.java` represents one reminder: medication, scheduled local date and time, and kind (scheduled dose, snooze or test). It knows how to travel inside an `Intent`.
- `ReminderManager.java` schedules, cancels and re-creates the alarms from each medication's dose times, keeps a copy of the scheduled medications in `SharedPreferences` and calculates the doses of a given day.
- `ReminderReceiver.java` receives the alarm, shows the dose notification and schedules the next occurrence.
- `DoseActionReceiver.java` handles the three notification buttons without opening the app.
- `DoseActions.java` contains the shared logic of an answer (from the notification, the confirmation screen or the Today screen): it records the dose in the history, snoozes when needed and shows the low-stock notification.
- `DoseConfirmationActivity.java` opens when the user taps the notification body and offers the same three options on a full screen.
- `NotificationHelper.java` creates the notification channels and builds the dose reminder and low-stock notifications using only the native Android APIs.
- `BootReceiver.java` reschedules every alarm after a reboot, an app update, a clock or time zone change, or a change in the exact-alarm permission, because Android clears alarms in those cases.

**Package `history` (Dimitri)**

- `DoseStatus.java` lists the four statuses (taken, taken late, snoozed, not taken) with their Portuguese labels and colors.
- `DoseTimes.java` formats and parses the local date-time strings exchanged with the backend (`yyyy-MM-ddTHH:mm`, no time zone).
- `MedicationHistory.java` is one history entry as returned by the API.
- `DoseRecordResult.java` is the answer to recording a dose: the saved entry, the medication with the updated stock and whether it was a duplicate.
- `HistoryStats.java` holds the statistics of a period, including the "no answer" count.
- `HistoryPeriod.java` represents the period being shown (day, week or month plus an anchor date): it calculates the start and end, the label such as "09/11 – 15/11", the previous and next periods and the reference sent to the backend.
- `HistoryApiClient.java` is the blocking HTTP client for `/api/history` and `/api/history/stats`.
- `HistoryRepository.java` runs those requests in the background and reuses the `Callback` of the medication module.
- `HistoryUi.java` contains shared pieces of the history screens: the day/week/month selector, the "◀ Anterior / Próximo ▶" navigator, cards and status labels.
- `HistoryActivity.java` lists the entries of the selected period and lets the user move to earlier periods (never past the current one).
- `StatsActivity.java` shows the adherence percentage, a progress bar and the count of each status plus the doses with no answer, for the same period chosen in the history.

### Backend (`backend/src/main/`)

- `resources/application.properties` sets the application name, the port (8080) and the SQLite file path (`./data/meditrack.db`, relative to the working directory).
- `MediTrackBackendApplication.java` is the Spring Boot entry point.

**Package `medication` (Nicolas)**

- `Medication.java` is the medication entity, including the calculated days remaining and the low-stock flag.
- `MedicationRequest.java` is the body accepted when creating or updating a medication.
- `StockAdjustmentRequest.java` is the body of a stock adjustment (`quantityChange`, negative to remove units).
- `MedicationController.java` exposes the REST endpoints under `/api/medications`.
- `MedicationService.java` validates the input, computes the derived fields and returns `409 Conflict` when the stock would become negative.
- `MedicationRepository.java` creates the `medications` table and runs the SQL. Stock changes are single atomic `UPDATE` statements that refuse to go below zero.

**Package `history` (Dimitri)**

- `DoseStatus.java` is the backend enum `TOMADO`, `ATRASADO`, `ADIADO`, `NAO_TOMADO`.
- `HistoryRequest.java` is the body of `POST /api/history`: medication, scheduled time, action time and status.
- `MedicationHistory.java` is one row of the `medication_history` table.
- `DoseRecordResult.java` is the response of recording a dose, with a `duplicate` flag when the dose had already been taken.
- `HistoryStats.java` is the statistics response: period, dates, expected and taken doses, adherence and the count of each status, including `semResposta` (no answer).
- `HistoryController.java` exposes `/api/history` and `/api/history/stats`.
- `HistoryService.java` applies the late rule, avoids double stock discounts, calculates the period ranges and the statistics, and calls the medication module to discount the stock.
- `HistoryRepository.java` creates the `medication_history` table in the same SQLite file and performs the insert-or-update, the rollback and the period queries.

## Design choices

**Spring Boot backend with SQLite instead of a local database.** The course project required a client and a server, and keeping the data on a server means that the history and the stock survive a reinstall of the app and could later be shared with another device or a caregiver. SQLite was chosen because it is a single file, needs no installation and is more than enough for one user. Both modules use the same file, so the history can reference medications directly.

**Exact AlarmManager with fallback and rescheduling on boot.** A medication reminder that arrives twenty minutes late is not very useful, so the app uses `setExactAndAllowWhileIdle`, which also fires in Doze mode. When the user has not granted the exact-alarm permission, or it is revoked in the middle of scheduling, the app falls back to `setAndAllowWhileIdle` instead of failing, and the Today screen shows a warning. Because Android deletes alarms on reboot, update or clock changes, `BootReceiver` recreates all of them.

**Cache in SharedPreferences.** Alarms fire when the app is closed and possibly without network. To show the medication name and dosage and to schedule the next dose, the reminder code keeps a small JSON copy of each scheduled medication in `SharedPreferences`. It is not a second database: the backend is still the source of truth, and the copy is refreshed every time the list is loaded.

**Idempotency by (medication, scheduled time).** The same dose can be answered from the notification, from the confirmation screen and from the Today screen, sometimes twice by accident. The history table has a unique key on `(medication_id, scheduled_at)` and the backend uses an insert-or-update that never overwrites a dose that was already taken. A second "Tomei" returns the existing entry with `duplicate = true` and does not discount the stock again. If the stock discount fails, the history entry is restored to its previous state.

**The ATRASADO rule (30 minutes).** A confirmation made more than 30 minutes after the scheduled time is stored as `ATRASADO` (taken late). It still counts as taken for adherence, but the user can see that the habit is slipping.

**How adherence is calculated.** Expected doses are the scheduled times of every medication inside the period that have already arrived, plus any recorded dose outside those times (such as the test reminder). Taken doses are `TOMADO` plus `ATRASADO`. Adherence is taken ÷ expected, rounded and capped at 100%. A snoozed dose that is later taken counts once as taken. "Sem resposta" (no answer) is expected minus all recorded statuses, never negative, so the numbers on the statistics screen always add up to the expected total.

**Local dates without time zone.** Medication schedules are about the user's wall clock: "08:00" should mean 08:00 wherever the phone is. Dates and times are therefore exchanged as local strings (`yyyy-MM-ddTHH:mm`) without time zone conversion. The app sends its own local time as `reference`, so the server clock does not matter. For past periods the app sends the last minute of that period, because the backend also uses the reference as "now" when counting expected doses.

**UI without AndroidX.** All screens are built in code with the framework widgets (`Activity`, `LinearLayout`, `ListView`, `Button`) and notifications use the native `Notification` APIs. This keeps the project free of extra dependencies and made every part of the interface easy to understand for both of us.

## How to run

### Environment

- JDK 26 for the backend and for the Android Gradle daemon (set in `android/gradle/gradle-daemon-jvm.properties`). The app keeps Java 17 source compatibility.
- Android Studio with Android SDK API 36. The versioned Gradle Wrapper pins Gradle 9.6.1.
- `backend/data/` and local build outputs are ignored by Git. Do not commit personal data or secrets.

### Backend (IntelliJ IDEA or command line)

1. Open `backend/pom.xml` and select JDK 26.
2. Wait for Maven to import the dependencies.
3. Run `com.meditrack.backend.MediTrackBackendApplication`, or from `backend/` run `java -jar target/meditrack-backend-0.0.1-SNAPSHOT.jar` after `mvn package`.
4. The API is available at `http://localhost:8080`. The SQLite file is created at `backend/data/meditrack.db` when the working directory is `backend/`.

### Android app (Android Studio)

1. Open the `android/` folder and sync Gradle.
2. Start an emulator (or connect a device) and run the app.
3. In the emulator the default API address is `http://10.0.2.2:8080/api/`. `10.0.2.2` is the emulator's alias for the computer's `localhost`.
4. For a physical device, set `MEDTRACK_API_BASE_URL` in your user `gradle.properties` (outside the repository) to the computer's local IP, for example `http://192.168.0.10:8080/api/`.

Plain HTTP is only enabled in the debug variant. Do not use this configuration to publish the app.

### API

Medications:

- `GET /api/medications` and `GET /api/medications/{id}`
- `POST /api/medications` and `PUT /api/medications/{id}`
- `DELETE /api/medications/{id}`
- `POST /api/medications/{id}/stock-adjustments` with `{"quantityChange": 5}` (negative to remove units)
- `POST /api/medications/{id}/doses/confirm` to discount one unit after a confirmation

History:

- `POST /api/history` with `medicationId`, `scheduledAt`, `actionAt` and `status`
- `GET /api/history?period=day|week|month&reference=yyyy-MM-ddTHH:mm`
- `GET /api/history/stats?period=day|week|month&reference=yyyy-MM-ddTHH:mm`

Dates use `yyyy-MM-dd` and times use `HH:mm`. The low-stock warning appears in the app and in the API response when the quantity is less than or equal to the medication's limit (default: 5 units). Weeks go from Monday to Sunday.

## How to test

1. Start the backend and the app in the emulator. Accept the notification permission (Android 13+) and, if the warning appears, allow exact alarms.
2. Register a medication with stock close to the low-stock limit, for example quantity 6 and alert at 5, with two dose times.
3. On the "Hoje" screen tap "Testar lembrete (1 min)" and leave the app.
4. When the notification appears, tap "Tomei". The history records the dose, the stock drops from 6 to 5 and the low-stock notification appears.
5. Open "Histórico" (the entry shows "Tomado"), use "◀ Anterior" and "Próximo ▶" to move between periods, and open "Estatísticas" to check the adherence and that Tomado + atraso + Adiado + Não tomado + Sem resposta equals the expected doses.
6. Open "Meus medicamentos" and check the low-stock banner. Also try "Adiar 10 min" and "Não tomei".

## AI usage

- **Integrante 2 (Dimitri):** Claude (Anthropic) was used as an assistant while implementing the reminders, notifications, history and statistics, and while writing this README. Every file written with its help says so in a comment on its first line. All code was reviewed, tested and adapted by Dimitri.
- **Integrante 1 (Nicolas):** _to be completed by Nicolas._
