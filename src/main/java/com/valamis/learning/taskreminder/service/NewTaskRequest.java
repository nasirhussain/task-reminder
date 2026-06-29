package com.valamis.learning.taskreminder.service;

import com.valamis.learning.taskreminder.domain.TaskStatus;
import java.time.LocalDate;
import java.util.Objects;

public record NewTaskRequest(String taskName, String remarks, TaskStatus status, LocalDate customReminderDate) {

    public NewTaskRequest {
        Objects.requireNonNull(status, "status");
    }
}
