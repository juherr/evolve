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
import de.rwth.idsg.steve.repository.ChargePointRepositoryV1;
import de.rwth.idsg.steve.repository.dto.ChargePoint;
import de.rwth.idsg.steve.repository.dto.ChargePointSelect;
import de.rwth.idsg.steve.repository.dto.ConnectorStatus;
import de.rwth.idsg.steve.service.ChargePointService;
import de.rwth.idsg.steve.web.dto.ChargePointForm;
import de.rwth.idsg.steve.web.dto.ChargePointQueryForm;
import de.rwth.idsg.steve.web.dto.ConnectorStatusForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ChargePointRepositoryV1Impl implements ChargePointRepositoryV1 {

    private final ChargePointRepository chargePointRepository;
    private final ChargePointService chargePointService;

    @Override
    public Optional<String> getRegistrationStatus(String chargeBoxId) {
        return chargePointRepository.getRegistrationStatus(chargeBoxId);
    }

    @Override
    public List<ChargePointSelect> getChargePointSelect(OcppProtocol protocol, List<String> inStatusFilter, List<String> chargeBoxIdFilter) {
        return chargePointRepository.getChargePointSelect(protocol, inStatusFilter, chargeBoxIdFilter);
    }

    @Override
    public List<String> getChargeBoxIds() {
        return chargePointRepository.getChargeBoxIds();
    }

    @Override
    public Map<String, Integer> getChargeBoxIdPkPair(List<String> chargeBoxIdList) {
        return chargePointRepository.getChargeBoxIdPkPair(chargeBoxIdList);
    }

    @Override
    public List<ChargePoint.Overview> getOverview(ChargePointQueryForm form) {
        return chargePointService.getOverview(form);
    }

    @Override
    public ChargePoint.Details getDetails(int chargeBoxPk) {
        return chargePointService.getDetails(chargeBoxPk);
    }

    @Override
    public List<ConnectorStatus> getChargePointConnectorStatus(ConnectorStatusForm form) {
        return chargePointService.getChargePointConnectorStatus(form);
    }

    @Override
    public List<Integer> getNonZeroConnectorIds(String chargeBoxId) {
        return chargePointRepository.getNonZeroConnectorIds(chargeBoxId);
    }

    @Override
    public void addChargePointList(List<String> chargeBoxIdList) {
        chargePointRepository.addChargePointList(chargeBoxIdList);
    }

    @Override
    public int addChargePoint(ChargePointForm form) {
        return chargePointService.addChargePoint(form);
    }

    @Override
    public void updateChargePoint(ChargePointForm form) {
        chargePointService.updateChargePoint(form);
    }

    @Override
    public void deleteChargePoint(int chargeBoxPk) {
        chargePointService.deleteChargePoint(chargeBoxPk);
    }
}
