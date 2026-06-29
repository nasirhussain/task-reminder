package com.valamis.learning.taskreminder.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ReminderPolicyTest {

    private final ReminderPolicy policy = new ReminderPolicy();

    @Test
    void openTaskWithoutReminderIsVisibleOnWeekday() {
        Task task = task(TaskStatus.OPEN, null);

        assertTrue(policy.isVisible(task, LocalDate.of(2026, 6, 29)));
    }

    @Test
    void holdTaskWithoutReminderIsVisibleOnWeekday() {
        Task task = task(TaskStatus.HOLD, null);

        assertTrue(policy.isVisible(task, LocalDate.of(2026, 6, 30)));
    }

    @Test
    void tasksAreHiddenOnWeekends() {
        Task task = task(TaskStatus.OPEN, null);

        assertFalse(policy.isVisible(task, LocalDate.of(2026, 7, 4)));
        assertFalse(policy.isVisible(task, LocalDate.of(2026, 7, 5)));
    }

    @Test
    void doneTaskIsNeverVisibleEvenWhenReminderDateIsReached() {
        Task task = task(TaskStatus.DONE, LocalDate.of(2026, 6, 29));

        assertFalse(policy.isVisible(task, LocalDate.of(2026, 6, 29)));
    }

    @Test
    void futureReminderDateHidesOpenOrHoldTaskUntilDateArrives() {
        Task openTask = task(TaskStatus.OPEN, LocalDate.of(2026, 7, 3));
        Task holdTask = task(TaskStatus.HOLD, LocalDate.of(2026, 7, 3));

        assertFalse(policy.isVisible(openTask, LocalDate.of(2026, 7, 2)));
        assertFalse(policy.isVisible(holdTask, LocalDate.of(2026, 7, 2)));
        assertTrue(policy.isVisible(openTask, LocalDate.of(2026, 7, 3)));
        assertTrue(policy.isVisible(holdTask, LocalDate.of(2026, 7, 3)));
    }

    @Test
    void reachedReminderDateKeepsTaskVisibleOnLaterWeekdays() {
        Task task = task(TaskStatus.OPEN, LocalDate.of(2026, 7, 3));

        assertTrue(policy.isVisible(task, LocalDate.of(2026, 7, 6)));
    }

    private Task task(TaskStatus status, LocalDate reminderDate) {
        LocalDateTime timestamp = LocalDateTime.of(2026, 6, 29, 9, 0);
        return new Task(1L, "Daily report", "Send summary", status, timestamp, reminderDate, timestamp);
    }
}
