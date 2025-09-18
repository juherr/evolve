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
import lombok.RequiredArgsConstructor;

/**
 * From OCPI 2.2.1, section 9.3.6.
 */
@RequiredArgsConstructor
public enum WhitelistType {

    /**
     * Token always has to be whitelisted, realtime authorization is not possible/allowed.
     * CPO shall always allow any use of this Token.
     */
    @JsonProperty("ALWAYS")
    ALWAYS,

    /**
     * It is allowed to whitelist the token, realtime authorization is also allowed.
     * The CPO may choose which version of authorization to use.
     */
    @JsonProperty("ALLOWED")
    ALLOWED,

    /**
     * In normal situations realtime authorization shall be used. But when the CPO cannot
     * get a response from the eMSP (communication between CPO and eMSP is offline),
     * the CPO shall allow this Token to be used.
     */
    @JsonProperty("ALLOWED_OFFLINE")
    ALLOWED_OFFLINE,

    /**
     * Whitelisting is forbidden, only realtime authorization is allowed. CPO shall always
     * send a realtime authorization for any use of this Token to the eMSP.
     */
    @JsonProperty("NEVER")
    NEVER;
}
