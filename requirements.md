# Daily Task Reminder Application Requirements

## 1. Overview

Build a lightweight desktop application using Java 17 for macOS that acts as a personal daily task reminder and tracker.

The application should focus on simplicity and fast task management.

Primary features:

* Create and manage daily recurring tasks
* Support task reminders
* Track task status
* Display pending tasks every working day (Monday–Friday)
* Allow postponing tasks using custom reminder dates

---

# 2. Technology Requirements

## Platform

* macOS desktop application

## Java Version

* Java 17

## Suggested UI Options

Preferred:

* JavaFX

Alternative:

* Swing

## Storage

Preferred:

* Embedded database (SQLite)

Alternative:

* JSON file storage

## Build Tool

Recommended:

* Maven

Alternative:

* Gradle

---

# 3. Functional Requirements

## 3.1 Task Model

Each task must contain the following fields:

| Field              | Type          | Required | Description                           |
| ------------------ | ------------- | -------- | ------------------------------------- |
| id                 | UUID / Long   | Yes      | Unique identifier                     |
| taskName           | String        | Yes      | Task title                            |
| remarks            | String        | No       | Additional notes                      |
| status             | Enum          | Yes      | OPEN / HOLD / DONE                    |
| createdDate        | LocalDateTime | Yes      | Task creation timestamp               |
| customReminderDate | LocalDate     | No       | Future date when task should reappear |
| updatedDate        | LocalDateTime | Yes      | Last modified timestamp               |

---

# 4. Task Status Rules

## OPEN

* Task is active
* Appears in daily task list
* Can have custom reminder date

## HOLD

* Task temporarily postponed
* Appears in daily task list
* Can have custom reminder date

## DONE

* Task completed
* Should NOT appear in daily task list
* Reminder date ignored

---

# 5. Daily Task Visibility Logic

## Working Days

The application should show daily tasks only on:

* Monday
* Tuesday
* Wednesday
* Thursday
* Friday

Optional future enhancement:

* Configurable working days

---

# 6. Task Display Rules

## Case 1: OPEN or HOLD without custom reminder date

Task should appear every weekday continuously.

Example:

* Task status = OPEN
* customReminderDate = null

Result:

* Visible every Monday–Friday

---

## Case 2: OPEN or HOLD with future custom reminder date

Task should remain hidden until the reminder date arrives.

Example:

* Today = 2026-06-29
* customReminderDate = 2026-07-03

Result:

* Hidden until 2026-07-03
* Visible from 2026-07-03 onward

---

## Case 3: Reminder date reached

Once the reminder date arrives:

* Task should appear every weekday
* Continue appearing daily until:

  * another custom reminder date is set
  * OR status becomes DONE

---

## Case 4: DONE status

Task should never appear in daily task view.

---

# 7. Main Screens

## 7.1 Daily Tasks Screen

Primary application screen.

### Sections

#### OPEN Tasks

* List all visible OPEN tasks

#### HOLD Tasks

* List all visible HOLD tasks

### Each task row should show:

* Task name
* Remarks
* Status
* Reminder date (if any)
* Last updated date

### Actions

* Change status
* Edit task
* Set reminder date
* Delete task

---

## 7.2 Add/Edit Task Screen

### Fields

* Task Name
* Remarks
* Status dropdown
* Custom Reminder Date picker

### Buttons

* Save
* Cancel

---

# 8. Reminder Date Behaviour

## Setting Reminder Date

When user sets a future reminder date:

* Task should disappear from current daily list
* Reappear automatically on reminder date

## Updating Reminder Date

User can:

* change reminder date multiple times

## Clearing Reminder Date

If reminder date removed:

* task should immediately resume daily visibility

---

# 9. Status Change Behaviour

| From      | To        | Expected Result                       |
| --------- | --------- | ------------------------------------- |
| OPEN      | HOLD      | Move to HOLD group                    |
| HOLD      | OPEN      | Move to OPEN group                    |
| OPEN/HOLD | DONE      | Remove from daily list                |
| DONE      | OPEN/HOLD | Visible again based on reminder logic |

---

# 10. User Experience Requirements

## General

* Fast startup
* Lightweight UI
* Minimal clicks
* Clean desktop layout

## Recommended UI Features

* Search tasks
* Sort by updated date
* Keyboard shortcuts
* Double-click to edit
* Color indication for statuses

---

# 11. Data Persistence

Application should persist tasks between restarts.

Recommended persistence strategy:

* SQLite database

## Suggested Tables

### tasks

| Column               | Type           |
| -------------------- | -------------- |
| id                   | INTEGER / UUID |
| task_name            | TEXT           |
| remarks              | TEXT           |
| status               | TEXT           |
| custom_reminder_date | DATE           |
| created_date         | TIMESTAMP      |
| updated_date         | TIMESTAMP      |

---

# 12. Suggested Application Structure

## Packages

```text
com.example.taskreminder
├── model
├── repository
├── service
├── ui
├── util
└── config
```

---

# 13. Suggested Classes

## Model

* Task
* TaskStatus

## Repository

* TaskRepository

## Service

* TaskService
* ReminderService

## UI

* MainWindow
* TaskFormDialog

---

# 14. Core Business Logic

## Visible Task Calculation

A task should be visible if:

```text
status != DONE
AND
(
    customReminderDate IS NULL
    OR
    customReminderDate <= today
)
AND
today is Monday-Friday
```

---

# 15. Future Enhancements (Optional)

* System tray support
* macOS notifications
* Recurring reminder intervals
* Tags/categories
* Dark mode
* Backup/export
* Cloud sync
* Mobile companion app

---

# 16. Non-Functional Requirements

## Performance

* Handle at least 10,000 tasks smoothly

## Reliability

* No data loss during restart

## Compatibility

* macOS Intel
* macOS Apple Silicon

## Packaging

Preferred distribution:

* DMG installer
* Native macOS app bundle

---

# 17. Suggested Libraries

## UI

* JavaFX

## Database

* SQLite JDBC Driver

## Utilities

* Lombok (optional)
* Jackson (if JSON storage used)

---

# 18. Example User Flow

## Scenario

1. User creates task:

   * "Submit tax document"

2. Status:

   * OPEN

3. User sets reminder:

   * 2026-07-10

4. Result:

   * Task hidden until 2026-07-10

5. On 2026-07-10:

   * Task appears again daily

6. User marks DONE:

   * Task removed permanently from daily view

---

# 19. Recommended Architecture

Recommended pattern:

* MVC
  or
* MVVM (preferred for JavaFX)

---

# 20. Deliverables

The project should include:

* Source code
* Maven/Gradle build
* SQLite schema
* README.md
* macOS runnable package
* Unit tests for visibility logic
