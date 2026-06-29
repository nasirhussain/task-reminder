package com.valamis.learning.taskreminder.service;

import com.valamis.learning.taskreminder.domain.TaskStatus;
import java.time.LocalDate;
import java.util.Objects;

public record UpdateTaskRequest(String taskName, String remarks, TaskStatus status, LocalDate customReminderDate) {

    public UpdateTaskRequest {
        Objects.requireNonNull(status, "status");
    }
}
