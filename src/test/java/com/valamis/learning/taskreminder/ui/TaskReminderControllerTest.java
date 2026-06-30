package com.valamis.learning.taskreminder.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import javafx.util.StringConverter;
import org.junit.jupiter.api.Test;

class TaskReminderControllerTest {

    private final StringConverter<LocalDate> converter = TaskReminderController.createReminderDateConverter();

    @Test
    void reminderDateConverterTreatsBlankTextAsNoReminder() {
        assertNull(converter.fromString(""));
    }

    @Test
    void reminderDateConverterParsesValidDateText() {
        assertEquals(LocalDate.of(2026, 7, 3), converter.fromString("2026-07-03"));
    }

    @Test
    void reminderDateConverterRejectsInvalidTextSoSaveValidationCanStillSeeIt() {
        assertThrows(IllegalArgumentException.class, () -> converter.fromString("tomorrow"));
    }
}
