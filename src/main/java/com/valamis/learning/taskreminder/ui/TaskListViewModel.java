package com.valamis.learning.taskreminder.ui;

import com.valamis.learning.taskreminder.domain.Task;
import com.valamis.learning.taskreminder.domain.TaskStatus;
import com.valamis.learning.taskreminder.service.DailyTasks;
import com.valamis.learning.taskreminder.service.ImportMode;
import com.valamis.learning.taskreminder.service.NewTaskRequest;
import com.valamis.learning.taskreminder.service.TaskDataTransferService;
import com.valamis.learning.taskreminder.service.TaskService;
import com.valamis.learning.taskreminder.service.UpdateTaskRequest;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public final class TaskListViewModel {

    private final TaskService taskService;
    private final TaskDataTransferService transferService;
    private final Clock clock;
    private final ObservableList<TaskRowViewModel> openTasks = FXCollections.observableArrayList();
    private final ObservableList<TaskRowViewModel> holdTasks = FXCollections.observableArrayList();
    private final ObservableList<TaskRowViewModel> allTasks = FXCollections.observableArrayList();

    public TaskListViewModel(TaskService taskService, TaskDataTransferService transferService, Clock clock) {
        this.taskService = Objects.requireNonNull(taskService, "taskService");
        this.transferService = Objects.requireNonNull(transferService, "transferService");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public ObservableList<TaskRowViewModel> openTasks() {
        return openTasks;
    }

    public ObservableList<TaskRowViewModel> holdTasks() {
        return holdTasks;
    }

    public ObservableList<TaskRowViewModel> allTasks() {
        return allTasks;
    }

    public void refresh() {
        DailyTasks dailyTasks = taskService.dailyTasks(LocalDate.now(clock));
        openTasks.setAll(dailyTasks.openTasks().stream().map(TaskRowViewModel::from).toList());
        holdTasks.setAll(dailyTasks.holdTasks().stream().map(TaskRowViewModel::from).toList());
        allTasks.setAll(taskService.allTasks().stream().map(TaskRowViewModel::from).toList());
    }

    public Task createTask(String taskName, String remarks, TaskStatus status, LocalDate reminderDate) {
        Task task = taskService.createTask(new NewTaskRequest(taskName, remarks, status, reminderDate));
        refresh();
        return task;
    }

    public Task updateTask(long id, String taskName, String remarks, TaskStatus status, LocalDate reminderDate) {
        Task task = taskService.updateTask(id, new UpdateTaskRequest(taskName, remarks, status, reminderDate));
        refresh();
        return task;
    }

    public Task changeStatus(long id, TaskStatus status) {
        Task task = taskService.changeStatus(id, status);
        refresh();
        return task;
    }

    public Task setReminderDate(long id, LocalDate reminderDate) {
        Task task = taskService.setReminderDate(id, reminderDate);
        refresh();
        return task;
    }

    public void deleteTask(long id) {
        taskService.deleteTask(id);
        refresh();
    }

    public int exportData(Path file) {
        return transferService.exportTo(file);
    }

    public int importData(Path file, ImportMode mode) {
        int imported = transferService.importFrom(file, mode);
        refresh();
        return imported;
    }
}
