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
 * along with this program.  If an, see <https://www.gnu.org/licenses/>.
 */
package de.rwth.idsg.steve.repository.impl;

import de.rwth.idsg.steve.repository.AddressRepository;
import de.rwth.idsg.steve.repository.AddressRepositoryV1;
import de.rwth.idsg.steve.web.dto.Address;
import jooq.steve.db.tables.records.AddressRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.SelectConditionStep;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;

@Repository
@RequiredArgsConstructor
public class AddressRepositoryV1Impl implements AddressRepositoryV1 {

    private final AddressRepository addressRepository;
    private final DSLContext ctx;

    @Override
    @Nullable
    public AddressRecord get(Integer addressPk) {
        return addressRepository.get(addressPk);
    }

    @Override
    @Nullable
    public Integer updateOrInsert(Address address) {
        if (address.isEmpty()) {
            return null;
        } else if (address.getAddressPk() == null) {
            return addressRepository.insert(address);
        } else {
            addressRepository.update(address);
            return address.getAddressPk();
        }
    }

    @Override
    public void delete(SelectConditionStep<Record1<Integer>> addressPkSelect) {
        // This method is tricky to refactor, as it uses a sub-select.
        // For now, we'll leave it as is, and refactor it in the service layer.
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
