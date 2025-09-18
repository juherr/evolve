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
package de.rwth.idsg.ocpi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * From OCPI 2.2.1, section 9.3.1.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Token {

    @NotEmpty @Size(min = 1, max = 2) @JsonProperty("country_code")
    private String countryCode;

    @NotEmpty @Size(min = 1, max = 3) @JsonProperty("party_id")
    private String partyId;

    @NotEmpty @Size(min = 1, max = 36) @JsonProperty("uid")
    private String uid;

    @NotNull @JsonProperty("type")
    private TokenType type;

    @NotEmpty @Size(min = 1, max = 36) @JsonProperty("contract_id")
    private String contractId;

    @Size(min = 1, max = 64) @JsonProperty("visual_number")
    private String visualNumber;

    @NotEmpty @Size(min = 1, max = 64) @JsonProperty("issuer")
    private String issuer;

    @Size(min = 1, max = 36) @JsonProperty("group_id")
    private String groupId;

    @NotNull @JsonProperty("valid")
    private Boolean valid;

    @NotNull @JsonProperty("whitelist")
    private WhitelistType whitelist;

    @Size(min = 1, max = 2) @JsonProperty("language")
    private String language;

    @JsonProperty("default_profile_type")
    private ProfileType defaultProfileType;

    @JsonProperty("energy_contract")
    private EnergyContract energyContract;

    @NotNull @JsonProperty("last_updated")
    private Instant lastUpdated;
}
