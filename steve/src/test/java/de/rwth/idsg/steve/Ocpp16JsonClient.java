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

import de.rwth.idsg.steve.ocpp.OcppVersion;
import de.rwth.idsg.steve.ocpp.ws.JsonObjectMapper;
import de.rwth.idsg.steve.utils.OcppJsonChargePoint;
import ocpp.cs._2015._10.AuthorizationStatus;
import ocpp.cs._2015._10.BootNotificationRequest;
import ocpp.cs._2015._10.ChargePointErrorCode;
import ocpp.cs._2015._10.ChargePointStatus;
import ocpp.cs._2015._10.HeartbeatRequest;
import ocpp.cs._2015._10.MeterValue;
import ocpp.cs._2015._10.MeterValuesRequest;
import ocpp.cs._2015._10.StartTransactionRequest;
import ocpp.cs._2015._10.StatusNotificationRequest;
import ocpp.cs._2015._10.StopTransactionRequest;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.byLessThan;

public class Ocpp16JsonClient implements OcppTestClient {

    private final OcppJsonChargePoint client;

    public Ocpp16JsonClient(String chargeBoxId, String path) {
        this.client =
                new OcppJsonChargePoint(JsonObjectMapper.createObjectMapper(), OcppVersion.V_16, chargeBoxId, path);
        this.client.start();
    }

    @Override
    public void bootNotification(String vendor, String model) {
        var req = new BootNotificationRequest();
        req.setChargePointVendor(vendor);
        req.setChargePointModel(model);

        client.prepare(
                req,
                ocpp.cs._2015._10.BootNotificationResponse.class,
                response -> assertThat(response.getStatus()).isEqualTo(ocpp.cs._2015._10.RegistrationStatus.ACCEPTED),
                error -> {
                    throw new RuntimeException(error.toString());
                });
        client.processAndClose();
    }

    @Override
    public void startTransaction(int connectorId, String idTag, int meterStart, OffsetDateTime timestamp) {
        var req = new StartTransactionRequest();
        req.setConnectorId(connectorId);
        req.setIdTag(idTag);
        req.setTimestamp(timestamp);
        req.setMeterStart(meterStart);

        client.prepare(
                req,
                ocpp.cs._2015._10.StartTransactionResponse.class,
                response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getIdTagInfo().getStatus()).isEqualTo(AuthorizationStatus.ACCEPTED);
                    assertThat(response.getTransactionId()).isGreaterThan(0);
                },
                error -> {
                    throw new RuntimeException(error.toString());
                });
        client.processAndClose();
    }

    @Override
    public void stopTransaction(int transactionId, int meterStop, OffsetDateTime timestamp) {
        var req = new StopTransactionRequest();
        req.setTransactionId(transactionId);
        req.setTimestamp(timestamp);
        req.setMeterStop(meterStop);

        client.prepare(
                req,
                ocpp.cs._2015._10.StopTransactionResponse.class,
                response -> assertThat(response).isNotNull(),
                error -> {
                    throw new RuntimeException(error.toString());
                });
        client.processAndClose();
    }

    @Override
    public void meterValues(int connectorId, int transactionId, OffsetDateTime timestamp, String value) {
        var sampledValue = new ocpp.cs._2015._10.SampledValue();
        sampledValue.setValue(value);

        var meterValue = new MeterValue();
        meterValue.setTimestamp(timestamp);
        meterValue.getSampledValue().add(sampledValue);

        var meterValuesRequest = new MeterValuesRequest();
        meterValuesRequest.setConnectorId(connectorId);
        meterValuesRequest.setTransactionId(transactionId);
        meterValuesRequest.getMeterValue().add(meterValue);

        client.prepare(
                meterValuesRequest,
                ocpp.cs._2015._10.MeterValuesResponse.class,
                response -> assertThat(response).isNotNull(),
                error -> {
                    throw new RuntimeException(error.toString());
                });
        client.processAndClose();
    }

    @Override
    public void heartbeat() {
        client.prepare(
                new HeartbeatRequest(),
                ocpp.cs._2015._10.HeartbeatResponse.class,
                response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getCurrentTime())
                            .isCloseTo(OffsetDateTime.now(), byLessThan(1, ChronoUnit.SECONDS));
                },
                error -> {
                    throw new RuntimeException(error.toString());
                });
        client.processAndClose();
    }

    @Override
    public void statusNotification(int connectorId, String status, String errorCode, OffsetDateTime timestamp) {
        var statusNotificationRequest = new StatusNotificationRequest();
        statusNotificationRequest.setConnectorId(connectorId);
        statusNotificationRequest.setStatus(ChargePointStatus.fromValue(status));
        statusNotificationRequest.setErrorCode(ChargePointErrorCode.fromValue(errorCode));
        statusNotificationRequest.setTimestamp(timestamp);

        client.prepare(
                statusNotificationRequest,
                ocpp.cs._2015._10.StatusNotificationResponse.class,
                response -> assertThat(response).isNotNull(),
                error -> {
                    throw new RuntimeException(error.toString());
                });
        client.processAndClose();
    }
}
