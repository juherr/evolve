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
import de.rwth.idsg.steve.repository.ChargingProfileRepository;
import de.rwth.idsg.steve.repository.dto.ChargingProfile;
import de.rwth.idsg.steve.repository.dto.ChargingProfileAssignment;
import de.rwth.idsg.steve.web.dto.ChargingProfileAssignmentQueryForm;
import de.rwth.idsg.steve.web.dto.ChargingProfileForm;
import de.rwth.idsg.steve.web.dto.ChargingProfileQueryForm;
import jooq.steve.db.tables.records.ChargingProfileRecord;
import jooq.steve.db.tables.records.ChargingSchedulePeriodRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;

import java.util.List;

import static de.rwth.idsg.steve.utils.CustomDSL.includes;
import static jooq.steve.db.Tables.CHARGING_PROFILE;
import static jooq.steve.db.Tables.CHARGE_BOX;
import static jooq.steve.db.Tables.CONNECTOR;
import static jooq.steve.db.Tables.CONNECTOR_CHARGING_PROFILE;

@Service
@RequiredArgsConstructor
public class ChargingProfileService {

    private final DSLContext ctx;
    private final ChargingProfileRepository chargingProfileRepository;

    public List<ChargingProfileAssignment> getAssignments(ChargingProfileAssignmentQueryForm query) {
        Condition conditions = DSL.trueCondition();

        if (query.getChargeBoxId() != null) {
            conditions = conditions.and(CHARGE_BOX.CHARGE_BOX_ID.eq(query.getChargeBoxId()));
        }

        if (query.getChargingProfilePk() != null) {
            conditions = conditions.and(CHARGING_PROFILE.CHARGING_PROFILE_PK.eq(query.getChargingProfilePk()));
        }

        if (query.getChargingProfileDescription() != null) {
            conditions = conditions.and(includes(CHARGING_PROFILE.DESCRIPTION, query.getChargingProfileDescription()));
        }

        return ctx.select(
                CHARGE_BOX.CHARGE_BOX_PK,
                CHARGE_BOX.CHARGE_BOX_ID,
                CONNECTOR.CONNECTOR_ID,
                CONNECTOR_CHARGING_PROFILE.CHARGING_PROFILE_PK,
                CHARGING_PROFILE.DESCRIPTION)
                .from(CONNECTOR_CHARGING_PROFILE)
                .join(CONNECTOR)
                .on(CONNECTOR.CONNECTOR_PK.eq(CONNECTOR_CHARGING_PROFILE.CONNECTOR_PK))
                .join(CHARGING_PROFILE)
                .on(CHARGING_PROFILE.CHARGING_PROFILE_PK.eq(CONNECTOR_CHARGING_PROFILE.CHARGING_PROFILE_PK))
                .join(CHARGE_BOX)
                .on(CHARGE_BOX.CHARGE_BOX_ID.eq(CONNECTOR.CHARGE_BOX_ID))
                .where(conditions)
                .orderBy(
                        CHARGE_BOX.CHARGE_BOX_ID,
                        CONNECTOR.CONNECTOR_ID,
                        CONNECTOR_CHARGING_PROFILE.CHARGING_PROFILE_PK)
                .fetch()
                .map(k -> ChargingProfileAssignment.builder()
                        .chargeBoxPk(k.value1())
                        .chargeBoxId(k.value2())
                        .connectorId(k.value3())
                        .chargingProfilePk(k.value4())
                        .chargingProfileDescription(k.value5())
                        .build()
                );
    }

    public List<ChargingProfile.Overview> getOverview(ChargingProfileQueryForm form) {
        Condition conditions = DSL.trueCondition();

        if (form.getChargingProfilePk() != null) {
            conditions = conditions.and(CHARGING_PROFILE.CHARGING_PROFILE_PK.eq(form.getChargingProfilePk()));
        }

        if (form.getStackLevel() != null) {
            conditions = conditions.and(CHARGING_PROFILE.STACK_LEVEL.eq(form.getStackLevel()));
        }

        if (form.getDescription() != null) {
            conditions = conditions.and(includes(CHARGING_PROFILE.DESCRIPTION, form.getDescription()));
        }

        if (form.getProfilePurpose() != null) {
            conditions = conditions.and(CHARGING_PROFILE.CHARGING_PROFILE_PURPOSE.eq(form.getProfilePurpose().value()));
        }

        if (form.getProfileKind() != null) {
            conditions = conditions.and(CHARGING_PROFILE.CHARGING_PROFILE_KIND.eq(form.getProfileKind().value()));
        }

        if (form.getRecurrencyKind() != null) {
            conditions = conditions.and(CHARGING_PROFILE.RECURRENCY_KIND.eq(form.getRecurrencyKind().value()));
        }

        if (form.getValidFrom() != null) {
            conditions = conditions.and(CHARGING_PROFILE.VALID_FROM.greaterOrEqual(form.getValidFrom()));
        }

        if (form.getValidTo() != null) {
            conditions = conditions.and(CHARGING_PROFILE.VALID_TO.lessOrEqual(form.getValidTo()));
        }

        return ctx.selectFrom(CHARGING_PROFILE)
                .where(conditions)
                .fetch()
                .map(r -> ChargingProfile.Overview.builder()
                        .chargingProfilePk(r.getChargingProfilePk())
                        .stackLevel(r.getStackLevel())
                        .description(r.getDescription())
                        .profilePurpose(r.getChargingProfilePurpose())
                        .profileKind(r.getChargingProfileKind())
                        .recurrencyKind(r.getRecurrencyKind())
                        .validFrom(r.getValidFrom())
                        .validTo(r.getValidTo())
                        .build()
                );
    }

    public ChargingProfile.Details getDetails(int chargingProfilePk) {
        ChargingProfileRecord profile = chargingProfileRepository.getProfile(chargingProfilePk);
        List<ChargingSchedulePeriodRecord> periods = chargingProfileRepository.getPeriods(chargingProfilePk);
        return new ChargingProfile.Details(profile, periods);
    }

    public int add(ChargingProfileForm form) {
        return ctx.transactionResult(configuration -> {
            try {
                return chargingProfileRepository.add(form);
            } catch (DataAccessException e) {
                throw new SteveException("Failed to add the charging profile", e);
            }
        });
    }

    public void update(ChargingProfileForm form) {
        checkProfileUsage(form.getChargingProfilePk());

        ctx.transaction(configuration -> {
            try {
                chargingProfileRepository.update(form);
            } catch (DataAccessException e) {
                throw new SteveException("Failed to update the charging profile with id '%s'",
                        form.getChargingProfilePk(), e);
            }
        });
    }

    public void delete(int chargingProfilePk) {
        checkProfileUsage(chargingProfilePk);
        chargingProfileRepository.delete(chargingProfilePk);
    }

    private void checkProfileUsage(int chargingProfilePk) {
        List<String> r = chargingProfileRepository.getChargeBoxIdsForProfile(chargingProfilePk);
        if (!r.isEmpty()) {
            throw new SteveException("Cannot modify this charging profile, since the following stations are still using it: %s", r);
        }
    }
}
