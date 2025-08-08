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

import de.rwth.idsg.steve.SteveException;
import de.rwth.idsg.steve.repository.SettingsRepository;
import de.rwth.idsg.steve.web.dto.SettingsForm;
import jooq.steve.db.tables.records.SettingsRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static de.rwth.idsg.steve.utils.StringUtils.joinByComma;
import static jooq.steve.db.tables.Settings.SETTINGS;

@Repository
@RequiredArgsConstructor
public class SettingsRepositoryImpl implements SettingsRepository {

    private static final String APP_ID = new String(
            Base64.getEncoder().encode("SteckdosenVerwaltung".getBytes(StandardCharsets.UTF_8)),
            StandardCharsets.UTF_8
    );

    private final DSLContext ctx;

    @Override
    public SettingsRecord getSettings() {
        return ctx.selectFrom(SETTINGS)
                .where(SETTINGS.APP_ID.eq(APP_ID))
                .fetchOne();
    }

    @Override
    public void update(SettingsForm form) {
        String eMails = joinByComma(form.getRecipients());
        String features = joinByComma(form.getEnabledFeatures());

        try {
            ctx.update(SETTINGS)
                    .set(SETTINGS.HEARTBEAT_INTERVAL_IN_SECONDS, toSec(form.getHeartbeat()))
                    .set(SETTINGS.HOURS_TO_EXPIRE, form.getExpiration())
                    .set(SETTINGS.MAIL_ENABLED, form.getEnabled())
                    .set(SETTINGS.MAIL_HOST, form.getMailHost())
                    .set(SETTINGS.MAIL_USERNAME, form.getUsername())
                    .set(SETTINGS.MAIL_PASSWORD, form.getPassword())
                    .set(SETTINGS.MAIL_FROM, form.getFrom())
                    .set(SETTINGS.MAIL_PROTOCOL, form.getProtocol())
                    .set(SETTINGS.MAIL_PORT, form.getPort())
                    .set(SETTINGS.MAIL_RECIPIENTS, eMails)
                    .set(SETTINGS.NOTIFICATION_FEATURES, features)
                    .where(SETTINGS.APP_ID.eq(APP_ID))
                    .execute();

        } catch (DataAccessException e) {
            throw new SteveException("FAILED to save the settings", e);
        }
    }

    private static int toSec(int minutes) {
        return (int) TimeUnit.MINUTES.toSeconds(minutes);
    }
}
