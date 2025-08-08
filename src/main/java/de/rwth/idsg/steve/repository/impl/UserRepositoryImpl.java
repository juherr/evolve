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
import de.rwth.idsg.steve.repository.UserRepository;
import de.rwth.idsg.steve.web.dto.UserForm;
import jooq.steve.db.tables.records.UserRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import static jooq.steve.db.tables.OcppTag.OCPP_TAG;
import static jooq.steve.db.tables.User.USER;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final DSLContext ctx;

    @Override
    public UserRecord getUser(int userPk) {
        return ctx.selectFrom(USER)
                .where(USER.USER_PK.equal(userPk))
                .fetchOne();
    }

    @Override
    public String getOcppIdTag(int ocppTagPk) {
        return ctx.select(OCPP_TAG.ID_TAG)
                .from(OCPP_TAG)
                .where(OCPP_TAG.OCPP_TAG_PK.eq(ocppTagPk))
                .fetchOne()
                .value1();
    }

    @Override
    public void add(UserForm form, Integer addressPk, Integer ocppTagPk) {
        int count = ctx.insertInto(USER)
                .set(USER.FIRST_NAME, form.getFirstName())
                .set(USER.LAST_NAME, form.getLastName())
                .set(USER.BIRTH_DAY, form.getBirthDay())
                .set(USER.SEX, form.getSex().getDatabaseValue())
                .set(USER.PHONE, form.getPhone())
                .set(USER.E_MAIL, form.getEMail())
                .set(USER.NOTE, form.getNote())
                .set(USER.ADDRESS_PK, addressPk)
                .set(USER.OCPP_TAG_PK, ocppTagPk)
                .execute();

        if (count != 1) {
            throw new SteveException("Failed to insert the user");
        }
    }

    @Override
    public void update(UserForm form, Integer addressPk, Integer ocppTagPk) {
        ctx.update(USER)
                .set(USER.FIRST_NAME, form.getFirstName())
                .set(USER.LAST_NAME, form.getLastName())
                .set(USER.BIRTH_DAY, form.getBirthDay())
                .set(USER.SEX, form.getSex().getDatabaseValue())
                .set(USER.PHONE, form.getPhone())
                .set(USER.E_MAIL, form.getEMail())
                .set(USER.NOTE, form.getNote())
                .set(USER.ADDRESS_PK, addressPk)
                .set(USER.OCPP_TAG_PK, ocppTagPk)
                .where(USER.USER_PK.eq(form.getUserPk()))
                .execute();
    }

    @Override
    public void delete(int userPk) {
        ctx.delete(USER)
                .where(USER.USER_PK.equal(userPk))
                .execute();
    }

    @Override
    public int getAddressId(int userPk) {
        return ctx.select(USER.ADDRESS_PK)
                .from(USER)
                .where(USER.USER_PK.eq(userPk))
                .fetchOne()
                .value1();
    }

    @Override
    public int getOcppTagPk(String ocppIdTag) {
        return ctx.select(OCPP_TAG.OCPP_TAG_PK)
                .from(OCPP_TAG)
                .where(OCPP_TAG.ID_TAG.eq(ocppIdTag))
                .fetchOne()
                .value1();
    }
}
