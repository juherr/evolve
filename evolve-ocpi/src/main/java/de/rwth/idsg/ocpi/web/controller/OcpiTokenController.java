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
package de.rwth.idsg.ocpi.web.controller;

import de.rwth.idsg.ocpi.model.Token;
import de.rwth.idsg.ocpi.service.OcpiTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/ocpi/receiver/2.2.1/tokens")
@RequiredArgsConstructor
public class OcpiTokenController {

    private final OcpiTokenService ocpiTokenService;

    @PutMapping("/{countryCode}/{partyId}/{tokenUid}")
    public ResponseEntity<Void> receiveToken(
            @PathVariable String countryCode,
            @PathVariable String partyId,
            @PathVariable String tokenUid,
            @Valid @RequestBody Token token) {

        log.info("Received OCPI token PUT request for token UID: {}", tokenUid);

        // From OCPI 2.2.1 spec: "country_code, party_id and token_uid SHALL be
        // the same as in the body."
        if (!tokenUid.equals(token.getUid())
                || !countryCode.equals(token.getCountryCode())
                || !partyId.equals(token.getPartyId())) {
            log.warn(
                    "Path parameters do not match token body content. Path UID: {}, Body UID: {}",
                    tokenUid,
                    token.getUid());
            return ResponseEntity.badRequest().build();
        }

        ocpiTokenService.saveToken(token);

        return ResponseEntity.ok().build();
    }
}
