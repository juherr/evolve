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
package de.rwth.idsg.steve.service;

import de.rwth.idsg.steve.SteveException;
import de.rwth.idsg.steve.repository.GenericRepository;
import de.rwth.idsg.steve.utils.DateTimeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GenericService {

    private final GenericRepository genericRepository;

    @EventListener
    public void afterStart(ContextRefreshedEvent event) {
        checkJavaAndMySQLOffsets();
    }

    public void checkJavaAndMySQLOffsets() {
        long java = DateTimeUtils.getOffsetFromUtcInSeconds();
        long sql = genericRepository.getSystemTimeDifference();

        if (sql != java) {
            throw new SteveException("MySQL and Java are not using the same time zone. " +
                    "Java offset in seconds (%s) != MySQL offset in seconds (%s)", java, sql);
        }
    }
}
