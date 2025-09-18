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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.rwth.idsg.steve.service.DummyReleaseCheckService;
import de.rwth.idsg.steve.service.GithubReleaseCheckService;
import de.rwth.idsg.steve.service.ReleaseCheckService;
import de.rwth.idsg.steve.utils.InternetChecker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration and beans of Spring Framework.
 *
 * @author Sevket Goekay <sevketgokay@gmail.com>
 * @since 15.08.2014
 */
@Slf4j
@Configuration
@EnableScheduling
@ComponentScan("de.rwth.idsg.steve")
public class BeanConfiguration {

    /**
     * There might be instances deployed in a local/closed network with no internet connection. In such situations,
     * it is unnecessary to try to access Github every time, even though the request will time out and result
     * report will be correct (that there is no new version). With DummyReleaseCheckService we bypass the intermediate
     * steps and return a "no new version" report immediately.
     */
    @Bean
    public ReleaseCheckService releaseCheckService(SteveProperties steveProperties) {
        String compositeVersion = steveProperties.getVersion();
        if (steveProperties.getGitDescribe() != null) {
            compositeVersion += "-g" + steveProperties.getGitDescribe();
        }

        if (InternetChecker.isInternetAvailable(compositeVersion)) {
            return new GithubReleaseCheckService(steveProperties);
        } else {
            return new DummyReleaseCheckService();
        }
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return objectMapper;
    }
}
