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

import de.rwth.idsg.steve.repository.ChargingProfileRepository;
import de.rwth.idsg.steve.repository.dto.ChargingProfile;
import de.rwth.idsg.steve.web.dto.ChargingProfileForm;
import jooq.steve.db.tables.records.ChargingProfileRecord;
import jooq.steve.db.tables.records.ChargingSchedulePeriodRecord;
import lombok.RequiredArgsConstructor;
import ocpp.cp._2015._10.ChargingProfilePurposeType;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.SelectConditionStep;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

import static jooq.steve.db.Tables.CHARGING_PROFILE;
import static jooq.steve.db.Tables.CHARGING_SCHEDULE_PERIOD;
import static jooq.steve.db.Tables.CONNECTOR;
import static jooq.steve.db.Tables.CONNECTOR_CHARGING_PROFILE;

@Repository
@RequiredArgsConstructor
public class ChargingProfileRepositoryImpl implements ChargingProfileRepository {

    private final DSLContext ctx;

    @Override
    public void setProfile(int chargingProfilePk, String chargeBoxId, int connectorId) {
        OcppServerRepositoryImpl.insertIgnoreConnector(ctx, chargeBoxId, connectorId);

        SelectConditionStep<Record1<Integer>> connectorPkSelect = ctx.select(CONNECTOR.CONNECTOR_PK)
                .from(CONNECTOR)
                .where(CONNECTOR.CHARGE_BOX_ID.eq(chargeBoxId))
                .and(CONNECTOR.CONNECTOR_ID.eq(connectorId));

        ctx.insertInto(CONNECTOR_CHARGING_PROFILE)
                .set(CONNECTOR_CHARGING_PROFILE.CONNECTOR_PK, connectorPkSelect)
                .set(CONNECTOR_CHARGING_PROFILE.CHARGING_PROFILE_PK, chargingProfilePk)
                .execute();
    }

    @Override
    public void clearProfile(int chargingProfilePk, String chargeBoxId) {
        SelectConditionStep<Record1<Integer>> connectorPkSelect = ctx.select(CONNECTOR.CONNECTOR_PK)
                .from(CONNECTOR)
                .where(CONNECTOR.CHARGE_BOX_ID.eq(chargeBoxId));

        ctx.delete(CONNECTOR_CHARGING_PROFILE)
                .where(CONNECTOR_CHARGING_PROFILE.CONNECTOR_PK.in(connectorPkSelect))
                .and(CONNECTOR_CHARGING_PROFILE.CHARGING_PROFILE_PK.eq(chargingProfilePk))
                .execute();
    }

    @Override
    public void clearProfile(String chargeBoxId, Integer connectorId, ChargingProfilePurposeType purpose, Integer stackLevel) {
        // -------------------------------------------------------------------------
        // Connector select
        // -------------------------------------------------------------------------

        Condition connectorIdCondition = (connectorId == null) ? DSL.trueCondition() : CONNECTOR.CONNECTOR_ID.eq(connectorId);

        SelectConditionStep<Record1<Integer>> connectorPkSelect = ctx.select(CONNECTOR.CONNECTOR_PK)
                .from(CONNECTOR)
                .where(CONNECTOR.CHARGE_BOX_ID.eq(chargeBoxId))
                .and(connectorIdCondition);

        // -------------------------------------------------------------------------
        // Profile select
        // -------------------------------------------------------------------------

        Condition profilePkCondition;

        if (purpose == null && stackLevel == null) {
            profilePkCondition = DSL.trueCondition();
        } else {
            Condition purposeCondition = (purpose == null) ? DSL.trueCondition() : CHARGING_PROFILE.CHARGING_PROFILE_PURPOSE.eq(purpose.value());

            Condition stackLevelCondition = (stackLevel == null) ? DSL.trueCondition() : CHARGING_PROFILE.STACK_LEVEL.eq(stackLevel);

            SelectConditionStep<Record1<Integer>> profilePkSelect = ctx.select(CHARGING_PROFILE.CHARGING_PROFILE_PK)
                    .from(CHARGING_PROFILE)
                    .where(purposeCondition)
                    .and(stackLevelCondition);

            profilePkCondition = CONNECTOR_CHARGING_PROFILE.CHARGING_PROFILE_PK.in(profilePkSelect);
        }

        // -------------------------------------------------------------------------
        // Delete execution
        // -------------------------------------------------------------------------

        ctx.delete(CONNECTOR_CHARGING_PROFILE)
                .where(CONNECTOR_CHARGING_PROFILE.CONNECTOR_PK.in(connectorPkSelect))
                .and(profilePkCondition)
                .execute();
    }

    @Override
    public List<ChargingProfile.BasicInfo> getBasicInfo() {
        return ctx.select(CHARGING_PROFILE.CHARGING_PROFILE_PK, CHARGING_PROFILE.DESCRIPTION)
                .from(CHARGING_PROFILE)
                .fetch()
                .map(r -> new ChargingProfile.BasicInfo(r.value1(), r.value2()));
    }

    @Override
    public ChargingProfileRecord getProfile(int chargingProfilePk) {
        return ctx.selectFrom(CHARGING_PROFILE)
                .where(CHARGING_PROFILE.CHARGING_PROFILE_PK.eq(chargingProfilePk))
                .fetchOne();
    }

