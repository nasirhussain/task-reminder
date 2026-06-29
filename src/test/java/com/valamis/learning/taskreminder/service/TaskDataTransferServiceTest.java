package com.valamis.learning.taskreminder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.valamis.learning.taskreminder.domain.ReminderPolicy;
import com.valamis.learning.taskreminder.domain.Task;
import com.valamis.learning.taskreminder.domain.TaskStatus;
import com.valamis.learning.taskreminder.persistence.InMemoryTaskRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TaskDataTransferServiceTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-06-29T08:15:30Z"), ZoneId.of("UTC"));

    @TempDir
    Path tempDir;

    private final InMemoryTaskRepository repository = new InMemoryTaskRepository();
    private final TaskService taskService = new TaskService(repository, new ReminderPolicy(), CLOCK);
    private final TaskDataTransferService transferService = new TaskDataTransferService(taskService);

    @Test
    void exportsAllTasksToVersionedJson() throws Exception {
        taskService.createTask(new NewTaskRequest("Open now", "Visible", TaskStatus.OPEN, null));
        taskService.createTask(new NewTaskRequest("Later", "", TaskStatus.HOLD, LocalDate.of(2026, 7, 3)));
        Path exportFile = tempDir.resolve("backup.json");

        int exported = transferService.exportTo(exportFile);

        String json = Files.readString(exportFile);
        assertEquals(2, exported);
        assertTrue(json.contains("\"version\""));
        assertTrue(json.contains("\"Open now\""));
        assertTrue(json.contains("\"customReminderDate\""));
    }

    @Test
    void mergeImportAppendsImportedTasksWithFreshIds() throws Exception {
        taskService.createTask(new NewTaskRequest("Existing", "", TaskStatus.OPEN, null));
        Path exportFile = tempDir.resolve("backup.json");
        transferService.exportTo(exportFile);
        taskService.createTask(new NewTaskRequest("Local only", "", TaskStatus.HOLD, null));

        int imported = transferService.importFrom(exportFile, ImportMode.MERGE);

        List<String> names = taskService.allTasks().stream().map(Task::taskName).sorted().toList();
        assertEquals(1, imported);
        assertEquals(List.of("Existing", "Existing", "Local only"), names);
        assertEquals(3, taskService.allTasks().stream().map(Task::id).distinct().count());
    }

    @Test
    void replaceImportClearsExistingTasksBeforeImporting() throws Exception {
        taskService.createTask(new NewTaskRequest("Backup only", "", TaskStatus.OPEN, null));
        Path exportFile = tempDir.resolve("backup.json");
        transferService.exportTo(exportFile);
        taskService.createTask(new NewTaskRequest("Remove me", "", TaskStatus.HOLD, null));

        int imported = transferService.importFrom(exportFile, ImportMode.REPLACE);

        assertEquals(1, imported);
        assertEquals(List.of("Backup only"), taskService.allTasks().stream().map(Task::taskName).toList());
    }

    @Test
    void replaceImportDoesNotClearExistingTasksWhenImportFileIsInvalid() throws Exception {
        taskService.createTask(new NewTaskRequest("Keep me", "", TaskStatus.OPEN, null));
        Path importFile = tempDir.resolve("invalid.json");
        Files.writeString(importFile, """
                {
                  "version": 1,
                  "tasks": [
                    {
                      "taskName": null,
                      "remarks": "",
                      "status": "OPEN",
                      "createdDate": "2026-06-29T08:15:30",
                      "customReminderDate": null,
                      "updatedDate": "2026-06-29T08:15:30"
                    }
                  ]
                }
                """);

        assertThrows(RuntimeException.class, () -> transferService.importFrom(importFile, ImportMode.REPLACE));

        assertEquals(List.of("Keep me"), taskService.allTasks().stream().map(Task::taskName).toList());
    }

    @Test
    void replaceImportDoesNotClearExistingTasksWhenImportFileHasBlankTaskName() throws Exception {
        taskService.createTask(new NewTaskRequest("Keep me", "", TaskStatus.OPEN, null));
        Path importFile = tempDir.resolve("blank-name.json");
        Files.writeString(importFile, """
                {
                  "version": 1,
                  "tasks": [
                    {
                      "taskName": "   ",
                      "remarks": "",
                      "status": "OPEN",
                      "createdDate": "2026-06-29T08:15:30",
                      "customReminderDate": null,
                      "updatedDate": "2026-06-29T08:15:30"
                    }
                  ]
                }
                """);

        assertThrows(IllegalArgumentException.class, () -> transferService.importFrom(importFile, ImportMode.REPLACE));

        assertEquals(List.of("Keep me"), taskService.allTasks().stream().map(Task::taskName).toList());
    }

    @Test
    void replaceImportDoesNotClearExistingTasksWhenTasksFieldIsMissing() throws Exception {
        taskService.createTask(new NewTaskRequest("Keep me", "", TaskStatus.OPEN, null));
        Path importFile = tempDir.resolve("missing-tasks.json");
        Files.writeString(importFile, """
                {
                  "version": 1
                }
                """);

        assertThrows(IllegalArgumentException.class, () -> transferService.importFrom(importFile, ImportMode.REPLACE));

        assertEquals(List.of("Keep me"), taskService.allTasks().stream().map(Task::taskName).toList());
    }

    @Test
    void replaceImportDoesNotClearExistingTasksWhenTasksFieldIsNull() throws Exception {
        taskService.createTask(new NewTaskRequest("Keep me", "", TaskStatus.OPEN, null));
        Path importFile = tempDir.resolve("null-tasks.json");
        Files.writeString(importFile, """
                {
                  "version": 1,
                  "tasks": null
                }
                """);

        assertThrows(IllegalArgumentException.class, () -> transferService.importFrom(importFile, ImportMode.REPLACE));

        assertEquals(List.of("Keep me"), taskService.allTasks().stream().map(Task::taskName).toList());
    }

    @Test
    void replaceImportAcceptsExplicitEmptyTaskList() throws Exception {
        taskService.createTask(new NewTaskRequest("Remove me", "", TaskStatus.OPEN, null));
        Path importFile = tempDir.resolve("empty-tasks.json");
        Files.writeString(importFile, """
                {
                  "version": 1,
                  "tasks": []
                }
                """);

        int imported = transferService.importFrom(importFile, ImportMode.REPLACE);

        assertEquals(0, imported);
        assertTrue(taskService.allTasks().isEmpty());
    }

    @Test
    void importPreservesExportedUpdatedDate() throws Exception {
        taskService.createTask(new NewTaskRequest("Original", "", TaskStatus.OPEN, null));
        Path exportFile = tempDir.resolve("backup.json");
        transferService.exportTo(exportFile);
        taskService.deleteAllTasks();

        transferService.importFrom(exportFile, ImportMode.MERGE);

        Task imported = taskService.allTasks().get(0);
        assertEquals(LocalDateTime.of(2026, 6, 29, 8, 15, 30), imported.updatedDate());
    }

    @Test
    void importReportsClearMessageWhenRequiredFieldIsMissing() throws Exception {
        taskService.createTask(new NewTaskRequest("Keep me", "", TaskStatus.OPEN, null));
        Path importFile = tempDir.resolve("missing-field.json");
        Files.writeString(importFile, """
                {
                  "version": 1,
                  "tasks": [
                    {
                      "taskName": "Valid",
                      "remarks": "",
                      "status": "OPEN",
                      "createdDate": null,
                      "customReminderDate": null,
                      "updatedDate": "2026-06-29T08:15:30"
                    }
                  ]
                }
                """);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> transferService.importFrom(importFile, ImportMode.MERGE));

        assertTrue(exception.getMessage().contains("createdDate"));
        assertEquals(List.of("Keep me"), taskService.allTasks().stream().map(Task::taskName).toList());
    }
}
