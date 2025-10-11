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
package de.rwth.idsg.steve.ocpp.ws;

import com.google.common.base.Strings;
import de.rwth.idsg.steve.service.ChargePointRegistrationService;
import de.rwth.idsg.steve.web.validation.ChargeBoxIdValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

/**
 * @author Sevket Goekay <sevketgokay@gmail.com>
 * @since 05.03.2022
 */
@Slf4j
@RequiredArgsConstructor
public class OcppWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final ChargeBoxIdValidator chargeBoxIdValidator;
    private final List<OcppWebSocketHandler> endpoints;
    private final ChargePointRegistrationService chargePointRegistrationService;
    private final String pathInfix;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes)
            throws Exception {
        // -------------------------------------------------------------------------
        // 1. Check the chargeBoxId
        // -------------------------------------------------------------------------

        var chargeBoxId = getLastBitFromUrl(pathInfix, request.getURI().getRawPath());
        var isValid = chargeBoxIdValidator.isValid(chargeBoxId);
        if (!isValid) {
            log.error("ChargeBoxId '{}' violates the configured pattern.", chargeBoxId);
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }

        var status = chargePointRegistrationService.getRegistrationStatus(chargeBoxId);

        // Allow connections, if station is in db (registration_status field from db does not matter)
        var allowConnection = status.isPresent();

        // https://github.com/steve-community/steve/issues/1020
        if (!allowConnection) {
            log.error("ChargeBoxId '{}' is not recognized.", chargeBoxId);
            response.setStatusCode(HttpStatus.NOT_FOUND);
            return false;
        }

        attributes.put(AbstractWebSocketEndpoint.CHARGEBOX_ID_KEY, chargeBoxId);

        // -------------------------------------------------------------------------
        // 2. Route according to the selected protocol
        // -------------------------------------------------------------------------

        var requestedProtocols = new WebSocketHttpHeaders(request.getHeaders()).getSecWebSocketProtocol();

        if (CollectionUtils.isEmpty(requestedProtocols)) {
            log.error("No protocol (OCPP version) is specified.");
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }

        var endpoint = selectEndpoint(requestedProtocols);

        if (endpoint == null) {
            log.error("None of the requested protocols '{}' is supported", requestedProtocols);
            response.setStatusCode(HttpStatus.NOT_FOUND);
            return false;
        }

        log.debug(
                "ChargeBoxId '{}' will be using {}",
                chargeBoxId,
                endpoint.getClass().getSimpleName());
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            @Nullable Exception exception) {
        // Nothing to do here
    }

    private @Nullable OcppWebSocketHandler selectEndpoint(List<String> requestedProtocols) {
        for (var requestedProtocol : requestedProtocols) {
            for (var item : endpoints) {
                if (item.getVersion().getValue().equals(requestedProtocol)) {
                    return item;
                }
            }
        }
        return null;
    }

    public static String getLastBitFromUrl(String pathInfix, String input) {
        if (Strings.isNullOrEmpty(input)) {
            return "";
        }

        var index = input.indexOf(pathInfix);
        if (index == -1) {
            return "";
        }
        return input.substring(index + pathInfix.length());
    }
}
