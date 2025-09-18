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
import ocpp.cs._2010._08.BootNotificationRequest;
import ocpp.cs._2010._08.CentralSystemService;
import ocpp.cs._2010._08.ChargePointErrorCode;
import ocpp.cs._2010._08.ChargePointStatus;
import ocpp.cs._2010._08.HeartbeatRequest;
import ocpp.cs._2010._08.MeterValue;
import ocpp.cs._2010._08.MeterValuesRequest;
import ocpp.cs._2010._08.RegistrationStatus;
import ocpp.cs._2010._08.StatusNotificationRequest;
import ocpp.cs._2010._08.StopTransactionRequest;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class Ocpp12SoapClient implements OcppTestClient {

    private final CentralSystemService client;
    private final String chargeBoxId;

    public Ocpp12SoapClient(String chargeBoxId, String path) {
        this.chargeBoxId = chargeBoxId;
        this.client = Helpers.getForOcpp12(path);
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
        // OCPP 1.2 does not support StartTransaction
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
        var meterValue = new MeterValue();
        meterValue.setTimestamp(timestamp);
        meterValue.setValue(Integer.parseInt(value));

        var meterValuesRequest = new MeterValuesRequest();
        meterValuesRequest.setConnectorId(connectorId);
        // OCPP 1.2 does not support transactionId in MeterValues
        // meterValuesRequest.setTransactionId(transactionId);
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
        // OCPP 1.2 does not support timestamp in StatusNotification
        // statusNotificationRequest.setTimestamp(timestamp);

        var statusNotificationResponse = client.statusNotification(statusNotificationRequest, chargeBoxId);
        assertThat(statusNotificationResponse).isNotNull();
    }
}
