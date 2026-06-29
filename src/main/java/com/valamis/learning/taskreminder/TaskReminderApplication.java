package com.valamis.learning.taskreminder;

import com.valamis.learning.taskreminder.domain.ReminderPolicy;
import com.valamis.learning.taskreminder.persistence.SqliteTaskRepository;
import com.valamis.learning.taskreminder.service.TaskDataTransferService;
import com.valamis.learning.taskreminder.service.TaskService;
import com.valamis.learning.taskreminder.ui.TaskReminderController;
import com.valamis.learning.taskreminder.ui.TaskListViewModel;
import java.io.IOException;
import java.time.Clock;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public final class TaskReminderApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        SqliteTaskRepository repository = new SqliteTaskRepository(SqliteTaskRepository.DEFAULT_DATABASE);
        repository.initialize();
        TaskService service = new TaskService(repository, new ReminderPolicy(), Clock.systemDefaultZone());
        TaskDataTransferService transferService = new TaskDataTransferService(service);
        TaskListViewModel viewModel = new TaskListViewModel(service, transferService, Clock.systemDefaultZone());

        FXMLLoader loader = new FXMLLoader(TaskReminderApplication.class.getResource("/com/valamis/learning/taskreminder/task-reminder.fxml"));
        loader.setControllerFactory(type -> {
            if (type == TaskReminderController.class) {
                return new TaskReminderController(viewModel);
            }
            throw new IllegalArgumentException("Unsupported controller: " + type);
        });

        Scene scene = new Scene(loader.load(), 1040, 680);
        scene.getStylesheets().add(TaskReminderApplication.class
                .getResource("/com/valamis/learning/taskreminder/task-reminder.css")
                .toExternalForm());
        stage.setTitle("Daily Task Reminder");
        stage.getIcons().add(new Image(TaskReminderApplication.class
                .getResourceAsStream("/com/valamis/learning/taskreminder/icons/app-icon.png")));
        stage.setMinWidth(920);
        stage.setMinHeight(620);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
