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
package de.rwth.idsg.ocpi.service;

import de.rwth.idsg.ocpi.client.OcpiClient;
import de.rwth.idsg.ocpi.model.Token;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static jooq.steve.db.tables.OcppTag.OCPP_TAG;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcpiTokenService {

    private final DSLContext dsl;
    private final OcpiClient ocpiClient;

    @Transactional
    public void pullTokens(String url, String token) {
        List<Token> tokens = ocpiClient.getTokens(url, token);
        for (Token t : tokens) {
            saveToken(t);
        }
    }

    @Transactional
    public void saveToken(Token token) {
        log.debug("Saving OCPI token: {}", token);

        String note = "OCPI Token from issuer: " + token.getIssuer();
        LocalDateTime lastUpdated = LocalDateTime.ofInstant(token.getLastUpdated(), ZoneOffset.UTC);
        int maxActiveCount = token.getValid() ? 1 : 0;

        dsl.insertInto(OCPP_TAG)
                .set(OCPP_TAG.ID_TAG, token.getUid())
                .set(OCPP_TAG.CONTRACT_ID, token.getContractId())
                .set(OCPP_TAG.ISSUER, token.getIssuer())
                .set(OCPP_TAG.LANGUAGE, token.getLanguage())
                .set(OCPP_TAG.VISUAL_NUMBER, token.getVisualNumber())
                .set(OCPP_TAG.PARENT_ID_TAG, token.getGroupId())
                .set(OCPP_TAG.EXPIRY_DATE, (LocalDateTime) null)
                .set(OCPP_TAG.MAX_ACTIVE_TRANSACTION_COUNT, maxActiveCount)
                .set(OCPP_TAG.CREATION_ORIGIN, "OCPI")
                .set(OCPP_TAG.TYPE, token.getType().name())
                .set(OCPP_TAG.LAST_UPDATED, lastUpdated)
                .set(OCPP_TAG.NOTE, note)
                .onDuplicateKeyUpdate()
                .set(OCPP_TAG.CONTRACT_ID, token.getContractId())
                .set(OCPP_TAG.ISSUER, token.getIssuer())
                .set(OCPP_TAG.LANGUAGE, token.getLanguage())
                .set(OCPP_TAG.VISUAL_NUMBER, token.getVisualNumber())
                .set(OCPP_TAG.PARENT_ID_TAG, token.getGroupId())
                .set(OCPP_TAG.MAX_ACTIVE_TRANSACTION_COUNT, maxActiveCount)
                .set(OCPP_TAG.TYPE, token.getType().name())
                .set(OCPP_TAG.LAST_UPDATED, lastUpdated)
                .set(OCPP_TAG.NOTE, note)
                .execute();

        log.info("Successfully saved OCPI token with UID: {}", token.getUid());
    }
}
