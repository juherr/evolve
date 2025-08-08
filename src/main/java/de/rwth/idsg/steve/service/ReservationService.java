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
package de.rwth.idsg.steve.service;

import de.rwth.idsg.steve.SteveException;
import de.rwth.idsg.steve.repository.ReservationRepository;
import de.rwth.idsg.steve.repository.ReservationStatus;
import de.rwth.idsg.steve.repository.dto.Reservation;
import de.rwth.idsg.steve.utils.DateTimeUtils;
import de.rwth.idsg.steve.web.dto.ReservationQueryForm;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record10;
import org.jooq.RecordMapper;
import org.jooq.SelectQuery;
import org.jooq.exception.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static jooq.steve.db.tables.ChargeBox.CHARGE_BOX;
import static jooq.steve.db.tables.Connector.CONNECTOR;
import static jooq.steve.db.tables.OcppTag.OCPP_TAG;
import static jooq.steve.db.tables.Reservation.RESERVATION;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final DSLContext ctx;
    private final ReservationRepository reservationRepository;

    public List<Reservation> getReservations(ReservationQueryForm form) {
        SelectQuery selectQuery = ctx.selectQuery();
        selectQuery.addFrom(RESERVATION);
        selectQuery.addJoin(OCPP_TAG, OCPP_TAG.ID_TAG.eq(RESERVATION.ID_TAG));
        selectQuery.addJoin(CONNECTOR, CONNECTOR.CONNECTOR_PK.eq(RESERVATION.CONNECTOR_PK));
        selectQuery.addJoin(CHARGE_BOX, CONNECTOR.CHARGE_BOX_ID.eq(CHARGE_BOX.CHARGE_BOX_ID));

        selectQuery.addSelect(
                RESERVATION.RESERVATION_PK,
                RESERVATION.TRANSACTION_PK,
                OCPP_TAG.OCPP_TAG_PK,
                CHARGE_BOX.CHARGE_BOX_PK,
                OCPP_TAG.ID_TAG,
                CHARGE_BOX.CHARGE_BOX_ID,
                RESERVATION.START_DATETIME,
                RESERVATION.EXPIRY_DATETIME,
                RESERVATION.STATUS,
                CONNECTOR.CONNECTOR_ID
        );

        if (form.isChargeBoxIdSet()) {
            selectQuery.addConditions(CHARGE_BOX.CHARGE_BOX_ID.eq(form.getChargeBoxId()));
        }

        if (form.isOcppIdTagSet()) {
            selectQuery.addConditions(RESERVATION.ID_TAG.eq(form.getOcppIdTag()));
        }

        if (form.isStatusSet()) {
            selectQuery.addConditions(RESERVATION.STATUS.eq(form.getStatus().name()));
        }

        processType(selectQuery, form);

        // Default order
        selectQuery.addOrderBy(RESERVATION.EXPIRY_DATETIME.asc());

        return selectQuery.fetch().map(new ReservationMapper());
    }

    public void accepted(int reservationId) {
        internalUpdateReservation(reservationId, ReservationStatus.ACCEPTED);
    }

    public void cancelled(int reservationId) {
        internalUpdateReservation(reservationId, ReservationStatus.CANCELLED);
    }

    private void internalUpdateReservation(int reservationId, ReservationStatus status) {
        try {
            reservationRepository.updateReservationStatus(reservationId, status);
        } catch (DataAccessException e) {
            throw new SteveException("Updating of reservationId '%s' to status '%s' FAILED.", reservationId, status, e);
        }
    }

    private void processType(SelectQuery selectQuery, ReservationQueryForm form) {
        switch (form.getPeriodType()) {
            case ACTIVE:
                selectQuery.addConditions(RESERVATION.EXPIRY_DATETIME.greaterThan(LocalDateTime.now()));
                break;

            case FROM_TO:
                selectQuery.addConditions(
                        RESERVATION.START_DATETIME.greaterOrEqual(form.getFrom()),
                        RESERVATION.EXPIRY_DATETIME.lessOrEqual(form.getTo())
                );
                break;

            default:
                throw new SteveException("Unknown enum type");
        }
    }

    private static class ReservationMapper implements
            RecordMapper<Record10<Integer, Integer, Integer, Integer, String,
                    String, LocalDateTime, LocalDateTime, String, Integer>, Reservation> {
        @Override
        public Reservation map(Record10<Integer, Integer, Integer, Integer, String,
                String, LocalDateTime, LocalDateTime, String, Integer> r) {
            return Reservation.builder()
                    .id(r.value1())
                    .transactionId(r.value2())
                    .ocppTagPk(r.value3())
                    .chargeBoxPk(r.value4())
                    .ocppIdTag(r.value5())
                    .chargeBoxId(r.value6())
                    .startDatetimeDT(r.value7())
                    .startDatetime(DateTimeUtils.humanize(r.value7()))
                    .expiryDatetimeDT(r.value8())
                    .expiryDatetime(DateTimeUtils.humanize(r.value8()))
                    .status(r.value9())
                    .connectorId(r.value10())
                    .build();
        }
    }
}
