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
package de.rwth.idsg.steve;

/**
 * @author Sevket Goekay <sevketgokay@gmail.com>
 * @since 05.11.2015
 */
public enum ApplicationProfile {
    DEV,
    DOCKER,
    KUBERNETES,
    PROD,
    TEST;

    public static ApplicationProfile fromName(String v) {
        for (ApplicationProfile ap : ApplicationProfile.values()) {
            if (ap.name().equalsIgnoreCase(v)) {
                return ap;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
