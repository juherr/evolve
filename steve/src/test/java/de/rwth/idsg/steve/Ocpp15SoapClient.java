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
import ocpp.cs._2012._06.BootNotificationRequest;
import ocpp.cs._2012._06.CentralSystemService;
import ocpp.cs._2012._06.ChargePointErrorCode;
import ocpp.cs._2012._06.ChargePointStatus;
import ocpp.cs._2012._06.HeartbeatRequest;
import ocpp.cs._2012._06.MeterValue;
import ocpp.cs._2012._06.MeterValuesRequest;
import ocpp.cs._2012._06.RegistrationStatus;
import ocpp.cs._2012._06.StartTransactionRequest;
import ocpp.cs._2012._06.StatusNotificationRequest;
import ocpp.cs._2012._06.StopTransactionRequest;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class Ocpp15SoapClient implements OcppTestClient {

    private final CentralSystemService client;
    private final String chargeBoxId;

    public Ocpp15SoapClient(String chargeBoxId, String path) {
        this.chargeBoxId = chargeBoxId;
        this.client = Helpers.getForOcpp15(path);
    }

    @Override
    public void bootNotification(String vendor, String model) {
        var req = new BootNotificationRequest();
        req.setChargePointVendor(vendor);
        req.setChargePointModel(model);

        var boot = client.bootNotification(req, chargeBoxId);
        assertThat(boot).isNotNull();
        assertThat(boot.getStatus()).isEqualTo(RegistrationStatus.ACCEPTED);
    }

    @Override
    public void startTransaction(int connectorId, String idTag, int meterStart, OffsetDateTime timestamp) {
        var req = new StartTransactionRequest();
        req.setConnectorId(connectorId);
        req.setIdTag(idTag);
        req.setTimestamp(timestamp);
        req.setMeterStart(meterStart);

        var start = client.startTransaction(req, chargeBoxId);
        assertThat(start).isNotNull();
        assertThat(start.getIdTagInfo().getStatus()).isEqualTo(ocpp.cs._2012._06.AuthorizationStatus.ACCEPTED);
        assertThat(start.getTransactionId()).isGreaterThan(0);
    }

    @Override
    public void stopTransaction(int transactionId, int meterStop, OffsetDateTime timestamp) {
        var req = new StopTransactionRequest();
        req.setTransactionId(transactionId);
        req.setTimestamp(timestamp);
        req.setMeterStop(meterStop);

        var stop = client.stopTransaction(req, chargeBoxId);
        assertThat(stop).isNotNull();
    }

    @Override
    public void meterValues(int connectorId, int transactionId, OffsetDateTime timestamp, String value) {
        var sampledValue = new MeterValue.Value();
        sampledValue.setValue(value);

        var meterValue = new MeterValue();
        meterValue.setTimestamp(timestamp);
        meterValue.getValue().add(sampledValue);

        var meterValuesRequest = new MeterValuesRequest();
        meterValuesRequest.setConnectorId(connectorId);
        meterValuesRequest.setTransactionId(transactionId);
        meterValuesRequest.getValues().add(meterValue);

        var meterValuesResponse = client.meterValues(meterValuesRequest, chargeBoxId);
        assertThat(meterValuesResponse).isNotNull();
    }

    @Override
    public void heartbeat() {
        var heartbeat = client.heartbeat(new HeartbeatRequest(), chargeBoxId);
        assertThat(heartbeat).isNotNull();
    }

    @Override
    public void statusNotification(int connectorId, String status, String errorCode, OffsetDateTime timestamp) {
        var statusNotificationRequest = new StatusNotificationRequest();
        statusNotificationRequest.setConnectorId(connectorId);
        statusNotificationRequest.setStatus(ChargePointStatus.fromValue(status));
        statusNotificationRequest.setErrorCode(ChargePointErrorCode.fromValue(errorCode));
        statusNotificationRequest.setTimestamp(timestamp);

        var statusNotificationResponse = client.statusNotification(statusNotificationRequest, chargeBoxId);
        assertThat(statusNotificationResponse).isNotNull();
    }
}
