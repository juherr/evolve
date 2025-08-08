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

import de.rwth.idsg.steve.repository.WebUserRepository;
import de.rwth.idsg.steve.repository.WebUserRepositoryV1;
import jooq.steve.db.tables.records.WebUserRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WebUserRepositoryV1Impl implements WebUserRepositoryV1 {

    private final WebUserRepository webUserRepository;

    @Override
    public void createUser(WebUserRecord user) {
        webUserRepository.createUser(user);
    }

    @Override
    public void updateUser(WebUserRecord user) {
        webUserRepository.updateUser(user);
    }

    @Override
    public void deleteUser(String username) {
        webUserRepository.deleteUser(username);
    }

    @Override
    public void deleteUser(int webUserPk) {
        webUserRepository.deleteUser(webUserPk);
    }

    @Override
    public void changeStatusOfUser(String username, boolean enabled) {
        webUserRepository.changeStatusOfUser(username, enabled);
    }

    @Override
    public Integer getUserCountWithAuthority(String authority) {
        return webUserRepository.getUserCountWithAuthority(authority);
    }

    @Override
    public void changePassword(String username, String newPassword) {
        webUserRepository.changePassword(username, newPassword);
    }

    @Override
    public boolean userExists(String username) {
        return webUserRepository.userExists(username);
    }

    @Override
    public WebUserRecord loadUserByUsername(String username) {
        return webUserRepository.loadUserByUsername(username);
    }
}
