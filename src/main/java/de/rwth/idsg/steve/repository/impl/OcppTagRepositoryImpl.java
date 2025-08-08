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
import de.rwth.idsg.steve.repository.OcppTagRepository;
import de.rwth.idsg.steve.repository.dto.OcppTag;
import de.rwth.idsg.steve.utils.DateTimeUtils;
import de.rwth.idsg.steve.web.dto.OcppTagForm;
import jooq.steve.db.tables.records.OcppTagRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record10;
import org.jooq.RecordMapper;
import org.jooq.exception.DataAccessException;
import org.springframework.stereotype.Repository;

import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static jooq.steve.db.tables.OcppTag.OCPP_TAG;
import static jooq.steve.db.tables.OcppTagActivity.OCPP_TAG_ACTIVITY;

@Repository
@RequiredArgsConstructor
public class OcppTagRepositoryImpl implements OcppTagRepository {

    private final DSLContext ctx;

    @Override
    public List<OcppTag.OcppTagOverview> getRecords() {
        return ctx.selectFrom(OCPP_TAG_ACTIVITY)
                .fetch()
                .map(new OcppTagOverviewMapper());
    }

    @Override
    public List<OcppTag.OcppTagOverview> getRecords(List<String> idTagList) {
        return ctx.selectFrom(OCPP_TAG_ACTIVITY)
                .where(OCPP_TAG_ACTIVITY.ID_TAG.in(idTagList))
                .fetch()
                .map(new OcppTagOverviewMapper());
    }

    @Override
    public Optional<OcppTag.OcppTagOverview> getRecord(String idTag) {
        return ctx.selectFrom(OCPP_TAG_ACTIVITY)
                .where(OCPP_TAG_ACTIVITY.ID_TAG.equal(idTag))
                .fetchOptional()
                .map(new OcppTagOverviewMapper());
    }

    @Override
    public Optional<OcppTag.OcppTagOverview> getRecord(int ocppTagPk) {
        return ctx.selectFrom(OCPP_TAG_ACTIVITY)
                .where(OCPP_TAG_ACTIVITY.OCPP_TAG_PK.equal(ocppTagPk))
                .fetchOptional()
                .map(new OcppTagOverviewMapper());
    }

    @Override
    public List<String> getIdTags() {
        return ctx.select(OCPP_TAG.ID_TAG)
                .from(OCPP_TAG)
                .fetch(OCPP_TAG.ID_TAG);
    }

    @Override
    public List<String> getActiveIdTags() {
        return ctx.select(OCPP_TAG_ACTIVITY.ID_TAG)
                .from(OCPP_TAG_ACTIVITY)
                .where(OCPP_TAG_ACTIVITY.ACTIVE_TRANSACTION_COUNT
                        .lessThan(OCPP_TAG_ACTIVITY.MAX_ACTIVE_TRANSACTION_COUNT.cast(Long.class))
                        .or(OCPP_TAG_ACTIVITY.MAX_ACTIVE_TRANSACTION_COUNT.lessThan(0)))
                .and(OCPP_TAG_ACTIVITY.BLOCKED.isFalse())
                .and(OCPP_TAG_ACTIVITY.EXPIRY_DATE.isNull()
                        .or(OCPP_TAG_ACTIVITY.EXPIRY_DATE.greaterThan(java.time.LocalDateTime.now())))
                .fetch(OCPP_TAG_ACTIVITY.ID_TAG);
    }

    @Override
    public List<String> getParentIdTags() {
        return ctx.selectDistinct(OCPP_TAG.PARENT_ID_TAG)
                .from(OCPP_TAG)
                .where(OCPP_TAG.PARENT_ID_TAG.isNotNull())
                .fetch(OCPP_TAG.PARENT_ID_TAG);
    }

