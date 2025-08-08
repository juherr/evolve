package de.rwth.idsg.steve.service;

import de.rwth.idsg.steve.repository.UserRepository;
import de.rwth.idsg.steve.repository.dto.User;
import de.rwth.idsg.steve.web.dto.UserForm;
import de.rwth.idsg.steve.web.dto.UserQueryForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<User.Overview> getUsers(UserQueryForm form) {
        return userRepository.getOverview(form);
    }

    public User.Details getUser(int userPk) {
        return userRepository.getDetails(userPk);
    }

    public void addUser(UserForm form) {
        userRepository.add(form);
    }

    public void updateUser(UserForm form) {
        userRepository.update(form);
    }

    public void deleteUser(int userPk) {
        userRepository.delete(userPk);
    }
}
