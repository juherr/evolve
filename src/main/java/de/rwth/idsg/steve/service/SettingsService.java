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

import de.rwth.idsg.steve.NotificationFeature;
import de.rwth.idsg.steve.repository.SettingsRepository;
import de.rwth.idsg.steve.repository.dto.MailSettings;
import de.rwth.idsg.steve.web.dto.SettingsForm;
import jooq.steve.db.tables.records.SettingsRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static de.rwth.idsg.steve.utils.StringUtils.splitByComma;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final SettingsRepository settingsRepository;

    public SettingsForm getForm() {
        SettingsRecord r = settingsRepository.getSettings();

        List<String> eMails = splitByComma(r.getMailRecipients());
        List<NotificationFeature> features = splitFeatures(r.getNotificationFeatures());

        return SettingsForm.builder()
                .heartbeat(toMin(r.getHeartbeatIntervalInSeconds()))
                .expiration(r.getHoursToExpire())
                .enabled(r.getMailEnabled())
                .mailHost(r.getMailHost())
                .username(r.getMailUsername())
                .password(r.getMailPassword())
                .from(r.getMailFrom())
                .protocol(r.getMailProtocol())
                .port(r.getMailPort())
                .recipients(eMails)
                .enabledFeatures(features)
                .build();

    }

    public MailSettings getMailSettings() {
        SettingsRecord r = settingsRepository.getSettings();

        List<String> eMails = splitByComma(r.getMailRecipients());
        List<NotificationFeature> features = splitFeatures(r.getNotificationFeatures());

        return MailSettings.builder()
                .enabled(r.getMailEnabled())
                .mailHost(r.getMailHost())
                .username(r.getMailUsername())
                .password(r.getMailPassword())
                .from(r.getMailFrom())
                .protocol(r.getMailProtocol())
                .port(r.getMailPort())
                .recipients(eMails)
                .enabledFeatures(features)
                .build();
    }

    public int getHeartbeatIntervalInSeconds() {
        return settingsRepository.getSettings().getHeartbeatIntervalInSeconds();
    }

    public int getHoursToExpire() {
        return settingsRepository.getSettings().getHoursToExpire();
    }

    private List<NotificationFeature> splitFeatures(String str) {
        return splitByComma(str).stream()
                .map(NotificationFeature::fromName)
                .collect(Collectors.toList());
    }

    private static int toMin(int seconds) {
        return (int) TimeUnit.SECONDS.toMinutes(seconds);
    }
}
