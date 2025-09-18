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

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("steve")
public class SteveProperties {

    private Auth auth = new Auth();
    private WebApi webapi = new WebApi();
    private Ocpp ocpp = new Ocpp();
    private String version;
    private String gitDescribe;

    @Data
    public static class Auth {
        private String user;
        private String password;
    }

    @Data
    public static class WebApi {
        private String key;
        private String value;
    }

    @Data
    public static class Ocpp {
        private String wsSessionSelectStrategy;
        private boolean autoRegisterUnknownStations;
        private String chargeBoxIdValidationRegex;
        private Soap soap = new Soap();
    }

    @Data
    public static class Soap {
        private String routerEndpointPath;
    }
}
