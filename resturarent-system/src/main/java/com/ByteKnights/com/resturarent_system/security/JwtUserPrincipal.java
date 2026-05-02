package com.ByteKnights.com.resturarent_system.security;

import com.ByteKnights.com.resturarent_system.entity.Privilege;
import com.ByteKnights.com.resturarent_system.entity.Role;
import com.ByteKnights.com.resturarent_system.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
public class JwtUserPrincipal implements UserDetails {

    private final User user;

    public JwtUserPrincipal(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();

        Role role = user.getRole();

        if (role != null && role.getName() != null) {
            String roleName = role.getName().trim();

            /*
             * Role authority.
             *
             * This keeps existing checks working:
             * @PreAuthorize("hasRole('ADMIN')")
             * @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
             *
             * Spring role checks expect ROLE_ prefix internally.
             */
            String roleAuthority = roleName.startsWith("ROLE_")
                    ? roleName
                    : "ROLE_" + roleName;

            authorities.add(new SimpleGrantedAuthority(roleAuthority));

            /*
             * Privilege authorities.
             *
             * These make dynamic RBAC permissions work:
             * @PreAuthorize("hasAuthority('CREATE_STAFF')")
             * @PreAuthorize("hasAuthority('VIEW_ORDERS')")
             *
             * These privileges are NOT stored in the JWT.
             * They are loaded from:
             * User -> Role -> role_permissions -> Privileges
             */
            if (role.getPermissions() != null) {
                for (Privilege privilege : role.getPermissions()) {
                    if (privilege != null && privilege.getName() != null) {
                        String privilegeName = privilege.getName().trim();

                        if (!privilegeName.isEmpty()) {
                            authorities.add(new SimpleGrantedAuthority(privilegeName));
                        }
                    }
                }
            }
        }

        return authorities;
    }

    public String getEmail() {
        return user.getEmail();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return user.getEmail();
        }

        return user.getPhone();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(user.getIsActive());
    }
}