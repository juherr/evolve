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

import de.rwth.idsg.steve.repository.OcppTagRepository;
import de.rwth.idsg.steve.repository.OcppTagRepositoryV1;
import de.rwth.idsg.steve.repository.dto.OcppTag;
import de.rwth.idsg.steve.service.OcppTagService;
import de.rwth.idsg.steve.web.dto.OcppTagForm;
import de.rwth.idsg.steve.web.dto.OcppTagQueryForm;
import jooq.steve.db.tables.records.OcppTagActivityRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.Result;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OcppTagRepositoryV1Impl implements OcppTagRepositoryV1 {

    private final OcppTagRepository ocppTagRepository;
    private final OcppTagService ocppTagService;

    @Override
    public List<OcppTag.OcppTagOverview> getOverview(OcppTagQueryForm form) {
        return ocppTagService.getOverview(form);
    }

    @Override
    public Result<OcppTagActivityRecord> getRecords() {
        return ocppTagRepository.getRecords();
    }

    @Override
    public Result<OcppTagActivityRecord> getRecords(List<String> idTagList) {
        return ocppTagRepository.getRecords(idTagList);
    }

    @Override
    public OcppTagActivityRecord getRecord(String idTag) {
        return ocppTagRepository.getRecord(idTag);
    }

    @Override
    public OcppTagActivityRecord getRecord(int ocppTagPk) {
        return ocppTagRepository.getRecord(ocppTagPk);
    }

    @Override
    public List<String> getIdTags() {
        return ocppTagRepository.getIdTags();
    }

    @Override
    public List<String> getActiveIdTags() {
        return ocppTagRepository.getActiveIdTags();
    }

    @Override
    public List<String> getParentIdTags() {
        return ocppTagRepository.getParentIdTags();
    }

    @Override
    public String getParentIdtag(String idTag) {
        return ocppTagRepository.getParentIdtag(idTag);
    }

    @Override
    public void addOcppTagList(List<String> idTagList) {
        ocppTagRepository.addOcppTagList(idTagList);
    }

    @Override
    public int addOcppTag(OcppTagForm u) {
        return ocppTagRepository.addOcppTag(u);
    }

    @Override
    public void updateOcppTag(OcppTagForm u) {
        ocppTagRepository.updateOcppTag(u);
    }

    @Override
    public void deleteOcppTag(int ocppTagPk) {
        ocppTagRepository.deleteOcppTag(ocppTagPk);
    }
}
