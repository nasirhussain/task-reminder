# Task Reminder

Lightweight Java 17 desktop application for daily weekday task reminders.

## Stack

- JavaFX
- MVVM
- FXML
- CSS
- SQLite
- Gradle

## Database

The app stores tasks in a local SQLite database at `~/.task-reminder/tasks.sqlite`.
The application creates the database file and `tasks` table automatically.

## Commands

```bash
./gradlew test
./gradlew run
```

Postponed tasks are hidden from the Daily tab until their reminder date, but remain editable from the All Tasks tab.

## Import and Export

Use `Export Data` to save all tasks to a JSON backup file.
Use `Import Data` to load a JSON backup file. The app asks whether to:

- Merge: add imported tasks while keeping current tasks
- Replace: delete current tasks, then import the backup
