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
import de.rwth.idsg.steve.repository.TransactionRepository;
import de.rwth.idsg.steve.web.dto.TransactionQueryForm;
import jooq.steve.db.enums.TransactionStopEventActor;
import jooq.steve.db.tables.records.ConnectorMeterValueRecord;
import jooq.steve.db.tables.records.TransactionStartRecord;
import ocpp.cs._2015._10.UnitOfMeasure;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record12;
import org.jooq.Record9;
import org.jooq.Result;
import org.jooq.SelectQuery;
import org.springframework.stereotype.Repository;

import java.io.Writer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static de.rwth.idsg.steve.utils.CustomDSL.date;
import static jooq.steve.db.tables.ChargeBox.CHARGE_BOX;
import static jooq.steve.db.tables.Connector.CONNECTOR;
import static jooq.steve.db.tables.ConnectorMeterValue.CONNECTOR_METER_VALUE;
import static jooq.steve.db.tables.OcppTag.OCPP_TAG;
import static jooq.steve.db.tables.Transaction.TRANSACTION;
import static jooq.steve.db.tables.TransactionStart.TRANSACTION_START;

@Repository
public class TransactionRepositoryImpl implements TransactionRepository {

    private final DSLContext ctx;

    public TransactionRepositoryImpl(DSLContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Result<? extends Record> getTransactions(TransactionQueryForm form) {
        return getInternal(form).fetch();
    }

    @Override
    public void writeTransactionsCSV(TransactionQueryForm form, Writer writer) {
        getInternalCSV(form).fetch().formatCSV(writer);
    }

    @Override
    public List<Integer> getActiveTransactionIds(String chargeBoxId) {
        return ctx.select(TRANSACTION.TRANSACTION_PK)
                .from(TRANSACTION)
                .join(CONNECTOR)
                .on(TRANSACTION.CONNECTOR_PK.equal(CONNECTOR.CONNECTOR_PK))
                .and(CONNECTOR.CHARGE_BOX_ID.equal(chargeBoxId))
                .where(TRANSACTION.STOP_TIMESTAMP.isNull())
                .fetch(TRANSACTION.TRANSACTION_PK);
    }

    @Override
    public Result<? extends Record> getTransaction(int transactionPk) {
        TransactionQueryForm form = new TransactionQueryForm();
        form.setTransactionPk(transactionPk);
        form.setType(TransactionQueryForm.QueryType.ALL);
        form.setPeriodType(TransactionQueryForm.QueryPeriodType.ALL);

        return getInternal(form).fetch();
    }

    @Override
    public TransactionStartRecord getNextTransaction(String chargeBoxId, int connectorId, LocalDateTime startTimestamp) {
        return ctx.selectFrom(TRANSACTION_START)
                .where(TRANSACTION_START.CONNECTOR_PK.eq(ctx.select(CONNECTOR.CONNECTOR_PK)
                        .from(CONNECTOR)
                        .where(CONNECTOR.CHARGE_BOX_ID.equal(chargeBoxId))
                        .and(CONNECTOR.CONNECTOR_ID.equal(connectorId))))
                .and(TRANSACTION_START.START_TIMESTAMP.greaterThan(startTimestamp))
                .orderBy(TRANSACTION_START.START_TIMESTAMP)
                .limit(1)
                .fetchOne();
    }

    @Override
    public Result<ConnectorMeterValueRecord> getMeterValues(int transactionPk) {
        Condition unitCondition = CONNECTOR_METER_VALUE.UNIT.isNull()
                .or(CONNECTOR_METER_VALUE.UNIT.in("", UnitOfMeasure.WH.value(), UnitOfMeasure.K_WH.value()));

        return ctx.selectFrom(CONNECTOR_METER_VALUE)
                .where(CONNECTOR_METER_VALUE.TRANSACTION_PK.eq(transactionPk))
                .and(unitCondition)
                .fetch();
    }

    @Override
    public Result<ConnectorMeterValueRecord> getMeterValues(String chargeBoxId, int connectorId, LocalDateTime startTimestamp, LocalDateTime stopTimestamp) {
        Condition unitCondition = CONNECTOR_METER_VALUE.UNIT.isNull()
                .or(CONNECTOR_METER_VALUE.UNIT.in("", UnitOfMeasure.WH.value(), UnitOfMeasure.K_WH.value()));

        return ctx.selectFrom(CONNECTOR_METER_VALUE)
                .where(CONNECTOR_METER_VALUE.CONNECTOR_PK.eq(ctx.select(CONNECTOR.CONNECTOR_PK)
                        .from(CONNECTOR)
                        .where(CONNECTOR.CHARGE_BOX_ID.equal(chargeBoxId))
                        .and(CONNECTOR.CONNECTOR_ID.equal(connectorId))))
                .and(CONNECTOR_METER_VALUE.VALUE_TIMESTAMP.between(startTimestamp, stopTimestamp))
                .and(unitCondition)
                .fetch();
    }

    @SuppressWarnings("unchecked")
    private
    SelectQuery<Record9<Integer, String, Integer, String, LocalDateTime, String, LocalDateTime, String, String>>
    getInternalCSV(TransactionQueryForm form) {

        SelectQuery selectQuery = ctx.selectQuery();
        selectQuery.addFrom(TRANSACTION);
        selectQuery.addJoin(CONNECTOR, TRANSACTION.CONNECTOR_PK.eq(CONNECTOR.CONNECTOR_PK));
        selectQuery.addSelect(
                TRANSACTION.TRANSACTION_PK,
                CONNECTOR.CHARGE_BOX_ID,
                CONNECTOR.CONNECTOR_ID,
                TRANSACTION.ID_TAG,
                TRANSACTION.START_TIMESTAMP,
                TRANSACTION.START_VALUE,
                TRANSACTION.STOP_TIMESTAMP,
                TRANSACTION.STOP_VALUE,
                TRANSACTION.STOP_REASON
        );

        return addConditions(selectQuery, form);
    }

    @SuppressWarnings("unchecked")
    private
    SelectQuery<Record12<Integer, String, Integer, String, LocalDateTime, String, LocalDateTime, String, String, Integer, Integer, TransactionStopEventActor>>
    getInternal(TransactionQueryForm form) {

        SelectQuery selectQuery = ctx.selectQuery();
        selectQuery.addFrom(TRANSACTION);
        selectQuery.addJoin(CONNECTOR, TRANSACTION.CONNECTOR_PK.eq(CONNECTOR.CONNECTOR_PK));
        selectQuery.addJoin(CHARGE_BOX, CHARGE_BOX.CHARGE_BOX_ID.eq(CONNECTOR.CHARGE_BOX_ID));
        selectQuery.addJoin(OCPP_TAG, OCPP_TAG.ID_TAG.eq(TRANSACTION.ID_TAG));
        selectQuery.addSelect(
                TRANSACTION.TRANSACTION_PK,
                CONNECTOR.CHARGE_BOX_ID,
                CONNECTOR.CONNECTOR_ID,
                TRANSACTION.ID_TAG,
                TRANSACTION.START_TIMESTAMP,
                TRANSACTION.START_VALUE,
                TRANSACTION.STOP_TIMESTAMP,
                TRANSACTION.STOP_VALUE,
                TRANSACTION.STOP_REASON,
                CHARGE_BOX.CHARGE_BOX_PK,
                OCPP_TAG.OCPP_TAG_PK,
                TRANSACTION.STOP_EVENT_ACTOR
        );

        return addConditions(selectQuery, form);
    }

    @SuppressWarnings("unchecked")
    private SelectQuery addConditions(SelectQuery selectQuery, TransactionQueryForm form) {
        if (form.isTransactionPkSet()) {
            selectQuery.addConditions(TRANSACTION.TRANSACTION_PK.eq(form.getTransactionPk()));
        }

        if (form.isChargeBoxIdSet()) {
            selectQuery.addConditions(CONNECTOR.CHARGE_BOX_ID.eq(form.getChargeBoxId()));
        }

        if (form.isOcppIdTagSet()) {
            selectQuery.addConditions(TRANSACTION.ID_TAG.eq(form.getOcppIdTag()));
        }

        if (form.getType() == TransactionQueryForm.QueryType.ACTIVE) {
            selectQuery.addConditions(TRANSACTION.STOP_TIMESTAMP.isNull());

        } else if (form.getType() == TransactionQueryForm.QueryType.STOPPED) {
            selectQuery.addConditions(TRANSACTION.STOP_TIMESTAMP.isNotNull());
        }

        processType(selectQuery, form);

        selectQuery.addOrderBy(TRANSACTION.TRANSACTION_PK.desc());

        return selectQuery;
    }

    private void processType(SelectQuery selectQuery, TransactionQueryForm form) {
        switch (form.getPeriodType()) {
            case TODAY:
                selectQuery.addConditions(
                        date(TRANSACTION.START_TIMESTAMP).eq(LocalDate.now())
                );
                break;

            case LAST_10:
            case LAST_30:
            case LAST_90:
                LocalDate now = LocalDate.now();
                selectQuery.addConditions(
                        date(TRANSACTION.START_TIMESTAMP).between(
                                now.minusDays(form.getPeriodType().getInterval()),
                                now
                        )
                );
                break;

            case ALL:
                break;

            case FROM_TO:
                LocalDateTime from = form.getFrom();
                LocalDateTime to = form.getTo();

                if (form.getType() == TransactionQueryForm.QueryType.ACTIVE) {
                    selectQuery.addConditions(TRANSACTION.START_TIMESTAMP.between(from, to));

                } else if (form.getType() == TransactionQueryForm.QueryType.STOPPED) {
                    selectQuery.addConditions(TRANSACTION.STOP_TIMESTAMP.between(from, to));
                }

                break;

            default:
                throw new SteveException("Unknown enum type");
        }
    }
}
