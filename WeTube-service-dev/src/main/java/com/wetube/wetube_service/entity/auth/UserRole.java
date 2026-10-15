package com.wetube.wetube_service.entity.auth;

import com.wetube.wetube_service.entity.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users_roles")
public class UserRole {

    @EmbeddedId
    private UserRoleId id = new UserRoleId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId") // match field in UserRoleId
    @JoinColumn(name = "user_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roleId") // match field in UserRoleId
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    public UserRole(AppUser user, Role role) {
        this.user = user;
        this.role = role;
        this.id = new UserRoleId(user.getId(), role.getId());
    }
}
