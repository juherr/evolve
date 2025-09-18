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
 * From OCPI 2.2.1, section 11.3.3 (Sessions Module)
 */
@RequiredArgsConstructor
public enum ProfileType {

    /**
     * Driver wants to use the cheapest charging profile possible.
     */
    @JsonProperty("CHEAP")
    CHEAP,

    /**
     * Driver wants his EV charged as fast as possible.
     */
    @JsonProperty("FAST")
    FAST,

    /**
     * Driver wants to use a green charging profile.
     */
    @JsonProperty("GREEN")
    GREEN,

    /**
     * Driver does not have any preference.
     */
    @JsonProperty("REGULAR")
    REGULAR;
}
