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
import ocpp.cs._2010._08.SampledValue;
import ocpp.cs._2010._08.StatusNotificationRequest;
import ocpp.cs._2010._08.StopTransactionRequest;

import java.time.OffsetDateTime;
import java.util.Collections;

import static de.rwth.idsg.steve.utils.Helpers.getRandomString;
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
        var boot = client.bootNotification(
                new BootNotificationRequest()
                        .withChargePointVendor(vendor)
                        .withChargePointModel(model),
                chargeBoxId);
        assertThat(boot).isNotNull();
        assertThat(boot.getStatus()).isEqualTo(RegistrationStatus.ACCEPTED);
    }

    @Override
    public void startTransaction(int connectorId, String idTag, int meterStart, OffsetDateTime timestamp) {
        // OCPP 1.2 does not support StartTransaction
    }

    @Override
    public void stopTransaction(int transactionId, int meterStop, OffsetDateTime timestamp) {
        var stop = client.stopTransaction(
                new StopTransactionRequest()
                        .withTransactionId(transactionId)
                        .withTimestamp(timestamp)
                        .withMeterStop(meterStop),
                chargeBoxId);
        assertThat(stop).isNotNull();
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
        var statusNotificationRequest = new StatusNotificationRequest()
                .withConnectorId(connectorId)
                .withStatus(ChargePointStatus.fromValue(status))
                .withErrorCode(ChargePointErrorCode.fromValue(errorCode))
                .withTimestamp(timestamp);
        var statusNotificationResponse = client.statusNotification(statusNotificationRequest, chargeBoxId);
        assertThat(statusNotificationResponse).isNotNull();
    }
}
