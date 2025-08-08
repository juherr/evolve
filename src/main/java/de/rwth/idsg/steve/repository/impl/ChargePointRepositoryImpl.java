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

import de.rwth.idsg.steve.ocpp.OcppProtocol;
import de.rwth.idsg.steve.repository.ChargePointRepository;
import de.rwth.idsg.steve.repository.dto.ChargePoint;
import de.rwth.idsg.steve.repository.dto.ChargePointSelect;
import de.rwth.idsg.steve.repository.dto.ConnectorStatus;
import de.rwth.idsg.steve.utils.DateTimeUtils;
import jooq.steve.db.tables.records.ChargeBoxRecord;
import lombok.RequiredArgsConstructor;
import ocpp.cs._2015._10.RegistrationStatus;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.rwth.idsg.steve.utils.DateTimeUtils.toOffsetDateTime;
import static jooq.steve.db.tables.ChargeBox.CHARGE_BOX;
import static jooq.steve.db.tables.Connector.CONNECTOR;
import static jooq.steve.db.tables.ConnectorStatus.CONNECTOR_STATUS;

@Repository
@RequiredArgsConstructor
public class ChargePointRepositoryImpl implements ChargePointRepository {

    private final DSLContext ctx;

    @Override
    public Optional<String> getRegistrationStatus(String chargeBoxId) {
        String status = ctx.select(CHARGE_BOX.REGISTRATION_STATUS)
                .from(CHARGE_BOX)
                .where(CHARGE_BOX.CHARGE_BOX_ID.eq(chargeBoxId))
                .fetchOne(CHARGE_BOX.REGISTRATION_STATUS);

        return Optional.ofNullable(status);
    }

    @Override
    public List<ChargePointSelect> getChargePointSelect(OcppProtocol protocol, List<String> inStatusFilter, List<String> chargeBoxIdFilter) {
        Condition chargeBoxIdCondition = CollectionUtils.isEmpty(chargeBoxIdFilter)
                ? DSL.trueCondition()
                : CHARGE_BOX.CHARGE_BOX_ID.in(chargeBoxIdFilter);

        return ctx.select(CHARGE_BOX.CHARGE_BOX_ID, CHARGE_BOX.ENDPOINT_ADDRESS)
                .from(CHARGE_BOX)
                .where(CHARGE_BOX.OCPP_PROTOCOL.equal(protocol.getCompositeValue()))
                .and(CHARGE_BOX.ENDPOINT_ADDRESS.isNotNull())
                .and(CHARGE_BOX.REGISTRATION_STATUS.in(inStatusFilter))
                .and(chargeBoxIdCondition)
                .fetch()
                .map(r -> new ChargePointSelect(protocol, r.value1(), r.value2()));
    }

    @Override
    public List<String> getChargeBoxIds() {
        return ctx.select(CHARGE_BOX.CHARGE_BOX_ID)
                .from(CHARGE_BOX)
                .fetch(CHARGE_BOX.CHARGE_BOX_ID);
    }

    @Override
    public Map<String, Integer> getChargeBoxIdPkPair(List<String> chargeBoxIdList) {
        return ctx.select(CHARGE_BOX.CHARGE_BOX_ID, CHARGE_BOX.CHARGE_BOX_PK)
                .from(CHARGE_BOX)
                .where(CHARGE_BOX.CHARGE_BOX_ID.in(chargeBoxIdList))
                .fetchMap(CHARGE_BOX.CHARGE_BOX_ID, CHARGE_BOX.CHARGE_BOX_PK);
    }

    @Override
    public Optional<ChargePoint.Details> getDetails(int chargeBoxPk) {
        return ctx.selectFrom(CHARGE_BOX)
                .where(CHARGE_BOX.CHARGE_BOX_PK.equal(chargeBoxPk))
                .fetchOptional()
                .map(ChargePoint.Details::new);
    }

