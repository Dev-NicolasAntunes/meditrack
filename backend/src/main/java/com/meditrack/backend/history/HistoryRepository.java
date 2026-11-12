// AI assistance: written with help from Claude (Anthropic); reviewed and adapted by Dimitri.
package com.meditrack.backend.history;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Tabela de histórico no mesmo arquivo SQLite usado pelo módulo de medicamentos
 * (propriedade meditrack.database.path).
 */
@Repository
public class HistoryRepository {
    private static final String SELECT_COLUMNS =
            "id, medication_id, medication_name, dosage, scheduled_at, action_at, status";

    private final String jdbcUrl;

    public HistoryRepository(Environment environment) {
        try {
            String databasePath = environment.getProperty("meditrack.database.path", "./data/meditrack.db");
            Path path = Paths.get(databasePath).toAbsolutePath();
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            this.jdbcUrl = "jdbc:sqlite:" + path;
            initializeSchema();
        } catch (IOException | SQLException exception) {
            throw new IllegalStateException("Não foi possível inicializar a tabela de histórico.", exception);
        }
    }

    private void initializeSchema() throws SQLException {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            // Uma única linha por dose prevista (medicamento + horário previsto) garante a idempotência.
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS medication_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        medication_id TEXT NOT NULL,
                        medication_name TEXT NOT NULL,
                        dosage TEXT NOT NULL,
                        scheduled_at TEXT NOT NULL,
                        action_at TEXT NOT NULL,
                        status TEXT NOT NULL
                            CHECK (status IN ('TOMADO', 'ATRASADO', 'ADIADO', 'NAO_TOMADO')),
                        UNIQUE (medication_id, scheduled_at)
                    )
                    """);
            statement.execute("""
                    CREATE INDEX IF NOT EXISTS idx_medication_history_scheduled_at
                    ON medication_history (scheduled_at)
                    """);
        }
    }

    public Optional<MedicationHistory> findByDose(String medicationId, String scheduledAt) {
        String sql = "SELECT " + SELECT_COLUMNS
                + " FROM medication_history WHERE medication_id = ? AND scheduled_at = ?";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, medicationId);
            statement.setString(2, scheduledAt);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Não foi possível consultar o histórico.", exception);
        }
    }

    /**
     * Insere ou atualiza o registro da dose numa única instrução. Um registro que já está
     * TOMADO/ATRASADO nunca é sobrescrito. Retorna true se a linha foi gravada.
     */
    public boolean upsertUnlessTaken(MedicationHistory history) {
        String sql = """
                INSERT INTO medication_history
                (medication_id, medication_name, dosage, scheduled_at, action_at, status)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (medication_id, scheduled_at) DO UPDATE SET
                    medication_name = excluded.medication_name,
                    dosage = excluded.dosage,
                    action_at = excluded.action_at,
                    status = excluded.status
                WHERE medication_history.status NOT IN ('TOMADO', 'ATRASADO')
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, history);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new IllegalStateException("Não foi possível registrar a dose no histórico.", exception);
        }
    }

    /** Desfaz uma gravação quando o desconto do estoque falha. */
    public void restore(String medicationId, String scheduledAt, Optional<MedicationHistory> previous) {
        String sql = previous.isPresent()
                ? """
                  UPDATE medication_history
                  SET medication_name = ?, dosage = ?, action_at = ?, status = ?
                  WHERE medication_id = ? AND scheduled_at = ?
                  """
                : "DELETE FROM medication_history WHERE medication_id = ? AND scheduled_at = ?";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            if (previous.isPresent()) {
                MedicationHistory old = previous.get();
                statement.setString(index++, old.getMedicationName());
                statement.setString(index++, old.getDosage());
                statement.setString(index++, old.getActionAt());
                statement.setString(index++, old.getStatus().name());
            }
            statement.setString(index++, medicationId);
            statement.setString(index, scheduledAt);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Não foi possível desfazer o registro no histórico.", exception);
        }
    }

    /** Registros com horário previsto em [start, end). Os valores usam o formato AAAA-MM-DDTHH:mm. */
    public List<MedicationHistory> findByScheduledBetween(String start, String endExclusive) {
        String sql = "SELECT " + SELECT_COLUMNS + """
                 FROM medication_history
                WHERE scheduled_at >= ? AND scheduled_at < ?
                ORDER BY scheduled_at DESC, id DESC
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, start);
            statement.setString(2, endExclusive);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<MedicationHistory> entries = new ArrayList<>();
                while (resultSet.next()) {
                    entries.add(map(resultSet));
                }
                return entries;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Não foi possível carregar o histórico.", exception);
        }
    }

    private Connection openConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA busy_timeout = 5000");
        }
        return connection;
    }

    private void bind(PreparedStatement statement, MedicationHistory history) throws SQLException {
        statement.setString(1, history.getMedicationId());
        statement.setString(2, history.getMedicationName());
        statement.setString(3, history.getDosage());
        statement.setString(4, history.getScheduledAt());
        statement.setString(5, history.getActionAt());
        statement.setString(6, history.getStatus().name());
    }

    private MedicationHistory map(ResultSet resultSet) throws SQLException {
        return new MedicationHistory(
                resultSet.getLong("id"),
                resultSet.getString("medication_id"),
                resultSet.getString("medication_name"),
                resultSet.getString("dosage"),
                resultSet.getString("scheduled_at"),
                resultSet.getString("action_at"),
                DoseStatus.valueOf(resultSet.getString("status"))
        );
    }
}
