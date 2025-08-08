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
import de.rwth.idsg.steve.repository.AddressRepository;
import de.rwth.idsg.steve.repository.ChargePointRepository;
import de.rwth.idsg.steve.repository.dto.ChargePoint;
import de.rwth.idsg.steve.repository.dto.ConnectorStatus;
import de.rwth.idsg.steve.utils.DateTimeUtils;
import de.rwth.idsg.steve.web.dto.Address;
import de.rwth.idsg.steve.web.dto.ChargePointForm;
import de.rwth.idsg.steve.web.dto.ChargePointQueryForm;
import de.rwth.idsg.steve.web.dto.ConnectorStatusForm;
import jooq.steve.db.tables.records.AddressRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record5;
import org.jooq.Result;
import org.jooq.SelectQuery;
import org.jooq.exception.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static de.rwth.idsg.steve.utils.CustomDSL.date;
import static de.rwth.idsg.steve.utils.CustomDSL.includes;
import static jooq.steve.db.tables.ChargeBox.CHARGE_BOX;

@Service
@RequiredArgsConstructor
public class ChargePointService {

    private final DSLContext ctx;
    private final ChargePointRepository chargePointRepository;
    private final AddressRepository addressRepository;

    public List<ChargePoint.Overview> getOverview(ChargePointQueryForm form) {
        return getOverviewInternal(form)
                .map(r -> ChargePoint.Overview.builder()
                        .chargeBoxPk(r.value1())
                        .chargeBoxId(r.value2())
                        .description(r.value3())
                        .ocppProtocol(r.value4())
                        .lastHeartbeatTimestampDT(r.value5())
                        .lastHeartbeatTimestamp(DateTimeUtils.humanize(r.value5()))
                        .build()
                );
    }

    @SuppressWarnings("unchecked")
    private Result<Record5<Integer, String, String, String, LocalDateTime>> getOverviewInternal(ChargePointQueryForm form) {
        SelectQuery selectQuery = ctx.selectQuery();
        selectQuery.addFrom(CHARGE_BOX);
        selectQuery.addSelect(
                CHARGE_BOX.CHARGE_BOX_PK,
                CHARGE_BOX.CHARGE_BOX_ID,
                CHARGE_BOX.DESCRIPTION,
                CHARGE_BOX.OCPP_PROTOCOL,
                CHARGE_BOX.LAST_HEARTBEAT_TIMESTAMP
        );

        if (form.isSetOcppVersion()) {
            selectQuery.addConditions(CHARGE_BOX.OCPP_PROTOCOL.like(form.getOcppVersion().getValue() + "_"));
        }

        if (form.isSetDescription()) {
            selectQuery.addConditions(includes(CHARGE_BOX.DESCRIPTION, form.getDescription()));
        }

        if (form.isSetChargeBoxId()) {
            selectQuery.addConditions(includes(CHARGE_BOX.CHARGE_BOX_ID, form.getChargeBoxId()));
        }

        if (form.isSetNote()) {
            selectQuery.addConditions(includes(CHARGE_BOX.NOTE, form.getNote()));
        }

        switch (form.getHeartbeatPeriod()) {
            case ALL:
                break;

            case TODAY:
                selectQuery.addConditions(
                        date(CHARGE_BOX.LAST_HEARTBEAT_TIMESTAMP).eq(LocalDate.now())
                );
                break;

            case YESTERDAY:
                selectQuery.addConditions(
                        date(CHARGE_BOX.LAST_HEARTBEAT_TIMESTAMP).eq(LocalDate.now().minusDays(1))
                );
                break;

            case EARLIER:
                selectQuery.addConditions(
                        date(CHARGE_BOX.LAST_HEARTBEAT_TIMESTAMP).lessThan(LocalDate.now().minusDays(1))
                );
                break;

            default:
                throw new SteveException("Unknown enum type");
        }

        selectQuery.addOrderBy(CHARGE_BOX.CHARGE_BOX_PK.asc());

        return selectQuery.fetch();
    }

    public ChargePoint.Details getDetails(int chargeBoxPk) {
        ChargePoint.Details details = chargePointRepository.getDetails(chargeBoxPk)
                .orElseThrow(() -> new SteveException("Charge point not found"));

        AddressRecord ar = addressRepository.get(details.getAddressPk());
        details.setAddress(ar);

        return details;
    }

    public List<ConnectorStatus> getChargePointConnectorStatus(ConnectorStatusForm form) {
        if (form == null) {
            return chargePointRepository.getChargePointConnectorStatus(null, null);
        } else {
            return chargePointRepository.getChargePointConnectorStatus(form.getChargeBoxId(), form.getStatus());
        }
    }

    public int addChargePoint(ChargePointForm form) {
        return ctx.transactionResult(configuration -> {
            try {
                Integer addressId = updateOrInsertAddress(form.getAddress());
                ChargePoint.Details details = new ChargePoint.Details(form);
                return chargePointRepository.addChargePoint(details, addressId);

            } catch (DataAccessException e) {
                throw new SteveException("Failed to add the charge point with chargeBoxId '%s'",
                        form.getChargeBoxId(), e);
            }
        });
    }

    public void updateChargePoint(ChargePointForm form) {
        ctx.transaction(configuration -> {
            try {
                Integer addressId = updateOrInsertAddress(form.getAddress());
                ChargePoint.Details details = new ChargePoint.Details(form);
                chargePointRepository.updateChargePoint(details, addressId);

            } catch (DataAccessException e) {
                throw new SteveException("Failed to update the charge point with chargeBoxId '%s'",
                        form.getChargeBoxId(), e);
            }
        });
    }

    public void deleteChargePoint(int chargeBoxPk) {
        ctx.transaction(configuration -> {
            try {
                int addressId = chargePointRepository.selectAddressId(chargeBoxPk);
                addressRepository.delete(addressId);
                chargePointRepository.deleteChargePoint(chargeBoxPk);

            } catch (DataAccessException e) {
                throw new SteveException("Failed to delete the charge point", e);
            }
        });
    }

    private Integer updateOrInsertAddress(Address address) {
        if (address.isEmpty()) {
            return null;
        } else if (address.getAddressPk() == null) {
            return addressRepository.insert(address);
        } else {
            addressRepository.update(address);
            return address.getAddressPk();
        }
    }
}
