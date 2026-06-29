package com.valamis.learning.taskreminder.ui;

import com.valamis.learning.taskreminder.domain.TaskStatus;
import com.valamis.learning.taskreminder.service.ImportMode;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public final class TaskReminderController {

    private static final DateTimeFormatter UPDATED_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final TaskListViewModel viewModel;

    @FXML private TableView<TaskRowViewModel> openTable;
    @FXML private TableView<TaskRowViewModel> holdTable;
    @FXML private TableView<TaskRowViewModel> allTable;
    @FXML private TextField taskNameField;
    @FXML private TextArea remarksArea;
    @FXML private ComboBox<TaskStatus> statusComboBox;
    @FXML private DatePicker reminderDatePicker;

    public TaskReminderController(TaskListViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @FXML
    private void initialize() {
        configureTable(openTable);
        configureTable(holdTable);
        configureTable(allTable);
        statusComboBox.getItems().setAll(TaskStatus.values());
        statusComboBox.setValue(TaskStatus.OPEN);

        openTable.setItems(viewModel.openTasks());
        holdTable.setItems(viewModel.holdTasks());
        allTable.setItems(viewModel.allTasks());
        bindSelection(openTable);
        bindSelection(holdTable);
        bindSelection(allTable);
        viewModel.refresh();
    }

    @FXML
    private void newTask() {
        clearForm();
    }

    @FXML
    private void saveTask() {
        try {
            TaskRowViewModel selected = selectedTask();
            LocalDate reminderDate = reminderDatePicker.getValue();
            if (selected == null) {
                viewModel.createTask(taskNameField.getText(), remarksArea.getText(), statusComboBox.getValue(), reminderDate);
            } else {
                viewModel.updateTask(
                        selected.id(),
                        taskNameField.getText(),
                        remarksArea.getText(),
                        statusComboBox.getValue(),
                        reminderDate);
            }
            clearForm();
        } catch (IllegalArgumentException exception) {
            showWarning("Invalid Task", messageOf(exception));
        }
    }

    @FXML
    private void markOpen() {
        changeSelectedStatus(TaskStatus.OPEN);
    }

    @FXML
    private void markHold() {
        changeSelectedStatus(TaskStatus.HOLD);
    }

    @FXML
    private void markDone() {
        changeSelectedStatus(TaskStatus.DONE);
    }

    @FXML
    private void clearReminder() {
        TaskRowViewModel selected = selectedTask();
        if (selected != null) {
            viewModel.setReminderDate(selected.id(), null);
            reminderDatePicker.setValue(null);
        }
    }

    @FXML
    private void deleteTask() {
        TaskRowViewModel selected = selectedTask();
        if (selected == null) {
            showWarning("No Task Selected", "Select a task first.");
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete task \"" + selected.taskName() + "\"?", ButtonType.YES, ButtonType.NO);
        alert.setHeaderText("Delete Task");
        alert.showAndWait().filter(ButtonType.YES::equals).ifPresent(button -> {
            viewModel.deleteTask(selected.id());
            clearForm();
        });
    }

    @FXML
    private void refresh() {
        viewModel.refresh();
    }

    @FXML
    private void exportData() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Task Data");
        chooser.setInitialFileName("task-reminder-backup.json");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
        File selectedFile = chooser.showSaveDialog(window());
        if (selectedFile == null) {
            return;
        }
        try {
            int exported = viewModel.exportData(selectedFile.toPath());
            showInformation("Export Complete", "Exported " + exported + " tasks.");
        } catch (RuntimeException exception) {
            showWarning("Export Failed", messageOf(exception));
        }
    }

    @FXML
    private void importData() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import Task Data");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
        File selectedFile = chooser.showOpenDialog(window());
        if (selectedFile == null) {
            return;
        }

        Optional<ImportMode> mode = chooseImportMode();
        if (mode.isEmpty()) {
            return;
        }

        try {
            int imported = viewModel.importData(selectedFile.toPath(), mode.get());
            clearForm();
            showInformation("Import Complete", "Imported " + imported + " tasks using " + mode.get().name().toLowerCase() + " mode.");
        } catch (RuntimeException exception) {
            showWarning("Import Failed", messageOf(exception));
        }
    }

    @FXML
    private void showUserGuide() {
        try {
            TextArea guideContent = new TextArea(UserGuideText.load());
            guideContent.setEditable(false);
            guideContent.setWrapText(true);
            guideContent.setPrefSize(760, 560);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("User Guide");
            alert.getDialogPane().setContent(guideContent);
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            alert.getButtonTypes().setAll(ButtonType.OK);
            alert.showAndWait();
        } catch (RuntimeException exception) {
            showWarning("User Guide Unavailable", messageOf(exception));
        }
    }

    private void configureTable(TableView<TaskRowViewModel> table) {
        for (TableColumn<TaskRowViewModel, ?> column : table.getColumns()) {
            switch (column.getId()) {
                case "taskNameColumn" -> setStringCellFactory(column, TaskRowViewModel::taskName);
                case "remarksColumn" -> setStringCellFactory(column, TaskRowViewModel::remarks);
                case "statusColumn" -> setStatusCellFactory(column);
                case "reminderColumn" -> setStringCellFactory(column, task -> task.customReminderDate() == null ? "" : task.customReminderDate().toString());
                case "updatedColumn" -> setStringCellFactory(column, task -> task.updatedDate().format(UPDATED_FORMATTER));
                default -> {
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void setStringCellFactory(TableColumn<TaskRowViewModel, ?> column, java.util.function.Function<TaskRowViewModel, String> valueProvider) {
        TableColumn<TaskRowViewModel, String> typedColumn = (TableColumn<TaskRowViewModel, String>) column;
        typedColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(valueProvider.apply(cell.getValue())));
    }

    @SuppressWarnings("unchecked")
    private void setStatusCellFactory(TableColumn<TaskRowViewModel, ?> column) {
        TableColumn<TaskRowViewModel, String> typedColumn = (TableColumn<TaskRowViewModel, String>) column;
        typedColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().status().name()));
        typedColumn.setCellFactory(tableColumn -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(status);
                badge.getStyleClass().addAll("status-badge", statusStyle(status));
                setGraphic(badge);
            }
        });
    }

    private String statusStyle(String status) {
        return switch (status) {
            case "OPEN" -> "status-open";
            case "HOLD" -> "status-hold";
            case "DONE" -> "status-done";
            default -> "status-done";
        };
    }

    private void bindSelection(TableView<TaskRowViewModel> table) {
        table.getSelectionModel().selectedItemProperty().addListener((observable, previous, selected) -> {
            if (selected != null) {
                if (table != openTable) {
                    openTable.getSelectionModel().clearSelection();
                }
                if (table != holdTable) {
                    holdTable.getSelectionModel().clearSelection();
                }
                if (table != allTable) {
                    allTable.getSelectionModel().clearSelection();
                }
                populateForm(selected);
            }
        });
    }

    private TaskRowViewModel selectedTask() {
        if (openTable.getSelectionModel().getSelectedItem() != null) {
            return openTable.getSelectionModel().getSelectedItem();
        }
        if (holdTable.getSelectionModel().getSelectedItem() != null) {
            return holdTable.getSelectionModel().getSelectedItem();
        }
        return allTable.getSelectionModel().getSelectedItem();
    }

    private void populateForm(TaskRowViewModel task) {
        taskNameField.setText(task.taskName());
        remarksArea.setText(task.remarks());
        statusComboBox.setValue(task.status());
        reminderDatePicker.setValue(task.customReminderDate());
    }

    private void clearForm() {
        openTable.getSelectionModel().clearSelection();
        holdTable.getSelectionModel().clearSelection();
        allTable.getSelectionModel().clearSelection();
        taskNameField.clear();
        remarksArea.clear();
        statusComboBox.setValue(TaskStatus.OPEN);
        reminderDatePicker.setValue(null);
    }

    private void changeSelectedStatus(TaskStatus status) {
        TaskRowViewModel selected = selectedTask();
        if (selected == null) {
            showWarning("No Task Selected", "Select a task first.");
            return;
        }
        viewModel.changeStatus(selected.id(), status);
        clearForm();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setHeaderText(title);
        alert.showAndWait();
    }

    private Optional<ImportMode> chooseImportMode() {
        ButtonType merge = new ButtonType("Merge");
        ButtonType replace = new ButtonType("Replace");
        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Merge adds imported tasks. Replace deletes current tasks before importing.",
                merge,
                replace,
                ButtonType.CANCEL);
        alert.setHeaderText("Import Data");
        return alert.showAndWait()
                .filter(button -> button != ButtonType.CANCEL)
                .map(button -> button == replace ? ImportMode.REPLACE : ImportMode.MERGE);
    }

    private void showInformation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setHeaderText(title);
        alert.showAndWait();
    }

    private Window window() {
        return taskNameField.getScene().getWindow();
    }

    private static String messageOf(Throwable exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? "Unexpected error: " + exception.getClass().getSimpleName()
                : message;
    }
}