    @Override
    public List<ConnectorStatus> getChargePointConnectorStatus(String chargeBoxId, String status) {
        // find out the latest timestamp for each connector
        Field<Integer> t1Pk = CONNECTOR_STATUS.CONNECTOR_PK.as("t1_pk");
        Field<LocalDateTime> t1TsMax = DSL.max(CONNECTOR_STATUS.STATUS_TIMESTAMP).as("t1_ts_max");
        Table<?> t1 = ctx.select(t1Pk, t1TsMax)
                .from(CONNECTOR_STATUS)
                .groupBy(CONNECTOR_STATUS.CONNECTOR_PK)
                .asTable("t1");

        // get the status table with latest timestamps only
        Field<Integer> t2Pk = CONNECTOR_STATUS.CONNECTOR_PK.as("t2_pk");
        Field<LocalDateTime> t2Ts = CONNECTOR_STATUS.STATUS_TIMESTAMP.as("t2_ts");
        Field<String> t2Status = CONNECTOR_STATUS.STATUS.as("t2_status");
        Field<String> t2Error = CONNECTOR_STATUS.ERROR_CODE.as("t2_error");
        Table<?> t2 = ctx.selectDistinct(t2Pk, t2Ts, t2Status, t2Error)
                .from(CONNECTOR_STATUS)
                .join(t1)
                .on(CONNECTOR_STATUS.CONNECTOR_PK.equal(t1.field(t1Pk)))
                .and(CONNECTOR_STATUS.STATUS_TIMESTAMP.equal(t1.field(t1TsMax)))
                .asTable("t2");

        // https://github.com/steve-community/steve/issues/691
        Condition chargeBoxCondition = CHARGE_BOX.REGISTRATION_STATUS.eq(RegistrationStatus.ACCEPTED.value());

        if (chargeBoxId != null) {
            chargeBoxCondition = chargeBoxCondition.and(CHARGE_BOX.CHARGE_BOX_ID.eq(chargeBoxId));
        }

        final Condition statusCondition;
        if (status == null) {
            statusCondition = DSL.noCondition();
        } else {
            statusCondition = t2.field(t2Status).eq(status);
        }

        return ctx.select(
                CHARGE_BOX.CHARGE_BOX_PK,
                CONNECTOR.CHARGE_BOX_ID,
                CONNECTOR.CONNECTOR_ID,
                t2.field(t2Ts),
                t2.field(t2Status),
                t2.field(t2Error),
                CHARGE_BOX.OCPP_PROTOCOL)
                .from(t2)
                .join(CONNECTOR)
                .on(CONNECTOR.CONNECTOR_PK.eq(t2.field(t2Pk)))
                .join(CHARGE_BOX)
                .on(CHARGE_BOX.CHARGE_BOX_ID.eq(CONNECTOR.CHARGE_BOX_ID))
                .where(chargeBoxCondition, statusCondition)
                .orderBy(t2.field(t2Ts).desc())
                .fetch()
                .map(r -> ConnectorStatus.builder()
                        .chargeBoxPk(r.value1())
                        .chargeBoxId(r.value2())
                        .connectorId(r.value3())
                        .timeStamp(DateTimeUtils.humanize(r.value4()))
                        .statusTimestamp(toOffsetDateTime(r.value4()))
                        .status(r.value5())
                        .errorCode(r.value6())
                        .ocppProtocol(r.value7() == null ? null : OcppProtocol.fromCompositeValue(r.value7()))
                        .build()
                );
    }

    @Override
    public List<Integer> getNonZeroConnectorIds(String chargeBoxId) {
        return ctx.select(CONNECTOR.CONNECTOR_ID)
                .from(CONNECTOR)
                .where(CONNECTOR.CHARGE_BOX_ID.equal(chargeBoxId))
                .and(CONNECTOR.CONNECTOR_ID.notEqual(0))
                .fetch(CONNECTOR.CONNECTOR_ID);
    }

    @Override
    public void addChargePointList(List<String> chargeBoxIdList) {
        List<ChargeBoxRecord> batch = chargeBoxIdList.stream()
                .map(s -> ctx.newRecord(CHARGE_BOX)
                        .setChargeBoxId(s)
                        .setInsertConnectorStatusAfterTransactionMsg(false))
                .collect(Collectors.toList());

        ctx.batchInsert(batch).execute();
    }

    @Override
    public int addChargePoint(ChargePoint.Details form, Integer addressPk) {
        return ctx.insertInto(CHARGE_BOX)
                .set(CHARGE_BOX.CHARGE_BOX_ID, form.getChargeBoxId())
                .set(CHARGE_BOX.DESCRIPTION, form.getDescription())
                .set(CHARGE_BOX.LOCATION_LATITUDE, form.getLocationLatitude())
                .set(CHARGE_BOX.LOCATION_LONGITUDE, form.getLocationLongitude())
                .set(CHARGE_BOX.INSERT_CONNECTOR_STATUS_AFTER_TRANSACTION_MSG, form.isInsertConnectorStatusAfterTransactionMsg())
                .set(CHARGE_BOX.REGISTRATION_STATUS, form.getRegistrationStatus())
                .set(CHARGE_BOX.NOTE, form.getNote())
                .set(CHARGE_BOX.ADMIN_ADDRESS, form.getAdminAddress())
                .set(CHARGE_BOX.ADDRESS_PK, addressPk)
                .returning(CHARGE_BOX.CHARGE_BOX_PK)
                .fetchOne()
                .getChargeBoxPk();
    }

    @Override
    public void updateChargePoint(ChargePoint.Details form, Integer addressPk) {
        ctx.update(CHARGE_BOX)
                .set(CHARGE_BOX.DESCRIPTION, form.getDescription())
                .set(CHARGE_BOX.LOCATION_LATITUDE, form.getLocationLatitude())
                .set(CHARGE_BOX.LOCATION_LONGITUDE, form.getLocationLongitude())
                .set(CHARGE_BOX.INSERT_CONNECTOR_STATUS_AFTER_TRANSACTION_MSG, form.isInsertConnectorStatusAfterTransactionMsg())
                .set(CHARGE_BOX.REGISTRATION_STATUS, form.getRegistrationStatus())
                .set(CHARGE_BOX.NOTE, form.getNote())
                .set(CHARGE_BOX.ADMIN_ADDRESS, form.getAdminAddress())
                .set(CHARGE_BOX.ADDRESS_PK, addressPk)
                .where(CHARGE_BOX.CHARGE_BOX_PK.equal(form.getChargeBoxPk()))
                .execute();
    }

    @Override
    public void deleteChargePoint(int chargeBoxPk) {
        ctx.delete(CHARGE_BOX)
                .where(CHARGE_BOX.CHARGE_BOX_PK.equal(chargeBoxPk))
                .execute();
    }

    @Override
    public int selectAddressId(int chargeBoxPk) {
        return ctx.select(CHARGE_BOX.ADDRESS_PK)
                .from(CHARGE_BOX)
                .where(CHARGE_BOX.CHARGE_BOX_PK.eq(chargeBoxPk))
                .fetchOne(CHARGE_BOX.ADDRESS_PK);
    }
}
