package com.valamis.learning.taskreminder.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public record Task(
        Long id,
        String taskName,
        String remarks,
        TaskStatus status,
        LocalDateTime createdDate,
        LocalDate customReminderDate,
        LocalDateTime updatedDate) {

    public Task {
        Objects.requireNonNull(taskName, "taskName");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdDate, "createdDate");
        Objects.requireNonNull(updatedDate, "updatedDate");
    }

    public Task withId(long newId) {
        return new Task(newId, taskName, remarks, status, createdDate, customReminderDate, updatedDate);
    }

    public Task withStatus(TaskStatus newStatus, LocalDateTime newUpdatedDate) {
        return new Task(id, taskName, remarks, newStatus, createdDate, customReminderDate, newUpdatedDate);
    }
}
