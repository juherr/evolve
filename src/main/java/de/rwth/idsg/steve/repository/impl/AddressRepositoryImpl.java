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
import de.rwth.idsg.steve.repository.AddressRepository;
import de.rwth.idsg.steve.web.dto.Address;
import jooq.steve.db.tables.records.AddressRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.springframework.stereotype.Repository;

import static jooq.steve.db.tables.Address.ADDRESS;

@Repository
@RequiredArgsConstructor
public class AddressRepositoryImpl implements AddressRepository {

    private final DSLContext ctx;

    @Override
    public AddressRecord get(Integer addressPk) {
        if (addressPk == null) {
            return null;
        }

        return ctx.selectFrom(ADDRESS)
                .where(ADDRESS.ADDRESS_PK.equal(addressPk))
                .fetchOne();
    }

    @Override
    public Integer insert(Address ad) {
        try {
            return ctx.insertInto(ADDRESS)
                    .set(ADDRESS.STREET, ad.getStreet())
                    .set(ADDRESS.HOUSE_NUMBER, ad.getHouseNumber())
                    .set(ADDRESS.ZIP_CODE, ad.getZipCode())
                    .set(ADDRESS.CITY, ad.getCity())
                    .set(ADDRESS.COUNTRY, ad.getCountryAlpha2OrNull())
                    .returning(ADDRESS.ADDRESS_PK)
                    .fetchOne()
                    .getAddressPk();
        } catch (DataAccessException e) {
            throw new SteveException("Failed to insert the address");
        }
    }

    @Override
    public void update(Address ad) {
        int count = ctx.update(ADDRESS)
                .set(ADDRESS.STREET, ad.getStreet())
                .set(ADDRESS.HOUSE_NUMBER, ad.getHouseNumber())
                .set(ADDRESS.ZIP_CODE, ad.getZipCode())
                .set(ADDRESS.CITY, ad.getCity())
                .set(ADDRESS.COUNTRY, ad.getCountryAlpha2OrNull())
                .where(ADDRESS.ADDRESS_PK.eq(ad.getAddressPk()))
                .execute();

        if (count != 1) {
            throw new SteveException("Failed to update the address");
        }
    }

    @Override
    public void delete(int addressPk) {
        ctx.delete(ADDRESS)
                .where(ADDRESS.ADDRESS_PK.eq(addressPk))
                .execute();
    }
}
