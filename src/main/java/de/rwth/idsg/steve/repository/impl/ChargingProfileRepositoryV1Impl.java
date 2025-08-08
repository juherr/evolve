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
import de.rwth.idsg.steve.repository.ChargingProfileRepositoryV1;
import de.rwth.idsg.steve.repository.dto.ChargingProfile;
import de.rwth.idsg.steve.repository.dto.ChargingProfileAssignment;
import de.rwth.idsg.steve.service.ChargingProfileService;
import de.rwth.idsg.steve.web.dto.ChargingProfileAssignmentQueryForm;
import de.rwth.idsg.steve.web.dto.ChargingProfileForm;
import de.rwth.idsg.steve.web.dto.ChargingProfileQueryForm;
import lombok.RequiredArgsConstructor;
import ocpp.cp._2015._10.ChargingProfilePurposeType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChargingProfileRepositoryV1Impl implements ChargingProfileRepositoryV1 {

    private final ChargingProfileRepository chargingProfileRepository;
    private final ChargingProfileService chargingProfileService;

    @Override
    public void setProfile(int chargingProfilePk, String chargeBoxId, int connectorId) {
        chargingProfileRepository.setProfile(chargingProfilePk, chargeBoxId, connectorId);
    }

    @Override
    public void clearProfile(int chargingProfilePk, String chargeBoxId) {
        chargingProfileRepository.clearProfile(chargingProfilePk, chargeBoxId);
    }

    @Override
    public void clearProfile(@NotNull String chargeBoxId,
                             @Nullable Integer connectorId,
                             @Nullable ChargingProfilePurposeType purpose,
                             @Nullable Integer stackLevel) {
        chargingProfileRepository.clearProfile(chargeBoxId, connectorId, purpose, stackLevel);
    }

    @Override
    public List<ChargingProfileAssignment> getAssignments(ChargingProfileAssignmentQueryForm query) {
        return chargingProfileService.getAssignments(query);
    }

    @Override
    public List<ChargingProfile.BasicInfo> getBasicInfo() {
        return chargingProfileRepository.getBasicInfo();
    }

    @Override
    public List<ChargingProfile.Overview> getOverview(ChargingProfileQueryForm form) {
        return chargingProfileService.getOverview(form);
    }

    @Override
    public ChargingProfile.Details getDetails(int chargingProfilePk) {
        return chargingProfileService.getDetails(chargingProfilePk);
    }

    @Override
    public int add(ChargingProfileForm form) {
        return chargingProfileService.add(form);
    }

    @Override
    public void update(ChargingProfileForm form) {
        chargingProfileService.update(form);
    }

    @Override
    public void delete(int chargingProfilePk) {
        chargingProfileService.delete(chargingProfilePk);
    }
}
