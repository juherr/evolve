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
import de.rwth.idsg.steve.repository.TransactionRepository;
import de.rwth.idsg.steve.repository.dto.Transaction;
import de.rwth.idsg.steve.repository.dto.TransactionDetails;
import de.rwth.idsg.steve.utils.DateTimeUtils;
import de.rwth.idsg.steve.utils.TransactionStopServiceHelper;
import jooq.steve.db.enums.TransactionStopEventActor;
import jooq.steve.db.tables.records.ConnectorMeterValueRecord;
import jooq.steve.db.tables.records.TransactionStartRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.Record;
import org.jooq.Record12;
import org.jooq.RecordMapper;
import org.jooq.Result;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static de.rwth.idsg.steve.utils.DateTimeUtils.toOffsetDateTime;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionDetails getDetails(int transactionPk) {
        Result<? extends Record> transactionResult = transactionRepository.getTransaction(transactionPk);

        if (transactionResult.isEmpty()) {
            throw new SteveException("There is no transaction with id '%s'", transactionPk);
        }

        Record12<Integer, String, Integer, String, LocalDateTime, String, LocalDateTime, String, String, Integer, Integer, TransactionStopEventActor>
                transaction = (Record12<Integer, String, Integer, String, LocalDateTime, String, LocalDateTime, String, String, Integer, Integer, TransactionStopEventActor>) transactionResult.get(0);

        LocalDateTime startTimestamp = transaction.value5();
        LocalDateTime stopTimestamp = transaction.value7();
        String stopValue = transaction.value8();
        String chargeBoxId = transaction.value2();
        int connectorId = transaction.value3();

        TransactionStartRecord nextTx = null;
        Result<ConnectorMeterValueRecord> meterValuesResult;

        if (stopTimestamp == null && stopValue == null) {
            nextTx = transactionRepository.getNextTransaction(chargeBoxId, connectorId, startTimestamp);

            if (nextTx == null) {
                meterValuesResult = transactionRepository.getMeterValues(transactionPk);
            } else {
                meterValuesResult = transactionRepository.getMeterValues(chargeBoxId, connectorId, startTimestamp, nextTx.getStartTimestamp());
            }
        } else {
            meterValuesResult = transactionRepository.getMeterValues(chargeBoxId, connectorId, startTimestamp, stopTimestamp);
        }

        List<TransactionDetails.MeterValues> values = meterValuesResult.stream()
                .map(r -> TransactionDetails.MeterValues.builder()
                        .valueTimestamp(toOffsetDateTime(r.getValueTimestamp()))
                        .value(r.getValue())
                        .readingContext(r.getReadingContext())
                        .format(r.getFormat())
                        .measurand(r.getMeasurand())
                        .location(r.getLocation())
                        .unit(r.getUnit())
                        .phase(r.getPhase())
                        .build())
                .filter(TransactionStopServiceHelper::isEnergyValue)
                .toList();

        return new TransactionDetails(new TransactionMapper().map(transaction), values, nextTx);
    }

    private static class TransactionMapper implements RecordMapper<Record12<Integer, String, Integer, String, LocalDateTime, String, LocalDateTime, String, String, Integer, Integer, TransactionStopEventActor>, Transaction> {
        @Override
        public Transaction map(Record12<Integer, String, Integer, String, LocalDateTime, String, LocalDateTime, String, String, Integer, Integer, TransactionStopEventActor> r) {
            return Transaction.builder()
                    .id(r.value1())
                    .chargeBoxId(r.value2())
                    .connectorId(r.value3())
                    .ocppIdTag(r.value4())
                    .startTimestamp(toOffsetDateTime(r.value5()))
                    .startTimestampFormatted(DateTimeUtils.humanize(r.value5()))
                    .startValue(r.value6())
                    .stopTimestamp(toOffsetDateTime(r.value7()))
                    .stopTimestampFormatted(DateTimeUtils.humanize(r.value7()))
                    .stopValue(r.value8())
                    .stopReason(r.value9())
                    .chargeBoxPk(r.value10())
                    .ocppTagPk(r.value11())
                    .stopEventActor(r.value12())
                    .build();
        }
    }
}
