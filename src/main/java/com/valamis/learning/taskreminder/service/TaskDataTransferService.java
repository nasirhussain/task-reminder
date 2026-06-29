package com.valamis.learning.taskreminder.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.valamis.learning.taskreminder.domain.Task;
import com.valamis.learning.taskreminder.domain.TaskStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public final class TaskDataTransferService {

    private static final int EXPORT_VERSION = 1;

    private final TaskService taskService;
    private final ObjectMapper objectMapper;

    public TaskDataTransferService(TaskService taskService) {
        this.taskService = Objects.requireNonNull(taskService, "taskService");
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    public int exportTo(Path file) {
        Objects.requireNonNull(file, "file");
        TaskExport export = new TaskExport(
                EXPORT_VERSION,
                taskService.allTasks().stream().map(ExportedTask::from).toList());
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            objectMapper.writeValue(file.toFile(), export);
            return export.tasks().size();
        } catch (IOException exception) {
            throw new TaskDataTransferException("Failed to export task data", exception);
        }
    }

    public int importFrom(Path file, ImportMode mode) {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(mode, "mode");
        try {
            TaskExport export = objectMapper.readValue(file.toFile(), TaskExport.class);
            validateExport(export);
            List<Task> importedTasks = export.tasks().stream()
                    .map(ExportedTask::toTask)
                    .toList();
            taskService.importTasks(importedTasks, mode);
            return importedTasks.size();
        } catch (IOException exception) {
            throw new TaskDataTransferException("Failed to import task data", exception);
        }
    }

    private void validateExport(TaskExport export) {
        if (export == null) {
            throw new IllegalArgumentException("Import file must contain an export object");
        }
        if (export.version() != EXPORT_VERSION) {
            throw new IllegalArgumentException("Unsupported import file version: " + export.version());
        }
        if (export.tasks() == null) {
            throw new IllegalArgumentException("Import file must contain a tasks array");
        }
        for (int index = 0; index < export.tasks().size(); index++) {
            validateExportedTask(export.tasks().get(index), index);
        }
    }

    private void validateExportedTask(ExportedTask task, int index) {
        if (task == null) {
            throw new IllegalArgumentException("Task at index " + index + " is missing");
        }
        requireField(task.taskName(), "taskName", index);
        requireField(task.status(), "status", index);
        requireField(task.createdDate(), "createdDate", index);
        requireField(task.updatedDate(), "updatedDate", index);
    }

    private void requireField(Object value, String field, int index) {
        if (value == null) {
            throw new IllegalArgumentException("Task at index " + index + " is missing required field: " + field);
        }
    }

    private record TaskExport(int version, List<ExportedTask> tasks) {
        private TaskExport {
            tasks = tasks == null ? null : List.copyOf(tasks);
        }
    }

    private record ExportedTask(
            String taskName,
            String remarks,
            TaskStatus status,
            @JsonFormat(shape = JsonFormat.Shape.STRING) LocalDateTime createdDate,
            @JsonFormat(shape = JsonFormat.Shape.STRING) LocalDate customReminderDate,
            @JsonFormat(shape = JsonFormat.Shape.STRING) LocalDateTime updatedDate) {

        static ExportedTask from(Task task) {
            return new ExportedTask(
                    task.taskName(),
                    task.remarks(),
                    task.status(),
                    task.createdDate(),
                    task.customReminderDate(),
                    task.updatedDate());
        }

        Task toTask() {
            return new Task(null, taskName, remarks, status, createdDate, customReminderDate, updatedDate);
        }
    }
}
