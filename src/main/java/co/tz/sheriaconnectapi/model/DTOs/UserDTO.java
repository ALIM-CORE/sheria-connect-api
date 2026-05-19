package co.tz.sheriaconnectapi.model.DTOs;


import co.tz.sheriaconnectapi.model.Entities.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
public class UserDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String address;

    @Enumerated(EnumType.STRING)
    private String profilePicture; // Base64 string

    @JsonIgnore
    private String password;
    private List<String> roles;
    private List<String> authorities;


    public UserDTO(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.roles = user.getRoles().stream()
                .map(role -> role.getName())
                .distinct()
                .sorted()
                .toList();
        this.authorities = user.getRoles().stream()
                .flatMap(role -> role.getAuthorities().stream())
                .map(authority -> authority.getName())
                .distinct()
                .sorted()
                .toList();

    }


    public UserDTO(Optional<User> user) {
        if (user.isEmpty()) return;
        this.id = user.get().getId();
        this.name = user.get().getName();
        this.email = user.get().getEmail();
        this.roles = user.get().getRoles().stream()
                .map(role -> role.getName())
                .distinct()
                .sorted()
                .toList();
        this.authorities = user.get().getRoles().stream()
                .flatMap(role -> role.getAuthorities().stream())
                .map(authority -> authority.getName())
                .distinct()
                .sorted()
                .toList();

    }

    private String encodeImageToBase64(byte[] imageData) {
        if (imageData == null) return null;
        return Base64.getEncoder().encodeToString(imageData);
    }
}
