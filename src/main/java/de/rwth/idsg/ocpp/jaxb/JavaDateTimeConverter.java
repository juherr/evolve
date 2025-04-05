/*
 * SteVe - SteckdosenVerwaltung - https://github.com/steve-community/steve
 * Copyright (C) 2013-2025 SteVe Community Team
 * All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.rwth.idsg.ocpp.jaxb;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

/**
 * Java-Time and XSD represent data and time information according to ISO 8601.
 *
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 20.10.2014
 */
public class JavaDateTimeConverter extends XmlAdapter<String, OffsetDateTime> {

    // Flexible ISO 8601 parser: supports the optional fraction and optional offset
    private static final DateTimeFormatter ISO_PARSER = new DateTimeFormatterBuilder()
        // Date: yyyy-MM-dd
        .appendValue(ChronoField.YEAR, 4, 10, java.time.format.SignStyle.EXCEEDS_PAD)
        .appendLiteral('-')
        .appendValue(ChronoField.MONTH_OF_YEAR, 2)
        .appendLiteral('-')
        .appendValue(ChronoField.DAY_OF_MONTH, 2)

        // Time: 'T'HH:mm:ss
        .appendLiteral('T')
        .appendValue(ChronoField.HOUR_OF_DAY, 2)
        .appendLiteral(':')
        .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
        .appendLiteral(':')
        .appendValue(ChronoField.SECOND_OF_MINUTE, 2)

        // Optional: .SSS... (fractional seconds)
        .optionalStart()
        .appendLiteral('.')
        .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, false)
        .optionalEnd()

        // Optional: +02:00 or Z (offset)
        .optionalStart()
        .appendOffsetId()
        .optionalEnd()

        .toFormatter(Locale.ROOT);

    // Outputs UTC timestamps with millisecond precision and trailing 'Z'
    private static final DateTimeFormatter UTC_FORMATTER = new DateTimeFormatterBuilder()
        .appendInstant(3)
        .toFormatter(Locale.ROOT);

    @Override
    public OffsetDateTime unmarshal(String v) throws Exception {
        if (isNullOrEmpty(v)) {
            return null;
        } else {
            TemporalAccessor parsed = ISO_PARSER.parse(v);
            if (parsed.isSupported(ChronoField.OFFSET_SECONDS)) {
                // Has explicit offset → safe to parse as OffsetDateTime
                return OffsetDateTime.from(parsed).withOffsetSameInstant(ZoneOffset.UTC);
            } else {
                // No offset → treat as UTC
                LocalDateTime ldt = LocalDateTime.from(parsed);
                return ldt.atOffset(ZoneOffset.UTC);
            }
        }
    }

    @Override
    public String marshal(OffsetDateTime v) throws Exception {
        if (v == null) {
            return null;
        } else {
            // Always output in UTC with `.SSS` and `Z`
            return UTC_FORMATTER.format(v.withOffsetSameInstant(ZoneOffset.UTC));
        }
    }

    /**
     * Because I did not want to include Guava or similar only for this.
     */
    private static boolean isNullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }
}
