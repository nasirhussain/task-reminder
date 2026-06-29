package com.valamis.learning.taskreminder.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public final class ReminderPolicy {

    private static final Set<DayOfWeek> WORKING_DAYS = EnumSet.of(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY);

    public boolean isVisible(Task task, LocalDate today) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(today, "today");

        if (!WORKING_DAYS.contains(today.getDayOfWeek()) || task.status() == TaskStatus.DONE) {
            return false;
        }
        return task.customReminderDate() == null || !task.customReminderDate().isAfter(today);
    }
}
