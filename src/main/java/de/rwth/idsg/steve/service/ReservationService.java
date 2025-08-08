package de.rwth.idsg.steve.service;

import de.rwth.idsg.steve.repository.ReservationRepository;
import de.rwth.idsg.steve.repository.dto.InsertReservationParams;
import de.rwth.idsg.steve.repository.dto.Reservation;
import de.rwth.idsg.steve.web.dto.ReservationForm;
import de.rwth.idsg.steve.web.dto.ReservationQueryForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;

    public List<Reservation> getReservations(ReservationQueryForm form) {
        return reservationRepository.getReservations(form);
    }

    public int addReservation(ReservationForm form) {
        InsertReservationParams params = InsertReservationParams.builder()
                .idTag(form.getIdTag())
                .chargeBoxId(form.getChargeBoxId())
                .connectorId(form.getConnectorId())
                .startTimestamp(form.getStartTimestamp())
                .expiryTimestamp(form.getExpiryTimestamp())
                .build();
        return reservationRepository.insert(params);
    }

    public void deleteReservation(int reservationId) {
        reservationRepository.delete(reservationId);
    }
}
