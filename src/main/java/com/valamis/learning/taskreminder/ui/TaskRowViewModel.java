package com.valamis.learning.taskreminder.ui;

import com.valamis.learning.taskreminder.domain.Task;
import com.valamis.learning.taskreminder.domain.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskRowViewModel(
        long id,
        String taskName,
        String remarks,
        TaskStatus status,
        LocalDate customReminderDate,
        LocalDateTime updatedDate) {

    public static TaskRowViewModel from(Task task) {
        return new TaskRowViewModel(
                task.id(),
                task.taskName(),
                task.remarks(),
                task.status(),
                task.customReminderDate(),
                task.updatedDate());
    }
}
