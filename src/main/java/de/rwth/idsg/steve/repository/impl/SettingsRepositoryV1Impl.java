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

import de.rwth.idsg.steve.repository.SettingsRepository;
import de.rwth.idsg.steve.repository.SettingsRepositoryV1;
import de.rwth.idsg.steve.repository.dto.MailSettings;
import de.rwth.idsg.steve.service.SettingsService;
import de.rwth.idsg.steve.web.dto.SettingsForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SettingsRepositoryV1Impl implements SettingsRepositoryV1 {

    private final SettingsRepository settingsRepository;
    private final SettingsService settingsService;

    @Override
    public SettingsForm getForm() {
        return settingsService.getForm();
    }

    @Override
    public MailSettings getMailSettings() {
        return settingsService.getMailSettings();
    }

    @Override
    public int getHeartbeatIntervalInSeconds() {
        return settingsService.getHeartbeatIntervalInSeconds();
    }

    @Override
    public int getHoursToExpire() {
        return settingsService.getHoursToExpire();
    }

    @Override
    public void update(SettingsForm form) {
        settingsRepository.update(form);
    }
}
