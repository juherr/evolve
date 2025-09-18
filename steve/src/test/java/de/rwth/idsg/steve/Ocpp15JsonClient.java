package de.rwth.idsg.steve;

import de.rwth.idsg.steve.ocpp.OcppVersion;
import de.rwth.idsg.steve.ocpp.ws.JsonObjectMapper;
import de.rwth.idsg.steve.utils.OcppJsonChargePoint;
import ocpp.cs._2012._06.BootNotificationRequest;
import ocpp.cs._2012._06.ChargePointErrorCode;
import ocpp.cs._2012._06.ChargePointStatus;
import ocpp.cs._2012._06.HeartbeatRequest;
import ocpp.cs._2012._06.MeterValue;
import ocpp.cs._2012._06.MeterValuesRequest;
import ocpp.cs._2012._06.StartTransactionRequest;
import ocpp.cs._2012._06.StatusNotificationRequest;
import ocpp.cs._2012._06.StopTransactionRequest;
import ocpp.cs._2012._06.SampledValue;
import ocpp.cs._2012._06.AuthorizationStatus;


import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.byLessThan;

public class Ocpp15JsonClient implements OcppTestClient {

    private final OcppJsonChargePoint client;

    public Ocpp15JsonClient(String chargeBoxId, String path) {
        this.client = new OcppJsonChargePoint(new JsonObjectMapper(), OcppVersion.V_15, chargeBoxId, path);
        this.client.start();
    }

    @Override
    public void bootNotification(String vendor, String model) {
        client.prepare(
                new BootNotificationRequest()
                        .withChargePointVendor(vendor)
                        .withChargePointModel(model),
                ocpp.cs._2012._06.BootNotificationResponse.class,
                response -> assertThat(response.getStatus()).isEqualTo(ocpp.cs._2012._06.RegistrationStatus.ACCEPTED),
                error -> {
                    throw new RuntimeException(error.toString());
                });
        client.processAndClose();
    }

    @Override
    public void startTransaction(int connectorId, String idTag, int meterStart, OffsetDateTime timestamp) {
        client.prepare(
                new StartTransactionRequest()
                        .withConnectorId(connectorId)
                        .withIdTag(idTag)
                        .withTimestamp(timestamp)
                        .withMeterStart(meterStart),
                ocpp.cs._2012._06.StartTransactionResponse.class,
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
        client.prepare(
                new StopTransactionRequest()
                        .withTransactionId(transactionId)
                        .withTimestamp(timestamp)
                        .withMeterStop(meterStop),
                ocpp.cs._2012._06.StopTransactionResponse.class,
                response -> assertThat(response).isNotNull(),
                error -> {
                    throw new RuntimeException(error.toString());
                });
        client.processAndClose();
    }

    @Override
    public void meterValues(int connectorId, int transactionId, OffsetDateTime timestamp, String value) {
        var meterValue = new MeterValue()
                .withTimestamp(timestamp)
                .withSampledValue(Collections.singletonList(new SampledValue().withValue(value)));
        var meterValuesRequest = new MeterValuesRequest()
                .withConnectorId(connectorId)
                .withTransactionId(transactionId)
                .withMeterValue(Collections.singletonList(meterValue));
        client.prepare(
                meterValuesRequest,
                ocpp.cs._2012._06.MeterValuesResponse.class,
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
                ocpp.cs._2012._06.HeartbeatResponse.class,
                response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getCurrentTime()).isCloseTo(OffsetDateTime.now(), byLessThan(1, ChronoUnit.SECONDS));
                },
                error -> {
                    throw new RuntimeException(error.toString());
                });
        client.processAndClose();
    }

    @Override
    public void statusNotification(int connectorId, String status, String errorCode, OffsetDateTime timestamp) {
        var statusNotificationRequest = new StatusNotificationRequest()
                .withConnectorId(connectorId)
                .withStatus(ChargePointStatus.fromValue(status))
                .withErrorCode(ChargePointErrorCode.fromValue(errorCode))
                .withTimestamp(timestamp);
        client.prepare(
                statusNotificationRequest,
                ocpp.cs._2012._06.StatusNotificationResponse.class,
                response -> assertThat(response).isNotNull(),
                error -> {
                    throw new RuntimeException(error.toString());
                });
        client.processAndClose();
    }
}
