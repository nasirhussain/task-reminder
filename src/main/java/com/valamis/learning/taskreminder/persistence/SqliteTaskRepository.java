package com.valamis.learning.taskreminder.persistence;

import com.valamis.learning.taskreminder.domain.Task;
import com.valamis.learning.taskreminder.domain.TaskStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SqliteTaskRepository implements TaskRepository {

    public static final Path DEFAULT_DATABASE =
            Path.of(System.getProperty("user.home"), ".task-reminder", "tasks.sqlite");

    private static final int SCHEMA_VERSION = 1;

    private final String jdbcUrl;

    public SqliteTaskRepository(Path databasePath) {
        this("jdbc:sqlite:" + databasePath.toAbsolutePath());
    }

    private SqliteTaskRepository(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public void initialize() {
        createParentDirectory();
        try (Connection connection = connect()) {
            migrate(connection);
        } catch (SQLException exception) {
            throw new TaskRepositoryException("Failed to initialize task schema", exception);
        }
    }

    private void migrate(Connection connection) throws SQLException {
        int version = readSchemaVersion(connection);
        if (version < 1) {
            applyInitialSchema(connection);
        }
        if (version != SCHEMA_VERSION) {
            setSchemaVersion(connection, SCHEMA_VERSION);
        }
    }

    private int readSchemaVersion(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("PRAGMA user_version")) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    private void setSchemaVersion(Connection connection, int version) throws SQLException {
        // PRAGMA statements cannot be parameterized; the value is an internal int constant, never user input.
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("PRAGMA user_version = " + version);
        }
    }

    private void applyInitialSchema(Connection connection) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    task_name TEXT NOT NULL,
                    remarks TEXT NOT NULL,
                    status TEXT NOT NULL,
                    created_date TEXT NOT NULL,
                    custom_reminder_date TEXT NULL,
                    updated_date TEXT NOT NULL
                )
                """;
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_tasks_status_reminder ON tasks (status, custom_reminder_date)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_tasks_updated_date ON tasks (updated_date)");
        }
    }

    @Override
    public Task save(Task task) {
        return task.id() == null ? insert(task) : update(task);
    }

    @Override
    public Optional<Task> findById(long id) {
        String sql = """
                SELECT id, task_name, remarks, status, created_date, custom_reminder_date, updated_date
                FROM tasks
                WHERE id = ?
                """;
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(toTask(resultSet)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new TaskRepositoryException("Failed to load task " + id, exception);
        }
    }

    @Override
    public List<Task> findAll() {
        String sql = """
                SELECT id, task_name, remarks, status, created_date, custom_reminder_date, updated_date
                FROM tasks
                ORDER BY updated_date DESC, task_name ASC
                """;
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            List<Task> tasks = new ArrayList<>();
            while (resultSet.next()) {
                tasks.add(toTask(resultSet));
            }
            return List.copyOf(tasks);
        } catch (SQLException exception) {
            throw new TaskRepositoryException("Failed to load tasks", exception);
        }
    }

    @Override
    public void deleteById(long id) {
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement("DELETE FROM tasks WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new TaskRepositoryException("Failed to delete task " + id, exception);
        }
    }

    @Override
    public void deleteAll() {
        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM tasks");
        } catch (SQLException exception) {
            throw new TaskRepositoryException("Failed to delete tasks", exception);
        }
    }

    @Override
    public List<Task> saveAll(List<Task> tasks) {
        return inTransaction("Failed to save tasks", connection -> insertAll(connection, tasks));
    }

    @Override
    public List<Task> replaceAll(List<Task> tasks) {
        return inTransaction("Failed to replace tasks", connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM tasks");
            }
            return insertAll(connection, tasks);
        });
    }

    private List<Task> insertAll(Connection connection, List<Task> tasks) throws SQLException {
        List<Task> saved = new ArrayList<>();
        for (Task task : tasks) {
            saved.add(insert(connection, task));
        }
        return List.copyOf(saved);
    }

    private <T> T inTransaction(String failureMessage, TransactionalWork<T> work) {
        try (Connection connection = connect()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                T result = work.execute(connection);
                connection.commit();
                connection.setAutoCommit(previousAutoCommit);
                return result;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                connection.setAutoCommit(previousAutoCommit);
                throw exception;
            }
        } catch (SQLException exception) {
            throw new TaskRepositoryException(failureMessage, exception);
        }
    }

    @FunctionalInterface
    private interface TransactionalWork<T> {
        T execute(Connection connection) throws SQLException;
    }

    private Task insert(Task task) {
        try (Connection connection = connect()) {
            return insert(connection, task);
        } catch (SQLException exception) {
            throw new TaskRepositoryException("Failed to save task", exception);
        }
    }

    private Task insert(Connection connection, Task task) throws SQLException {
        String sql = """
                INSERT INTO tasks (task_name, remarks, status, created_date, custom_reminder_date, updated_date)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindTaskFields(statement, task);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new TaskRepositoryException("Task insert did not return an id", null);
                }
                return task.withId(keys.getLong(1));
            }
        }
    }

    private Task update(Task task) {
        String sql = """
                UPDATE tasks
                SET task_name = ?, remarks = ?, status = ?, created_date = ?, custom_reminder_date = ?, updated_date = ?
                WHERE id = ?
                """;
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            bindTaskFields(statement, task);
            statement.setLong(7, task.id());
            int rows = statement.executeUpdate();
            if (rows == 0) {
                throw new TaskRepositoryException("Task not found for update: " + task.id(), null);
            }
            return task;
        } catch (SQLException exception) {
            throw new TaskRepositoryException("Failed to update task " + task.id(), exception);
        }
    }

    private void bindTaskFields(PreparedStatement statement, Task task) throws SQLException {
        statement.setString(1, task.taskName());
        statement.setString(2, task.remarks());
        statement.setString(3, task.status().name());
        statement.setString(4, task.createdDate().toString());
        if (task.customReminderDate() == null) {
            statement.setNull(5, Types.VARCHAR);
        } else {
            statement.setString(5, task.customReminderDate().toString());
        }
        statement.setString(6, task.updatedDate().toString());
    }

    private Task toTask(ResultSet resultSet) throws SQLException {
        long id = resultSet.getLong("id");
        String status = resultSet.getString("status");
        String createdDate = resultSet.getString("created_date");
        String updatedDate = resultSet.getString("updated_date");
        String reminderDate = resultSet.getString("custom_reminder_date");
        try {
            LocalDate customReminderDate =
                    reminderDate == null || reminderDate.isBlank() ? null : LocalDate.parse(reminderDate);
            return new Task(
                    id,
                    resultSet.getString("task_name"),
                    resultSet.getString("remarks"),
                    TaskStatus.valueOf(status),
                    LocalDateTime.parse(createdDate),
                    customReminderDate,
                    LocalDateTime.parse(updatedDate));
        } catch (IllegalArgumentException | DateTimeException exception) {
            throw new TaskRepositoryException("Failed to read task " + id + " from the database", exception);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    private void createParentDirectory() {
        if (!jdbcUrl.startsWith("jdbc:sqlite:")) {
            return;
        }
        Path databasePath = Path.of(jdbcUrl.substring("jdbc:sqlite:".length()));
        Path parent = databasePath.getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException exception) {
            throw new TaskRepositoryException("Failed to create SQLite database directory", exception);
        }
    }
}
