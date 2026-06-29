# User Guide

## Daily Task Reminder

Daily Task Reminder helps you manage weekday recurring tasks. Tasks appear on working days, Monday through Friday, unless they are completed or postponed with a future reminder date.

## Task Status

### OPEN

Use `OPEN` for active tasks. Open tasks appear in the Daily tab when they are due.

### HOLD

Use `HOLD` for paused or postponed tasks. Hold tasks still appear in the Daily tab when they are due.

### DONE

Use `DONE` for completed tasks. Done tasks do not appear in the Daily tab, but they remain visible in the All Tasks tab.

## Daily Tab

The Daily tab shows only tasks due today.

- OPEN tasks appear in the Open Tasks table.
- HOLD tasks appear in the Hold Tasks table.
- DONE tasks are hidden.
- Tasks with a future reminder date are hidden until that date.

## All Tasks Tab

The All Tasks tab shows every task, including postponed and completed tasks.

Use this tab when you need to edit, delete, or clear the reminder date for a task that is not currently due.

## Creating A Task

1. Click `New`.
2. Enter a task name.
3. Choose a status.
4. Optionally enter remarks.
5. Optionally enter a reminder date in `yyyy-MM-dd` format.
6. Click `Save`.

## Editing A Task

1. Select a task from Daily or All Tasks.
2. Update the fields in Task Inspector.
3. Click `Save`.

## Postponing A Task

1. Select a task.
2. Enter a future reminder date using `yyyy-MM-dd`.
3. Click `Save`.

The task will disappear from Daily until the reminder date arrives. It remains available in All Tasks.

## Clearing A Reminder

1. Select a task.
2. Click `Clear Reminder`.

The task resumes normal weekday visibility based on its status.

## Changing Status

Select a task and click:

- `Open`
- `Hold`
- `Done`

Changing a task to DONE removes it from Daily.

## Import And Export

### Export Data

Click `Export Data` to save all tasks to a JSON backup file.

### Import Data

Click `Import Data` and choose a JSON backup file.

You will be asked to choose:

- `Merge`: add imported tasks while keeping current tasks
- `Replace`: delete current tasks, then import backup tasks

Use Replace only when you want the backup file to become the complete current task list.

## Local Data

The app stores data in:

`~/.task-reminder/tasks.sqlite`

You normally do not need to edit this file directly.