    @Override
    public String getParentIdtag(String idTag) {
        return ctx.select(OCPP_TAG.PARENT_ID_TAG)
                .from(OCPP_TAG)
                .where(OCPP_TAG.ID_TAG.eq(idTag))
                .fetchOne()
                .value1();
    }

    @Override
    public void addOcppTagList(List<String> idTagList) {
        List<OcppTagRecord> batch = idTagList.stream()
                .map(s -> ctx.newRecord(OCPP_TAG)
                        .setIdTag(s))
                .collect(Collectors.toList());

        ctx.batchInsert(batch).execute();
    }

    @Override
    public int addOcppTag(OcppTagForm u) {
        try {
            return ctx.insertInto(OCPP_TAG)
                    .set(OCPP_TAG.ID_TAG, u.getIdTag())
                    .set(OCPP_TAG.PARENT_ID_TAG, u.getParentIdTag())
                    .set(OCPP_TAG.EXPIRY_DATE, u.getExpiryDate())
                    .set(OCPP_TAG.MAX_ACTIVE_TRANSACTION_COUNT, u.getMaxActiveTransactionCount())
                    .set(OCPP_TAG.NOTE, u.getNote())
                    .returning(OCPP_TAG.OCPP_TAG_PK)
                    .fetchOne()
                    .getOcppTagPk();

        } catch (DataAccessException e) {
            if (e.getCause() instanceof SQLIntegrityConstraintViolationException) {
                throw new SteveException.AlreadyExists("A user with idTag '%s' already exists.", u.getIdTag());
            } else {
                throw new SteveException("Execution of addOcppTag for idTag '%s' FAILED.", u.getIdTag(), e);
            }
        }
    }

    @Override
    public void updateOcppTag(OcppTagForm u) {
        try {
            ctx.update(OCPP_TAG)
                    .set(OCPP_TAG.PARENT_ID_TAG, u.getParentIdTag())
                    .set(OCPP_TAG.EXPIRY_DATE, u.getExpiryDate())
                    .set(OCPP_TAG.MAX_ACTIVE_TRANSACTION_COUNT, u.getMaxActiveTransactionCount())
                    .set(OCPP_TAG.NOTE, u.getNote())
                    .where(OCPP_TAG.OCPP_TAG_PK.equal(u.getOcppTagPk()))
                    .execute();
        } catch (DataAccessException e) {
            throw new SteveException("Execution of updateOcppTag for idTag '%s' FAILED.", u.getIdTag(), e);
        }
    }

    @Override
    public void deleteOcppTag(int ocppTagPk) {
        try {
            ctx.delete(OCPP_TAG)
                    .where(OCPP_TAG.OCPP_TAG_PK.equal(ocppTagPk))
                    .execute();
        } catch (DataAccessException e) {
            throw new SteveException("Execution of deleteOcppTag for idTag FAILED.", e);
        }
    }

    private static final class OcppTagOverviewMapper implements RecordMapper<Record10<Integer, Integer, String, String, OffsetDateTime, Boolean, Boolean, Integer, Long, String>, OcppTag.OcppTagOverview> {
        @Override
        public OcppTag.OcppTagOverview map(Record10<Integer, Integer, String, String, OffsetDateTime, Boolean, Boolean, Integer, Long, String> r) {
            OffsetDateTime expiryDate = r.value5();
            String expiryDateFormatted = (expiryDate == null) ? null : DateTimeUtils.humanize(expiryDate.toLocalDateTime());

            return OcppTag.OcppTagOverview.builder()
                    .ocppTagPk(r.value1())
                    .parentOcppTagPk(r.value2())
                    .idTag(r.value3())
                    .parentIdTag(r.value4())
                    .expiryDate(expiryDate)
                    .expiryDateFormatted(expiryDateFormatted)
                    .inTransaction(r.value6())
                    .blocked(r.value7())
                    .maxActiveTransactionCount(r.value8())
                    .activeTransactionCount(r.value9())
                    .note(r.value10())
                    .build();
        }
    }
}
