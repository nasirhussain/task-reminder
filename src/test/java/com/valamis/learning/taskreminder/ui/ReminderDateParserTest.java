package com.valamis.learning.taskreminder.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ReminderDateParserTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void blankInputParsesToEmpty(String input) {
        assertTrue(ReminderDateParser.parse(input).isEmpty());
    }

    @Test
    void validDateParsesIgnoringSurroundingWhitespace() {
        assertEquals(Optional.of(LocalDate.of(2026, 7, 3)), ReminderDateParser.parse("  2026-07-03 "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-13-40", "2026-02-30", "07/03/2026", "tomorrow", "2026-7-3"})
    void invalidDateThrowsWithActionableMessage(String input) {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> ReminderDateParser.parse(input));

        assertTrue(exception.getMessage().contains("yyyy-MM-dd"));
    }

    @Test
    void formatRendersDateAsIsoText() {
        assertEquals("2026-07-03", ReminderDateParser.format(LocalDate.of(2026, 7, 3)));
    }

    @Test
    void formatRendersNullAsEmptyString() {
        assertEquals("", ReminderDateParser.format(null));
    }
}
