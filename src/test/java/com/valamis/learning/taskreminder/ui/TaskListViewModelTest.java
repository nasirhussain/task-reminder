package com.valamis.learning.taskreminder.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.valamis.learning.taskreminder.domain.ReminderPolicy;
import com.valamis.learning.taskreminder.domain.TaskStatus;
import com.valamis.learning.taskreminder.persistence.InMemoryTaskRepository;
import com.valamis.learning.taskreminder.service.NewTaskRequest;
import com.valamis.learning.taskreminder.service.TaskDataTransferService;
import com.valamis.learning.taskreminder.service.TaskService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class TaskListViewModelTest {

    private final InMemoryTaskRepository repository = new InMemoryTaskRepository();
    private final TaskService service = new TaskService(
            repository,
            new ReminderPolicy(),
            Clock.fixed(Instant.parse("2026-06-29T08:15:30Z"), ZoneId.of("UTC")));
    private final TaskListViewModel viewModel = new TaskListViewModel(service, new TaskDataTransferService(service), Clock.fixed(
            Instant.parse("2026-06-29T08:15:30Z"),
            ZoneId.of("UTC")));

    @Test
    void refreshSeparatesDailyTasksFromAllManageableTasks() {
        service.createTask(new NewTaskRequest("Visible", "", TaskStatus.OPEN, null));
        service.createTask(new NewTaskRequest("Postponed", "", TaskStatus.OPEN, LocalDate.of(2026, 7, 3)));
        service.createTask(new NewTaskRequest("Done", "", TaskStatus.DONE, null));

        viewModel.refresh();

        assertEquals(List.of("Visible"), viewModel.openTasks().stream().map(TaskRowViewModel::taskName).toList());
        assertTrue(viewModel.holdTasks().isEmpty());
        assertEquals(
                List.of("Done", "Postponed", "Visible"),
                viewModel.allTasks().stream().map(TaskRowViewModel::taskName).sorted().toList());
    }

    @Test
    void searchFiltersByTaskNameCaseInsensitively() {
        service.createTask(new NewTaskRequest("Pay bills", "", TaskStatus.OPEN, null));
        service.createTask(new NewTaskRequest("Call vendor", "", TaskStatus.OPEN, null));
        viewModel.refresh();

        viewModel.setSearchQuery("PAY");

        assertEquals(List.of("Pay bills"), viewModel.openTasks().stream().map(TaskRowViewModel::taskName).toList());
    }

    @Test
    void searchMatchesRemarksAcrossDailyAndAllLists() {
        service.createTask(new NewTaskRequest("Call vendor", "about the invoice", TaskStatus.HOLD, null));
        service.createTask(new NewTaskRequest("Review PR", "", TaskStatus.HOLD, null));
        viewModel.refresh();

        viewModel.setSearchQuery("invoice");

        assertEquals(List.of("Call vendor"), viewModel.holdTasks().stream().map(TaskRowViewModel::taskName).toList());
        assertEquals(List.of("Call vendor"), viewModel.allTasks().stream().map(TaskRowViewModel::taskName).toList());
    }

    @Test
    void clearingSearchRestoresEveryTask() {
        service.createTask(new NewTaskRequest("Pay bills", "", TaskStatus.OPEN, null));
        service.createTask(new NewTaskRequest("Call vendor", "", TaskStatus.OPEN, null));
        viewModel.refresh();
        viewModel.setSearchQuery("pay");

        viewModel.setSearchQuery("");

        assertEquals(2, viewModel.openTasks().size());
    }

    @Test
    void searchFiltersLoadedSnapshotWithoutQueryingTheRepositoryAgain() {
        service.createTask(new NewTaskRequest("Pay bills", "", TaskStatus.OPEN, null));
        viewModel.refresh();
        repository.deleteAll();

        viewModel.setSearchQuery("pay");

        assertEquals(List.of("Pay bills"), viewModel.openTasks().stream().map(TaskRowViewModel::taskName).toList());
    }
}
