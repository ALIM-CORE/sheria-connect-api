package co.tz.sheriaconnectapi.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import co.tz.sheriaconnectapi.model.DTOs.UserDTO;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.services.UserServices.CreateUserService;
import co.tz.sheriaconnectapi.utils.StandardResponse;

@RestController
public class UserController {

    private final CreateUserService createUserService;

    public UserController(CreateUserService createUserService) {
        this.createUserService = createUserService;
    }

    @PostMapping("/user")
    public ResponseEntity<StandardResponse<UserDTO>> createUser(
            @RequestBody User user,
            HttpServletRequest request
    ) {

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            user.setPassword(user.getEmail()); // default password
        }

        return createUserService.execute(user, request);
    }
}
