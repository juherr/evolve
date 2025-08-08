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

import de.rwth.idsg.steve.repository.TransactionRepository;
import de.rwth.idsg.steve.repository.TransactionRepositoryV1;
import de.rwth.idsg.steve.repository.dto.Transaction;
import de.rwth.idsg.steve.repository.dto.TransactionDetails;
import de.rwth.idsg.steve.service.TransactionService;
import de.rwth.idsg.steve.utils.DateTimeUtils;
import de.rwth.idsg.steve.web.dto.TransactionQueryForm;
import jooq.steve.db.enums.TransactionStopEventActor;
import lombok.RequiredArgsConstructor;
import org.jooq.Record12;
import org.jooq.RecordMapper;
import org.jooq.Result;
import org.springframework.stereotype.Repository;

import java.io.Writer;
import java.time.LocalDateTime;
import java.util.List;

import static de.rwth.idsg.steve.utils.DateTimeUtils.toOffsetDateTime;

@Repository
@RequiredArgsConstructor
public class TransactionRepositoryV1Impl implements TransactionRepositoryV1 {

    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;

    @Override
    public List<Transaction> getTransactions(TransactionQueryForm form) {
        Result<Record12<Integer, String, Integer, String, LocalDateTime, String, LocalDateTime, String, String, Integer, Integer, TransactionStopEventActor>> result =
                (Result<Record12<Integer, String, Integer, String, LocalDateTime, String, LocalDateTime, String, String, Integer, Integer, TransactionStopEventActor>>)
                        transactionRepository.getTransactions(form);

        return result.map(new TransactionMapper());
    }

    @Override
    public void writeTransactionsCSV(TransactionQueryForm form, Writer writer) {
        transactionRepository.writeTransactionsCSV(form, writer);
    }

    @Override
    public List<Integer> getActiveTransactionIds(String chargeBoxId) {
        return transactionRepository.getActiveTransactionIds(chargeBoxId);
    }

    @Override
    public TransactionDetails getDetails(int transactionPk) {
        return transactionService.getDetails(transactionPk);
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
