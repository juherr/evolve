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

import java.time.OffsetDateTime;

public interface OcppTestClient {
    void bootNotification(String vendor, String model);

    void startTransaction(int connectorId, String idTag, int meterStart, OffsetDateTime timestamp);

    void stopTransaction(int transactionId, int meterStop, OffsetDateTime timestamp);

    void meterValues(int connectorId, int transactionId, OffsetDateTime timestamp, String value);

    void heartbeat();

    void statusNotification(int connectorId, String status, String errorCode, OffsetDateTime timestamp);
}
