# Task Reminder

Lightweight Java 17 JavaFX desktop application for daily weekday task reminders.

## Stack

- **Java 17** (Gradle toolchain)
- **JavaFX 17** (controls + FXML) — provided by the `org.openjfx.javafxplugin`, no manual SDK install needed
- **MVVM** with FXML views and CSS styling
- **SQLite** (via `xerial sqlite-jdbc`) for local storage
- **Jackson** for JSON import/export
- **JUnit 5** + **JaCoCo** for tests and coverage
- **Gradle** (wrapper included)

## Prerequisites

- A **JDK 17** installation. The Gradle toolchain targets Java 17; the macOS packaging tasks also rely on `jpackage`, which ships with the JDK.
- No separate Gradle install is required — use the bundled wrapper (`./gradlew`).

## Build

```bash
./gradlew build          # compile, run tests, and assemble
```

## Run

```bash
./gradlew run            # launch the JavaFX application
```

On first launch the app creates its SQLite database (see [Database](#database)).

## Test & coverage

```bash
./gradlew test               # run the JUnit 5 suite
./gradlew jacocoTestReport   # generate the coverage report
./gradlew check              # tests + coverage gate (fails on regression)
```

- HTML coverage report: `build/reports/jacoco/test/html/index.html`
- `check` enforces a JaCoCo gate of **≥80% line / ≥70% branch** on the domain, service, and persistence layers. UI controllers and the application bootstrap are excluded, since they hold no business logic and require a JavaFX harness to exercise.

## Package (macOS)

Build a native macOS bundle with `jpackage`:

```bash
./gradlew packageMacApp      # .app image  -> build/jpackage/app/
./gradlew packageMacDmg      # .dmg installer -> build/jpackage/dmg/
```

## Database

The app stores tasks in a local SQLite database at `~/.task-reminder/tasks.sqlite`.
The database file, `tasks` table, and indexes are created automatically on first run, and the schema is versioned via SQLite's `PRAGMA user_version` so future upgrades can migrate cleanly.

## Features

- **Daily weekday reminders.** Open and Hold tasks surface on weekdays only; weekends are clear.
- **Statuses.** Tasks move between `OPEN`, `HOLD`, and `DONE`.
- **Postponing.** Set a reminder date to hide a task from the Daily view until that date. Postponed tasks are hidden from the Daily tab until their reminder date, but remain editable from the All Tasks tab.
- **Built-in user guide** available from within the app.

## Import and export

- **Export Data** saves all tasks to a JSON backup file.
- **Import Data** loads a JSON backup file. The app asks whether to:
  - **Merge** — add imported tasks while keeping current tasks
  - **Replace** — delete current tasks, then import the backup

## Project structure

```
src/main/java/com/valamis/learning/taskreminder/
  domain/        # pure models + reminder rules (no I/O)
  service/       # task workflows, import/export orchestration
  persistence/   # SQLite repository (parameterized, transactional)
  ui/            # JavaFX controller + view models
  TaskReminderApplication.java   # application entry point
src/main/resources/...           # FXML, CSS, icons, bundled user guide
src/test/java/...                # JUnit 5 tests mirroring the source layout
```
