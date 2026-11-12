// AI assistance: ChatGPT (OpenAI) assisted with implementation; reviewed and adapted by Nicolas.
package com.meditrack.backend.medication;

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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Repository
public class MedicationRepository {
    private static final String SELECT_COLUMNS =
            "id, name, dosage, quantity, frequency, dose_times, start_date, end_date, low_stock_threshold";

    private final String jdbcUrl;

    public MedicationRepository(Environment environment) {
        try {
            String databasePath = environment.getProperty("meditrack.database.path", "./data/meditrack.db");
            Path path = Paths.get(databasePath).toAbsolutePath();
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            this.jdbcUrl = "jdbc:sqlite:" + path;
            initializeSchema();
        } catch (IOException | SQLException exception) {
            throw new IllegalStateException("Não foi possível inicializar o banco SQLite.", exception);
        }
    }

    private void initializeSchema() throws SQLException {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS medications (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        dosage TEXT NOT NULL,
                        quantity INTEGER NOT NULL CHECK (quantity >= 0),
                        frequency TEXT NOT NULL,
                        dose_times TEXT NOT NULL,
                        start_date TEXT NOT NULL,
                        end_date TEXT NOT NULL,
                        low_stock_threshold INTEGER NOT NULL CHECK (low_stock_threshold >= 0)
                    )
                    """);
        }
    }

    public List<Medication> findAll() {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM medications ORDER BY name COLLATE NOCASE";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Medication> medications = new ArrayList<>();
            while (resultSet.next()) {
                medications.add(map(resultSet));
            }
            return medications;
        } catch (SQLException exception) {
            throw new IllegalStateException("Não foi possível carregar os medicamentos.", exception);
        }
    }

    public Optional<Medication> findById(String id) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM medications WHERE id = ?";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Não foi possível consultar o medicamento.", exception);
        }
    }

    public void insert(Medication medication) {
        String sql = """
                INSERT INTO medications
                (id, name, dosage, quantity, frequency, dose_times, start_date, end_date, low_stock_threshold)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, medication);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Não foi possível cadastrar o medicamento.", exception);
        }
    }

    public boolean update(Medication medication) {
        String sql = """
                UPDATE medications
                SET name = ?, dosage = ?, quantity = ?, frequency = ?, dose_times = ?,
                    start_date = ?, end_date = ?, low_stock_threshold = ?
                WHERE id = ?
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, medication.getName());
            statement.setString(2, medication.getDosage());
            statement.setInt(3, medication.getQuantity());
            statement.setString(4, medication.getFrequency());
            statement.setString(5, encodeTimes(medication.getDoseTimes()));
            statement.setString(6, medication.getStartDate());
            statement.setString(7, medication.getEndDate());
            statement.setInt(8, medication.getLowStockThreshold());
            statement.setString(9, medication.getId());
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new IllegalStateException("Não foi possível atualizar o medicamento.", exception);
        }
    }

    public boolean adjustStock(String id, int quantityChange) {
        String sql = """
                UPDATE medications
                SET quantity = quantity + ?
                WHERE id = ? AND quantity + ? >= 0 AND quantity + ? <= 2147483647
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, quantityChange);
            statement.setString(2, id);
            statement.setInt(3, quantityChange);
            statement.setInt(4, quantityChange);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new IllegalStateException("Não foi possível ajustar o estoque.", exception);
        }
    }

    public boolean decreaseStockForDose(String id) {
        String sql = "UPDATE medications SET quantity = quantity - 1 WHERE id = ? AND quantity > 0";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new IllegalStateException("Não foi possível descontar a dose do estoque.", exception);
        }
    }

    public boolean delete(String id) {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM medications WHERE id = ?")) {
            statement.setString(1, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new IllegalStateException("Não foi possível excluir o medicamento.", exception);
        }
    }

    private Connection openConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA busy_timeout = 5000");
        }
        return connection;
    }

    private void bind(PreparedStatement statement, Medication medication) throws SQLException {
        statement.setString(1, medication.getId());
        statement.setString(2, medication.getName());
        statement.setString(3, medication.getDosage());
        statement.setInt(4, medication.getQuantity());
        statement.setString(5, medication.getFrequency());
        statement.setString(6, encodeTimes(medication.getDoseTimes()));
        statement.setString(7, medication.getStartDate());
        statement.setString(8, medication.getEndDate());
        statement.setInt(9, medication.getLowStockThreshold());
    }

    private Medication map(ResultSet resultSet) throws SQLException {
        List<String> doseTimes = Arrays.asList(resultSet.getString("dose_times").split(","));
        return new Medication(
                resultSet.getString("id"),
                resultSet.getString("name"),
                resultSet.getString("dosage"),
                resultSet.getInt("quantity"),
                resultSet.getString("frequency"),
                doseTimes,
                resultSet.getString("start_date"),
                resultSet.getString("end_date"),
                resultSet.getInt("low_stock_threshold")
        );
    }

    private String encodeTimes(List<String> doseTimes) {
        return String.join(",", doseTimes);
    }
}
