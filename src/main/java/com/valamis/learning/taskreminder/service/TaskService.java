package com.valamis.learning.taskreminder.service;

import com.valamis.learning.taskreminder.domain.ReminderPolicy;
import com.valamis.learning.taskreminder.domain.Task;
import com.valamis.learning.taskreminder.domain.TaskStatus;
import com.valamis.learning.taskreminder.persistence.TaskRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public final class TaskService {

    private final TaskRepository repository;
    private final ReminderPolicy reminderPolicy;
    private final Clock clock;

    public TaskService(TaskRepository repository, ReminderPolicy reminderPolicy, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.reminderPolicy = Objects.requireNonNull(reminderPolicy, "reminderPolicy");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public Task createTask(NewTaskRequest request) {
        String taskName = normalizeRequiredName(request.taskName());
        LocalDateTime now = LocalDateTime.now(clock);
        Task task = new Task(
                null,
                taskName,
                normalizeRemarks(request.remarks()),
                request.status(),
                now,
                request.customReminderDate(),
                now);
        return repository.save(task);
    }

    public Task updateTask(long id, UpdateTaskRequest request) {
        Task existing = findRequired(id);
        LocalDateTime now = LocalDateTime.now(clock);
        Task updated = new Task(
                existing.id(),
                normalizeRequiredName(request.taskName()),
                normalizeRemarks(request.remarks()),
                request.status(),
                existing.createdDate(),
                request.customReminderDate(),
                now);
        return repository.save(updated);
    }

    public Task changeStatus(long id, TaskStatus status) {
        Task existing = findRequired(id);
        return repository.save(existing.withStatus(status, LocalDateTime.now(clock)));
    }

    public Task setReminderDate(long id, LocalDate reminderDate) {
        Task existing = findRequired(id);
        LocalDateTime now = LocalDateTime.now(clock);
        Task updated = new Task(
                existing.id(),
                existing.taskName(),
                existing.remarks(),
                existing.status(),
                existing.createdDate(),
                reminderDate,
                now);
        return repository.save(updated);
    }

    public void deleteTask(long id) {
        repository.deleteById(id);
    }

    public void deleteAllTasks() {
        repository.deleteAll();
    }

    public List<Task> importTasks(List<Task> tasks, ImportMode mode) {
        Objects.requireNonNull(tasks, "tasks");
        Objects.requireNonNull(mode, "mode");
        List<Task> importedTasks = tasks.stream()
                .map(this::normalizeImportedTask)
                .toList();
        if (mode == ImportMode.REPLACE) {
            return repository.replaceAll(importedTasks);
        }
        return repository.saveAll(importedTasks);
    }

    public DailyTasks dailyTasks(LocalDate today) {
        List<Task> visibleTasks = repository.findAll().stream()
                .filter(task -> reminderPolicy.isVisible(task, today))
                .toList();
        List<Task> openTasks = visibleTasks.stream()
                .filter(task -> task.status() == TaskStatus.OPEN)
                .toList();
        List<Task> holdTasks = visibleTasks.stream()
                .filter(task -> task.status() == TaskStatus.HOLD)
                .toList();
        return new DailyTasks(openTasks, holdTasks);
    }

    public List<Task> allTasks() {
        return repository.findAll();
    }

    private Task findRequired(long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
    }

    private String normalizeRequiredName(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Task name is required");
        }
        return normalized;
    }

    private String normalizeRemarks(String value) {
        return value == null ? "" : value.trim();
    }

    private Task normalizeImportedTask(Task task) {
        Objects.requireNonNull(task, "task");
        return new Task(
                null,
                normalizeRequiredName(task.taskName()),
                normalizeRemarks(task.remarks()),
                task.status(),
                task.createdDate(),
                task.customReminderDate(),
                task.updatedDate());
    }
}
