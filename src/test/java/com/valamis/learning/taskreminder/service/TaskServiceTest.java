package com.valamis.learning.taskreminder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.valamis.learning.taskreminder.domain.ReminderPolicy;
import com.valamis.learning.taskreminder.domain.Task;
import com.valamis.learning.taskreminder.domain.TaskStatus;
import com.valamis.learning.taskreminder.persistence.InMemoryTaskRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TaskServiceTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-06-29T08:15:30Z"), ZoneId.of("UTC"));

    private final InMemoryTaskRepository repository = new InMemoryTaskRepository();
    private final TaskService service = new TaskService(repository, new ReminderPolicy(), CLOCK);

    @Test
    void createTaskRequiresNonBlankName() {
        NewTaskRequest request = new NewTaskRequest("  ", "remarks", TaskStatus.OPEN, null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.createTask(request));

        assertEquals("Task name is required", exception.getMessage());
    }

    @Test
    void createTaskTrimsNameAndRemarksAndSetsTimestamps() {
        Task created = service.createTask(new NewTaskRequest("  Pay bills  ", "  due today  ", TaskStatus.OPEN, null));

        assertEquals("Pay bills", created.taskName());
        assertEquals("due today", created.remarks());
        assertEquals(TaskStatus.OPEN, created.status());
        assertEquals(LocalDateTime.of(2026, 6, 29, 8, 15, 30), created.createdDate());
        assertEquals(created.createdDate(), created.updatedDate());
        assertTrue(created.id() > 0);
    }

    @Test
    void updateTaskCanClearReminderDateAndKeepsCreatedDate() {
        Task created = service.createTask(
                new NewTaskRequest("Follow up", "Initial", TaskStatus.HOLD, LocalDate.of(2026, 7, 3)));

        Task updated = service.updateTask(created.id(), new UpdateTaskRequest("Follow up updated", "", TaskStatus.OPEN, null));

        assertEquals(created.createdDate(), updated.createdDate());
        assertEquals("Follow up updated", updated.taskName());
        assertEquals("", updated.remarks());
        assertEquals(TaskStatus.OPEN, updated.status());
        assertEquals(Optional.empty(), Optional.ofNullable(updated.customReminderDate()));
        assertEquals(LocalDateTime.of(2026, 6, 29, 8, 15, 30), updated.updatedDate());
    }

    @Test
    void updateMissingTaskFails() {
        UpdateTaskRequest request = new UpdateTaskRequest("Missing", "", TaskStatus.OPEN, null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.updateTask(999L, request));

        assertEquals("Task not found: 999", exception.getMessage());
    }

    @Test
    void changeStatusMovesTaskBetweenDailyGroups() {
        Task created = service.createTask(new NewTaskRequest("Review queue", "", TaskStatus.OPEN, null));

        DailyTasks before = service.dailyTasks(LocalDate.of(2026, 6, 29));
        Task changed = service.changeStatus(created.id(), TaskStatus.HOLD);
        DailyTasks after = service.dailyTasks(LocalDate.of(2026, 6, 29));

        assertEquals(TaskStatus.HOLD, changed.status());
        assertEquals(1, before.openTasks().size());
        assertEquals(0, before.holdTasks().size());
        assertEquals(0, after.openTasks().size());
        assertEquals(1, after.holdTasks().size());
    }

    @Test
    void dailyTasksExcludeDoneAndFutureReminderTasks() {
        service.createTask(new NewTaskRequest("Open now", "", TaskStatus.OPEN, null));
        service.createTask(new NewTaskRequest("Hold now", "", TaskStatus.HOLD, null));
        service.createTask(new NewTaskRequest("Done", "", TaskStatus.DONE, null));
        service.createTask(new NewTaskRequest("Later", "", TaskStatus.OPEN, LocalDate.of(2026, 7, 3)));

        DailyTasks dailyTasks = service.dailyTasks(LocalDate.of(2026, 6, 29));

        assertEquals(List.of("Open now"), dailyTasks.openTasks().stream().map(Task::taskName).toList());
        assertEquals(List.of("Hold now"), dailyTasks.holdTasks().stream().map(Task::taskName).toList());
    }

    @Test
    void deleteTaskRemovesItFromDailyTasks() {
        Task created = service.createTask(new NewTaskRequest("Delete me", "", TaskStatus.OPEN, null));

        service.deleteTask(created.id());

        assertTrue(service.dailyTasks(LocalDate.of(2026, 6, 29)).openTasks().isEmpty());
    }

    @Test
    void setReminderDateHidesTaskUntilReminderDateAndClearReminderMakesItVisibleAgain() {
        Task created = service.createTask(new NewTaskRequest("Call vendor", "", TaskStatus.OPEN, null));

        Task postponed = service.setReminderDate(created.id(), LocalDate.of(2026, 7, 3));

        assertEquals(LocalDate.of(2026, 7, 3), postponed.customReminderDate());
        assertTrue(service.dailyTasks(LocalDate.of(2026, 6, 29)).openTasks().isEmpty());
        assertEquals(1, service.dailyTasks(LocalDate.of(2026, 7, 3)).openTasks().size());

        Task resumed = service.setReminderDate(created.id(), null);

        assertEquals(Optional.empty(), Optional.ofNullable(resumed.customReminderDate()));
        assertEquals(1, service.dailyTasks(LocalDate.of(2026, 6, 29)).openTasks().size());
    }

    @Test
    void completedTaskCanBecomeVisibleAgainBasedOnReminderDate() {
        Task created = service.createTask(
                new NewTaskRequest("Renew cert", "", TaskStatus.DONE, LocalDate.of(2026, 6, 29)));

        service.changeStatus(created.id(), TaskStatus.OPEN);

        assertEquals(1, service.dailyTasks(LocalDate.of(2026, 6, 29)).openTasks().size());
    }

    @Test
    void dailyTasksAreEmptyOnWeekend() {
        service.createTask(new NewTaskRequest("Weekday only", "", TaskStatus.OPEN, null));

        DailyTasks dailyTasks = service.dailyTasks(LocalDate.of(2026, 7, 4));

        assertTrue(dailyTasks.openTasks().isEmpty());
        assertTrue(dailyTasks.holdTasks().isEmpty());
    }
}
