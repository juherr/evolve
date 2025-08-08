package de.rwth.idsg.steve.web.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class ReservationForm {

    private Integer reservationPk;

    @NotEmpty(message = "ID Tag is required")
    private String idTag;

    @NotEmpty(message = "ChargeBox ID is required")
    private String chargeBoxId;

    @NotNull(message = "Connector ID is required")
    private Integer connectorId;

    @NotNull(message = "Start timestamp is required")
    private LocalDateTime startTimestamp;

    @NotNull(message = "Expiry timestamp is required")
    @Future(message = "Expiry timestamp must be in the future")
    private LocalDateTime expiryTimestamp;
}
