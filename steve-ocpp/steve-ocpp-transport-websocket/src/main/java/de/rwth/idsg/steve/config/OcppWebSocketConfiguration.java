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
package de.rwth.idsg.steve.config;

import de.rwth.idsg.steve.ocpp.ws.OcppWebSocketHandler;
import de.rwth.idsg.steve.ocpp.ws.OcppWebSocketHandshakeInterceptor;
import de.rwth.idsg.steve.service.ChargePointRegistrationService;
import de.rwth.idsg.steve.web.validation.ChargeBoxIdValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.jetty.JettyRequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.util.List;

/**
 * @author Sevket Goekay <sevketgokay@gmail.com>
 * @since 11.03.2015
 */
@EnableWebSocket
@Configuration
@Slf4j
@RequiredArgsConstructor
public class OcppWebSocketConfiguration implements WebSocketConfigurer {

    private final ChargePointRegistrationService chargePointRegistrationService;
    private final ChargeBoxIdValidator chargeBoxIdValidator;

    private final List<OcppWebSocketHandler> ocppWebSocketHandlers;
    private final SteveProperties steveProperties;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

        var pathInfix = steveProperties.getPaths().getWebsocketMapping()
                + steveProperties.getPaths().getRouterEndpointPath() + "/";

        var handshakeInterceptor = new OcppWebSocketHandshakeInterceptor(
                chargeBoxIdValidator, ocppWebSocketHandlers, chargePointRegistrationService, pathInfix);

        /*
         * We need some WebSocketHandler just for Spring to register it for the path. We will not use it for the actual
         * operations. This instance will be passed to doHandshake(..) below. We will find the proper WebSocketEndpoint
         * based on the selectedProtocol and replace the dummy one with the proper one in the subsequent call chain.
         */
        registry.addHandler(dummyWebSocketHandler(), pathInfix + "*")
                .setHandshakeHandler(handshakeHandler())
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins(steveProperties.getOcpp().getWs().getAllowedOriginPatterns());
    }

    @Bean
    public WebSocketHandler dummyWebSocketHandler() {
        return new TextWebSocketHandler();
    }

    /**
     * See Spring docs: https://docs.spring.io/spring-framework/reference/web/websocket/server.html
     */
    @Bean
    public HandshakeHandler handshakeHandler() {
        var strategy = new JettyRequestUpgradeStrategy();

        strategy.addWebSocketConfigurer(configurable -> {
            configurable.setMaxTextMessageSize(steveProperties.getOcpp().getWs().getMaxTextMessageSize());
            configurable.setIdleTimeout(steveProperties.getOcpp().getWs().getIdleTimeout());
        });

        return new DefaultHandshakeHandler(strategy);
    }
}
