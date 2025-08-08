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
package de.rwth.idsg.steve.repository.impl;

import de.rwth.idsg.steve.repository.ReservationRepository;
import de.rwth.idsg.steve.repository.ReservationStatus;
import de.rwth.idsg.steve.repository.dto.InsertReservationParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static jooq.steve.db.tables.Connector.CONNECTOR;
import static jooq.steve.db.tables.Reservation.RESERVATION;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ReservationRepositoryImpl implements ReservationRepository {

    private final DSLContext ctx;

    @Override
    public List<Integer> getActiveReservationIds(String chargeBoxId) {
        return ctx.select(RESERVATION.RESERVATION_PK)
                .from(RESERVATION)
                .where(RESERVATION.CONNECTOR_PK.in(DSL.select(CONNECTOR.CONNECTOR_PK)
                        .from(CONNECTOR)
                        .where(CONNECTOR.CHARGE_BOX_ID.equal(chargeBoxId))))
                .and(RESERVATION.EXPIRY_DATETIME.greaterThan(LocalDateTime.now()))
                .and(RESERVATION.STATUS.equal(ReservationStatus.ACCEPTED.name()))
                .fetch(RESERVATION.RESERVATION_PK);
    }

    @Override
    public int insert(InsertReservationParams params) {
        SelectConditionStep<Record1<Integer>> connectorPkQuery =
                DSL.select(CONNECTOR.CONNECTOR_PK)
                        .from(CONNECTOR)
                        .where(CONNECTOR.CHARGE_BOX_ID.equal(params.getChargeBoxId()))
                        .and(CONNECTOR.CONNECTOR_ID.equal(params.getConnectorId()));

        int reservationId = ctx.insertInto(RESERVATION)
                .set(RESERVATION.CONNECTOR_PK, connectorPkQuery)
                .set(RESERVATION.ID_TAG, params.getIdTag())
                .set(RESERVATION.START_DATETIME, params.getStartTimestamp())
                .set(RESERVATION.EXPIRY_DATETIME, params.getExpiryTimestamp())
                .set(RESERVATION.STATUS, ReservationStatus.WAITING.name())
                .returning(RESERVATION.RESERVATION_PK)
                .fetchOne()
                .getReservationPk();

        log.debug("A new reservation '{}' is inserted.", reservationId);
        return reservationId;
    }

    @Override
    public void delete(int reservationId) {
        ctx.delete(RESERVATION)
                .where(RESERVATION.RESERVATION_PK.equal(reservationId))
                .execute();

        log.debug("The reservation '{}' is deleted.", reservationId);
    }

    @Override
    public void updateReservationStatus(int reservationId, ReservationStatus status) {
        ctx.update(RESERVATION)
                .set(RESERVATION.STATUS, status.name())
                .where(RESERVATION.RESERVATION_PK.equal(reservationId))
                .execute();
    }

    @Override
    public void used(String chargeBoxId, int connectorId, String ocppIdTag, int reservationId, int transactionId) {
        SelectConditionStep<Record1<Integer>> connectorPkSelect =
                DSL.select(CONNECTOR.CONNECTOR_PK)
                        .from(CONNECTOR)
                        .where(CONNECTOR.CHARGE_BOX_ID.equal(chargeBoxId))
                        .and(CONNECTOR.CONNECTOR_ID.equal(connectorId));

        int count = ctx.update(RESERVATION)
                .set(RESERVATION.STATUS, ReservationStatus.USED.name())
                .set(RESERVATION.TRANSACTION_PK, transactionId)
                .where(RESERVATION.RESERVATION_PK.equal(reservationId))
                .and(RESERVATION.ID_TAG.equal(ocppIdTag))
                .and(RESERVATION.CONNECTOR_PK.equal(connectorPkSelect))
                .and(RESERVATION.STATUS.eq(ReservationStatus.ACCEPTED.name()))
                .execute();

        if (count != 1) {
            log.warn("Could not mark the reservation '{}' as used: Problems occurred due to sent reservation id, " +
                    "charge box connector, user id tag or the reservation was used already.", reservationId);
        }
    }
}