    @Override
    public List<ChargingSchedulePeriodRecord> getPeriods(int chargingProfilePk) {
        return ctx.selectFrom(CHARGING_SCHEDULE_PERIOD)
                .where(CHARGING_SCHEDULE_PERIOD.CHARGING_PROFILE_PK.eq(chargingProfilePk))
                .fetch();
    }

    @Override
    public int add(ChargingProfileForm form) {
        int profilePk = ctx.insertInto(CHARGING_PROFILE)
                .set(CHARGING_PROFILE.DESCRIPTION, form.getDescription())
                .set(CHARGING_PROFILE.NOTE, form.getNote())
                .set(CHARGING_PROFILE.STACK_LEVEL, form.getStackLevel())
                .set(CHARGING_PROFILE.CHARGING_PROFILE_PURPOSE, form.getChargingProfilePurpose().value())
                .set(CHARGING_PROFILE.CHARGING_PROFILE_KIND, form.getChargingProfileKind().value())
                .set(CHARGING_PROFILE.RECURRENCY_KIND, form.getRecurrencyKind() == null ? null : form.getRecurrencyKind().value())
                .set(CHARGING_PROFILE.VALID_FROM, form.getValidFrom())
                .set(CHARGING_PROFILE.VALID_TO, form.getValidTo())
                .set(CHARGING_PROFILE.DURATION_IN_SECONDS, form.getDurationInSeconds())
                .set(CHARGING_PROFILE.START_SCHEDULE, form.getStartSchedule())
                .set(CHARGING_PROFILE.CHARGING_RATE_UNIT, form.getChargingRateUnit().value())
                .set(CHARGING_PROFILE.MIN_CHARGING_RATE, form.getMinChargingRate())
                .returning(CHARGING_SCHEDULE_PERIOD.CHARGING_PROFILE_PK)
                .fetchOne()
                .getChargingProfilePk();

        form.setChargingProfilePk(profilePk);
        insertPeriods(form);
        return profilePk;
    }

    @Override
    public void update(ChargingProfileForm form) {
        ctx.update(CHARGING_PROFILE)
                .set(CHARGING_PROFILE.DESCRIPTION, form.getDescription())
                .set(CHARGING_PROFILE.NOTE, form.getNote())
                .set(CHARGING_PROFILE.STACK_LEVEL, form.getStackLevel())
                .set(CHARGING_PROFILE.CHARGING_PROFILE_PURPOSE, form.getChargingProfilePurpose().value())
                .set(CHARGING_PROFILE.CHARGING_PROFILE_KIND, form.getChargingProfileKind().value())
                .set(CHARGING_PROFILE.RECURRENCY_KIND, form.getRecurrencyKind() == null ? null : form.getRecurrencyKind().value())
                .set(CHARGING_PROFILE.VALID_FROM, form.getValidFrom())
                .set(CHARGING_PROFILE.VALID_TO, form.getValidTo())
                .set(CHARGING_PROFILE.DURATION_IN_SECONDS, form.getDurationInSeconds())
                .set(CHARGING_PROFILE.START_SCHEDULE, form.getStartSchedule())
                .set(CHARGING_PROFILE.CHARGING_RATE_UNIT, form.getChargingRateUnit().value())
                .set(CHARGING_PROFILE.MIN_CHARGING_RATE, form.getMinChargingRate())
                .where(CHARGING_PROFILE.CHARGING_PROFILE_PK.eq(form.getChargingProfilePk()))
                .execute();

        // -------------------------------------------------------------------------
        // the form contains all period information for this schedule. instead of
        // computing a delta about what to insert/update, we can simply delete everything
        // for this profile and re-insert.
        // -------------------------------------------------------------------------

        ctx.delete(CHARGING_SCHEDULE_PERIOD)
                .where(CHARGING_SCHEDULE_PERIOD.CHARGING_PROFILE_PK.eq(form.getChargingProfilePk()))
                .execute();

        insertPeriods(form);
    }

    @Override
    public void delete(int chargingProfilePk) {
        ctx.delete(CHARGING_PROFILE)
                .where(CHARGING_PROFILE.CHARGING_PROFILE_PK.eq(chargingProfilePk))
                .execute();
    }

    @Override
    public List<String> getChargeBoxIdsForProfile(int chargingProfilePk) {
        return ctx.select(CONNECTOR.CHARGE_BOX_ID)
                .from(CONNECTOR_CHARGING_PROFILE)
                .join(CONNECTOR)
                .on(CONNECTOR.CONNECTOR_PK.eq(CONNECTOR_CHARGING_PROFILE.CONNECTOR_PK))
                .where(CONNECTOR_CHARGING_PROFILE.CHARGING_PROFILE_PK.eq(chargingProfilePk))
                .fetch(CONNECTOR.CHARGE_BOX_ID);
    }

    private void insertPeriods(ChargingProfileForm form) {
        if (CollectionUtils.isEmpty(form.getSchedulePeriodMap())) {
            return;
        }

        List<ChargingSchedulePeriodRecord> r = form.getSchedulePeriodMap()
                .values()
                .stream()
                .map(k -> ctx.newRecord(CHARGING_SCHEDULE_PERIOD)
                        .setChargingProfilePk(form.getChargingProfilePk())
                        .setStartPeriodInSeconds(k.getStartPeriodInSeconds())
                        .setPowerLimit(k.getPowerLimit())
                        .setNumberPhases(k.getNumberPhases()))
                .collect(Collectors.toList());

        ctx.batchInsert(r).execute();
    }
}
