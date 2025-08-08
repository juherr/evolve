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
package de.rwth.idsg.steve.repository.impl;

import de.rwth.idsg.steve.repository.GenericRepository;
import de.rwth.idsg.steve.repository.GenericRepositoryV1;
import de.rwth.idsg.steve.repository.dto.DbVersion;
import de.rwth.idsg.steve.service.GenericService;
import de.rwth.idsg.steve.web.dto.Statistics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class GenericRepositoryV1Impl implements GenericRepositoryV1 {

    private final GenericRepository genericRepository;
    private final GenericService genericService;

    @Override
    public void checkJavaAndMySQLOffsets() {
        genericService.checkJavaAndMySQLOffsets();
    }

    @Override
    public Statistics getStats() {
        return genericRepository.getStats();
    }

    @Override
    public DbVersion getDBVersion() {
        return genericRepository.getDBVersion();
    }
}
