package de.rwth.idsg.steve.web.api;

import de.rwth.idsg.steve.repository.dto.User;
import de.rwth.idsg.steve.service.UserService;
import de.rwth.idsg.steve.web.dto.UserForm;
import de.rwth.idsg.steve.web.dto.UserQueryForm;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(path = "/api/users", produces = "application/json;charset=UTF-8")
@RequiredArgsConstructor
public class UserRestController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<User.Overview>> getUsers(UserQueryForm form) {
        List<User.Overview> users = userService.getUsers(form);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User.Details> getUser(@PathVariable int id) {
        User.Details user = userService.getUser(id);
        return ResponseEntity.ok(user);
    }

    @PostMapping
    public ResponseEntity<Void> addUser(@Valid @RequestBody UserForm form) {
        userService.addUser(form);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateUser(@PathVariable int id, @Valid @RequestBody UserForm form) {
        form.setUserPk(id);
        userService.updateUser(form);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable int id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }
}
