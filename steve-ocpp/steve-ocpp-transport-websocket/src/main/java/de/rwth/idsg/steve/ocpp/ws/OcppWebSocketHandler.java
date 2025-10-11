package de.rwth.idsg.steve.ocpp.ws;

import de.rwth.idsg.steve.ocpp.OcppVersion;
import org.springframework.web.socket.WebSocketHandler;

public interface OcppWebSocketHandler extends WebSocketHandler {

    OcppVersion getVersion();
}
