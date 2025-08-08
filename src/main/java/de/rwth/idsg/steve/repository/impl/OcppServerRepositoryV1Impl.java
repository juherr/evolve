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
import de.rwth.idsg.steve.repository.OcppServerRepository;
import de.rwth.idsg.steve.repository.OcppServerRepositoryV1;
import de.rwth.idsg.steve.repository.dto.InsertConnectorStatusParams;
import de.rwth.idsg.steve.repository.dto.InsertTransactionParams;
import de.rwth.idsg.steve.repository.dto.UpdateChargeboxParams;
import de.rwth.idsg.steve.repository.dto.UpdateTransactionParams;
import lombok.RequiredArgsConstructor;
import ocpp.cs._2015._10.MeterValue;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OcppServerRepositoryV1Impl implements OcppServerRepositoryV1 {

    private final OcppServerRepository ocppServerRepository;

    @Override
    public void updateChargebox(UpdateChargeboxParams p) {
        ocppServerRepository.updateChargebox(p);
    }

    @Override
    public void updateOcppProtocol(String chargeBoxIdentity, OcppProtocol protocol) {
        ocppServerRepository.updateOcppProtocol(chargeBoxIdentity, protocol);
    }

    @Override
    public void updateEndpointAddress(String chargeBoxIdentity, String endpointAddress) {
        ocppServerRepository.updateEndpointAddress(chargeBoxIdentity, endpointAddress);
    }

    @Override
    public void updateChargeboxFirmwareStatus(String chargeBoxIdentity, String firmwareStatus) {
        ocppServerRepository.updateChargeboxFirmwareStatus(chargeBoxIdentity, firmwareStatus);
    }

    @Override
    public void updateChargeboxDiagnosticsStatus(String chargeBoxIdentity, String status) {
        ocppServerRepository.updateChargeboxDiagnosticsStatus(chargeBoxIdentity, status);
    }

    @Override
    public void updateChargeboxHeartbeat(String chargeBoxIdentity, LocalDateTime ts) {
        ocppServerRepository.updateChargeboxHeartbeat(chargeBoxIdentity, ts);
    }

    @Override
    public void insertConnectorStatus(InsertConnectorStatusParams p) {
        ocppServerRepository.insertConnectorStatus(p);
    }

    @Override
    public void insertMeterValues(String chargeBoxIdentity, List<MeterValue> list, int connectorId, Integer transactionId) {
        ocppServerRepository.insertMeterValues(chargeBoxIdentity, list, connectorId, transactionId);
    }

    @Override
    public void insertMeterValues(String chargeBoxIdentity, List<MeterValue> list, int transactionId) {
        ocppServerRepository.insertMeterValues(chargeBoxIdentity, list, transactionId);
    }

    @Override
    public int insertTransaction(InsertTransactionParams p) {
        return ocppServerRepository.insertTransaction(p);
    }

    @Override
    public void updateTransaction(UpdateTransactionParams p) {
        ocppServerRepository.updateTransaction(p);
    }
}
