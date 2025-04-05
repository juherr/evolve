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
package jaxb;

import de.rwth.idsg.ocpp.jaxb.JavaDateTimeConverter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.TimeZone;
import java.util.stream.Stream;

public class JavaDateTimeConverterTest {

    private final de.rwth.idsg.ocpp.jaxb.JavaDateTimeConverter converter = new JavaDateTimeConverter();

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    // -------------------------------------------------------------------------
    // Marshal
    // -------------------------------------------------------------------------

    @Test
    public void testMarshallNullInput() throws Exception {
        String val = converter.marshal(null);
        Assertions.assertNull(val);
    }

    @ParameterizedTest
    @MethodSource("provideValidInput")
    public void testMarshallEmptyInput(String val, String expected) throws Exception {
        OffsetDateTime input = converter.unmarshal(val);
        String output = converter.marshal(input);
        Assertions.assertEquals(expected, output);
    }

    // -------------------------------------------------------------------------
    // Unmarshal
    // -------------------------------------------------------------------------

    @Test
    public void testUnmarshallNullInput() throws Exception {
        converter.unmarshal(null);
    }

    @Test
    public void testUnmarshallEmptyInput() throws Exception {
        converter.unmarshal("");
    }

    @ParameterizedTest
    @MethodSource("provideValidInput")
    public void testUnmarshalValid(String val) throws Exception {
        converter.unmarshal(val);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidInput")
    public void testUnmarshalInvalid(String val) {
        Assertions.assertThrows(DateTimeParseException.class, () -> {
            converter.unmarshal(val);
        });
    }

    /**
     * First argument is used for marshaling only.
     * Both arguments are used for unmarshaling: We use the second as the expected output of formatting.
     */
    private static Stream<Arguments> provideValidInput() {
        return Stream.of(
            Arguments.of("2022-06-30T01:20:52", "2022-06-30T01:20:52.000Z"),
            Arguments.of("2022-06-30T01:20:52+02:00", "2022-06-29T23:20:52.000Z"),
            Arguments.of("2022-06-30T01:20:52Z", "2022-06-30T01:20:52.000Z"),
            Arguments.of("2022-06-30T01:20:52+00:00", "2022-06-30T01:20:52.000Z"),
            Arguments.of("2022-06-30T01:20:52.126", "2022-06-30T01:20:52.126Z"),
            Arguments.of("2022-06-30T01:20:52.126+05:00", "2022-06-29T20:20:52.126Z"),
            Arguments.of("2018-11-13T20:20:39+00:00", "2018-11-13T20:20:39.000Z"),
            Arguments.of("-2022-06-30T01:20:52", "-2022-06-30T01:20:52.000Z")
        );
    }

    private static Stream<Arguments> provideInvalidInput() {
        return Stream.of(
            Arguments.of("-1"),
            Arguments.of("10000"), // https://github.com/steve-community/steve/issues/1292
            Arguments.of("text"),
            Arguments.of("2022-06-30"), // no time
            Arguments.of("2022-06-30T01:20"), // seconds are required
            Arguments.of("2022-06-30T25:20:34"), // hour out of range
            Arguments.of("22-06-30T25:20:34") // year not YYYY-format
        );
    }
}
