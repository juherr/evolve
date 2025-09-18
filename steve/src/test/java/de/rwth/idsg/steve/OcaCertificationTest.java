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
package de.rwth.idsg.steve;

import de.rwth.idsg.steve.utils.Helpers;
import de.rwth.idsg.steve.utils.LogFileRetriever;
import de.rwth.idsg.steve.utils.SteveConfigurationReader;
import de.rwth.idsg.steve.utils.__DatabasePreparer__;
import lombok.extern.slf4j.Slf4j;
import ocpp.cs._2015._10.ChargePointErrorCode;
import ocpp.cs._2015._10.ChargePointStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.OffsetDateTime;
import java.util.stream.Stream;

import static de.rwth.idsg.steve.utils.Helpers.getWsPath;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class OcaCertificationTest {

    private static Stream<Arguments> ocppClients() {
        return Stream.of(
                Arguments.of(new Ocpp12SoapClient(REGISTERED_CHARGE_BOX_ID, httpPath)),
                Arguments.of(new Ocpp15SoapClient(REGISTERED_CHARGE_BOX_ID, httpPath)),
                Arguments.of(new Ocpp16SoapClient(REGISTERED_CHARGE_BOX_ID, httpPath)),
                Arguments.of(new Ocpp12JsonClient(REGISTERED_CHARGE_BOX_ID, getWsPath(config))),
                Arguments.of(new Ocpp15JsonClient(REGISTERED_CHARGE_BOX_ID, getWsPath(config))),
                Arguments.of(new Ocpp16JsonClient(REGISTERED_CHARGE_BOX_ID, getWsPath(config))));
    }

    private static final String REGISTERED_CHARGE_BOX_ID = __DatabasePreparer__.getRegisteredChargeBoxId();
    private static final String REGISTERED_OCPP_TAG = __DatabasePreparer__.getRegisteredOcppTag();

    private static SteveConfiguration config;
    private static String httpPath;
    private static Application app;

    @BeforeAll
    public static void initClass() throws Exception {
        config = SteveConfigurationReader.readSteveConfiguration("main.properties");
        assertThat(config.getProfile()).isEqualTo(ApplicationProfile.TEST);

        httpPath = Helpers.getHttpPath(config);

        app = new Application(config, new LogFileRetriever());
        app.start();
    }

    @AfterAll
    public static void destroyClass() throws Exception {
        app.stop();
    }

    @BeforeEach
    public void init() {
        __DatabasePreparer__.prepare(config);
    }

    @AfterEach
    public void destroy() {
        __DatabasePreparer__.cleanUp();
    }

    @ParameterizedTest
    @MethodSource("ocppClients")
    public void testBootNotification(OcppTestClient client) {
        client.bootNotification("test-vendor", "test-model");
    }

    @ParameterizedTest
    @MethodSource("ocppClients")
    public void testStartTransaction(OcppTestClient client) {
        client.bootNotification("test-vendor", "test-model");
        client.startTransaction(1, REGISTERED_OCPP_TAG, 0, OffsetDateTime.now());
    }

    @ParameterizedTest
    @MethodSource("ocppClients")
    public void testStopTransaction(OcppTestClient client) {
        client.bootNotification("test-vendor", "test-model");
        var transactionId = __DatabasePreparer__.startTransaction();
        client.stopTransaction(transactionId, 100, OffsetDateTime.now());
    }

    @ParameterizedTest
    @MethodSource("ocppClients")
    public void testMeterValues(OcppTestClient client) {
        client.bootNotification("test-vendor", "test-model");
        var transactionId = __DatabasePreparer__.startTransaction();
        client.meterValues(1, transactionId, OffsetDateTime.now(), "50");
    }

    @ParameterizedTest
    @MethodSource("ocppClients")
    public void testHeartbeat(OcppTestClient client) {
        client.bootNotification("test-vendor", "test-model");
        client.heartbeat();
    }

    @ParameterizedTest
    @MethodSource("ocppClients")
    public void testStatusNotification(OcppTestClient client) {
        client.bootNotification("test-vendor", "test-model");
        client.statusNotification(
                1, ChargePointStatus.CHARGING.value(), ChargePointErrorCode.NO_ERROR.value(), OffsetDateTime.now());
    }
}
