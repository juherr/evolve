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
import de.rwth.idsg.steve.repository.ReservationRepositoryV1;
import de.rwth.idsg.steve.repository.dto.InsertReservationParams;
import de.rwth.idsg.steve.repository.dto.Reservation;
import de.rwth.idsg.steve.service.ReservationService;
import de.rwth.idsg.steve.web.dto.ReservationQueryForm;
import lombok.RequiredArgsConstructor;
import org.jooq.Record1;
import org.jooq.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ReservationRepositoryV1Impl implements ReservationRepositoryV1 {

    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;

    @Override
    public List<Reservation> getReservations(ReservationQueryForm form) {
        return reservationService.getReservations(form);
    }

    @Override
    public List<Integer> getActiveReservationIds(String chargeBoxId) {
        return reservationRepository.getActiveReservationIds(chargeBoxId);
    }

    @Override
    public int insert(InsertReservationParams params) {
        return reservationRepository.insert(params);
    }

    @Override
    public void delete(int reservationId) {
        reservationRepository.delete(reservationId);
    }

    @Override
    public void accepted(int reservationId) {
        reservationService.accepted(reservationId);
    }

    @Override
    public void cancelled(int reservationId) {
        reservationService.cancelled(reservationId);
    }

    @Override
    public void used(Select<Record1<Integer>> connectorPkSelect, String ocppIdTag, int reservationId, int transactionId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
