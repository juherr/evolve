package de.rwth.idsg.steve.web.api;

import de.rwth.idsg.steve.repository.dto.Reservation;
import de.rwth.idsg.steve.service.ReservationService;
import de.rwth.idsg.steve.web.dto.ReservationForm;
import de.rwth.idsg.steve.web.dto.ReservationQueryForm;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(path = "/api/reservations", produces = "application/json;charset=UTF-8")
@RequiredArgsConstructor
public class ReservationRestController {

    private final ReservationService reservationService;

    @GetMapping
    public ResponseEntity<List<Reservation>> getReservations(ReservationQueryForm form) {
        List<Reservation> reservations = reservationService.getReservations(form);
        return ResponseEntity.ok(reservations);
    }

    @PostMapping
    public ResponseEntity<Integer> addReservation(@Valid @RequestBody ReservationForm form) {
        int reservationId = reservationService.addReservation(form);
        return ResponseEntity.ok(reservationId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable int id) {
        reservationService.deleteReservation(id);
        return ResponseEntity.ok().build();
    }
}
