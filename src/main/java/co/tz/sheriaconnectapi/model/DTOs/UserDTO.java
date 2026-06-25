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
import co.tz.sheriaconnectapi.model.Enums.UserAccountType;
import java.time.Instant;
import co.tz.sheriaconnectapi.security.Access.EffectiveAccess;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;

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
    private List<String> roleDisplayNames;
    private List<String> authorities;
    private UserAccountType accountType;
    private boolean active;
    private boolean locked;
    private Instant lastLoginAt;
    private AccessContext activeContext;


    public UserDTO(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.roles = user.getRoles().stream()
                .map(role -> role.getName())
                .distinct()
                .sorted()
                .toList();
        this.roleDisplayNames = user.getRoles().stream()
                .map(role -> role.getDisplayName())
                .distinct()
                .sorted()
                .toList();
        this.authorities = user.getRoles().stream()
                .flatMap(role -> role.getAuthorities().stream())
                .map(authority -> authority.getName())
                .distinct()
                .sorted()
                .toList();
        this.accountType = user.getAccountType();
        this.active = Boolean.TRUE.equals(user.getActive());
        this.locked = Boolean.TRUE.equals(user.getLocked());
        this.lastLoginAt = user.getLastLoginAt();

    }

    public UserDTO(User user, EffectiveAccess access) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.roles = access.roles().stream()
                .map(role -> role.getName())
                .distinct()
                .sorted()
                .toList();
        this.roleDisplayNames = access.roles().stream()
                .map(role -> role.getDisplayName())
                .distinct()
                .sorted()
                .toList();
        this.authorities = access.authorities().stream()
                .map(authority -> authority.getAuthority())
                .distinct()
                .sorted()
                .toList();
        this.accountType = user.getAccountType();
        this.active = Boolean.TRUE.equals(user.getActive());
        this.locked = Boolean.TRUE.equals(user.getLocked());
        this.lastLoginAt = user.getLastLoginAt();
        this.activeContext = access.context();
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
        this.roleDisplayNames = user.get().getRoles().stream()
                .map(role -> role.getDisplayName())
                .distinct()
                .sorted()
                .toList();
        this.authorities = user.get().getRoles().stream()
                .flatMap(role -> role.getAuthorities().stream())
                .map(authority -> authority.getName())
                .distinct()
                .sorted()
                .toList();
        this.accountType = user.get().getAccountType();
        this.active = Boolean.TRUE.equals(user.get().getActive());
        this.locked = Boolean.TRUE.equals(user.get().getLocked());
        this.lastLoginAt = user.get().getLastLoginAt();

    }

    private String encodeImageToBase64(byte[] imageData) {
        if (imageData == null) return null;
        return Base64.getEncoder().encodeToString(imageData);
    }
}
