package com.valamis.learning.taskreminder.ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Optional;

/**
 * Parses and formats the reminder-date field shown in the task inspector.
 *
 * <p>The {@link javafx.scene.control.DatePicker} editor accepts free-form text, so user input must be
 * validated before it reaches the service layer. Parsing is strict ({@code uuuu-MM-dd}) so impossible
 * dates such as {@code 2026-13-40} are rejected instead of being silently coerced.
 */
public final class ReminderDateParser {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

    private ReminderDateParser() {
    }

    /**
     * Parses reminder-date text entered by the user.
     *
     * @param text raw editor text; {@code null} or blank means "no reminder date"
     * @return the parsed date, or {@link Optional#empty()} when the field is blank
     * @throws IllegalArgumentException when the text is present but not a valid {@code yyyy-MM-dd} date
     */
    public static Optional<LocalDate> parse(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(text.trim(), FORMAT));
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "Reminder date must be a valid date in yyyy-MM-dd format", exception);
        }
    }

    /**
     * Formats a reminder date for display in the editor.
     *
     * @param date the date to format, or {@code null}
     * @return the {@code yyyy-MM-dd} text, or an empty string when {@code date} is {@code null}
     */
    public static String format(LocalDate date) {
        return date == null ? "" : FORMAT.format(date);
    }
}
