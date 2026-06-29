package com.valamis.learning.taskreminder.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.valamis.learning.taskreminder.domain.Task;
import com.valamis.learning.taskreminder.domain.TaskStatus;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteTaskRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void savesLoadsUpdatesAndDeletesTasksInIsolatedSqliteFile() throws Exception {
        Path database = tempDir.resolve("tasks-test.sqlite");
        SqliteTaskRepository repository = new SqliteTaskRepository(database);
        repository.initialize();
        LocalDateTime created = LocalDateTime.of(2026, 6, 29, 8, 15, 30);

        Task saved = repository.save(new Task(
                null,
                "Submit timesheet",
                "Before noon",
                TaskStatus.OPEN,
                created,
                LocalDate.of(2026, 7, 3),
                created));

        Task updated = repository.save(new Task(
                saved.id(),
                "Submit timesheet updated",
                "",
                TaskStatus.HOLD,
                saved.createdDate(),
                null,
                created.plusHours(1)));

        List<Task> tasks = repository.findAll();
        assertTrue(Files.exists(database));
        assertEquals(1, tasks.size());
        assertEquals(updated, tasks.get(0));
        assertEquals(updated, repository.findById(saved.id()).orElseThrow());

        repository.deleteById(saved.id());

        assertFalse(repository.findById(saved.id()).isPresent());
    }

    @Test
    void replaceAllRollsBackWhenAnyImportedTaskFailsToPersist() {
        SqliteTaskRepository repository = new SqliteTaskRepository(tempDir.resolve("rollback-test.sqlite"));
        repository.initialize();
        LocalDateTime timestamp = LocalDateTime.of(2026, 6, 29, 8, 15, 30);
        repository.save(new Task(null, "Keep me", "", TaskStatus.OPEN, timestamp, null, timestamp));

        List<Task> replacement = List.of(
                new Task(null, "Imported", "", TaskStatus.OPEN, timestamp, null, timestamp),
                new Task(null, "Broken", null, TaskStatus.OPEN, timestamp, null, timestamp));

        assertThrows(RuntimeException.class, () -> repository.replaceAll(replacement));

        assertEquals(List.of("Keep me"), repository.findAll().stream().map(Task::taskName).toList());
    }

    @Test
    void saveAllRollsBackEveryTaskWhenAnyTaskFailsToPersist() {
        SqliteTaskRepository repository = new SqliteTaskRepository(tempDir.resolve("save-all-rollback.sqlite"));
        repository.initialize();
        LocalDateTime timestamp = LocalDateTime.of(2026, 6, 29, 8, 15, 30);
        repository.save(new Task(null, "Existing", "", TaskStatus.OPEN, timestamp, null, timestamp));

        List<Task> batch = List.of(
                new Task(null, "Imported", "", TaskStatus.OPEN, timestamp, null, timestamp),
                new Task(null, "Broken", null, TaskStatus.OPEN, timestamp, null, timestamp));

        assertThrows(RuntimeException.class, () -> repository.saveAll(batch));

        assertEquals(List.of("Existing"), repository.findAll().stream().map(Task::taskName).toList());
    }

    @Test
    void initializeStampsSchemaVersionAndPreservesDataOnReinitialize() throws Exception {
        Path database = tempDir.resolve("migration-test.sqlite");
        SqliteTaskRepository repository = new SqliteTaskRepository(database);
        repository.initialize();
        LocalDateTime timestamp = LocalDateTime.of(2026, 6, 29, 8, 15, 30);
        repository.save(new Task(null, "Persisted", "", TaskStatus.OPEN, timestamp, null, timestamp));

        repository.initialize();

        assertEquals(1, readUserVersion(database));
        assertEquals(List.of("Persisted"), repository.findAll().stream().map(Task::taskName).toList());
    }

    @Test
    void findAllWrapsCorruptStatusRowInRepositoryException() throws Exception {
        Path database = tempDir.resolve("corrupt-row.sqlite");
        SqliteTaskRepository repository = new SqliteTaskRepository(database);
        repository.initialize();
        executeSql(database,
                "INSERT INTO tasks (task_name, remarks, status, created_date, custom_reminder_date, updated_date) "
                        + "VALUES ('Corrupt', '', 'NOT_A_STATUS', '2026-06-29T08:15:30', NULL, '2026-06-29T08:15:30')");

        assertThrows(TaskRepositoryException.class, repository::findAll);
    }

    private void executeSql(Path database, String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database.toAbsolutePath());
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private int readUserVersion(Path database) throws SQLException {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database.toAbsolutePath());
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("PRAGMA user_version")) {
            return resultSet.next() ? resultSet.getInt(1) : -1;
        }
    }
}
