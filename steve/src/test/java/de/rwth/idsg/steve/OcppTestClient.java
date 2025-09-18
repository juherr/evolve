package de.rwth.idsg.steve;

import java.time.OffsetDateTime;

public interface OcppTestClient {
    void bootNotification(String vendor, String model);
    void startTransaction(int connectorId, String idTag, int meterStart, OffsetDateTime timestamp);
    void stopTransaction(int transactionId, int meterStop, OffsetDateTime timestamp);
    void meterValues(int connectorId, int transactionId, OffsetDateTime timestamp, String value);
    void heartbeat();
    void statusNotification(int connectorId, String status, String errorCode, OffsetDateTime timestamp);
}
