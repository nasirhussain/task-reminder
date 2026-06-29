# Code Documentation

## Overview

Task Reminder is a Java 17 desktop application built with JavaFX, FXML, CSS, MVVM-style view models, SQLite, and Gradle.

The application manages recurring weekday tasks, reminder dates, status changes, and JSON import/export.

## Architecture

### Domain

- `Task` is the immutable task record.
- `TaskStatus` defines `OPEN`, `HOLD`, and `DONE`.
- `ReminderPolicy` decides whether a task is visible on a given date.

### Service Layer

- `TaskService` owns task use cases:
  - create
  - update
  - status changes
  - reminder changes
  - daily task filtering
  - import normalization
- `TaskDataTransferService` owns JSON import/export.
- `ImportMode` defines import behavior:
  - `MERGE`
  - `REPLACE`

### Persistence

- `TaskRepository` is the persistence boundary.
- `SqliteTaskRepository` stores tasks in SQLite at `~/.task-reminder/tasks.sqlite`.
- `replaceAll` runs inside a SQLite transaction so replace imports roll back on failure.

### UI

- `TaskReminderApplication` starts JavaFX and wires dependencies.
- `TaskListViewModel` exposes observable task lists and UI operations.
- `TaskReminderController` binds FXML controls to the view model.
- `task-reminder.fxml` defines layout.
- `task-reminder.css` defines the visual style.

## Data Model

Each task contains:

- `id`
- `taskName`
- `remarks`
- `status`
- `createdDate`
- `customReminderDate`
- `updatedDate`

Imported tasks receive fresh IDs but preserve `createdDate` and `updatedDate`.

## Import And Export

Export creates a versioned JSON file:

```json
{
  "version": 1,
  "tasks": []
}
```

Import validates the JSON before changing data. Missing or `null` `tasks` is rejected. An explicit empty task list is valid.

Replace imports use `TaskRepository.replaceAll`, which is transactional for SQLite.

## Build And Test

```bash
./gradlew clean build
./gradlew test
./gradlew run
```

Tests cover:

- reminder visibility
- task service behavior
- SQLite persistence
- transactional replace imports
- JSON import/export safety
- view-model task list separation
