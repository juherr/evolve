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

import de.rwth.idsg.steve.repository.UserRepositoryV1;
import de.rwth.idsg.steve.repository.dto.User;
import de.rwth.idsg.steve.service.UserService;
import de.rwth.idsg.steve.web.dto.UserForm;
import de.rwth.idsg.steve.web.dto.UserQueryForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserRepositoryV1Impl implements UserRepositoryV1 {

    private final UserService userService;

    @Override
    public List<User.Overview> getOverview(UserQueryForm form) {
        return userService.getOverview(form);
    }

    @Override
    public User.Details getDetails(int userPk) {
        return userService.getDetails(userPk);
    }

    @Override
    public void add(UserForm form) {
        userService.add(form);
    }

    @Override
    public void update(UserForm form) {
        userService.update(form);
    }

    @Override
    public void delete(int userPk) {
        userService.delete(userPk);
    }
}
