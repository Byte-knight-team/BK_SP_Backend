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

    /*
     * Uses this to understand the logged in user.
     */
    private final User user;

    public JwtUserPrincipal(User user) {
        this.user = user;
    }

    @Override
    /*
     * Returns all access rights for the logged in user
     * Role authority and privilege authority 
     */
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();

        Role role = user.getRole();

        if (role != null && role.getName() != null) {
            String roleName = role.getName().trim();

            /*
             * Role authority - checks need the ROLE_ prefix
             */
            String roleAuthority = roleName.startsWith("ROLE_")
                    ? roleName
                    : "ROLE_" + roleName;

            //Add role as authority
            authorities.add(new SimpleGrantedAuthority(roleAuthority));

            /*
            * Add role permissions as authorities.
            * This allows permission-based checks like hasAuthority("CREATE_STAFF").
            */

            //Check permissions are not null
            if (role.getPermissions() != null) {
                for (Privilege privilege : role.getPermissions()) {
                    if (privilege != null && privilege.getName() != null) {
                        String privilegeName = privilege.getName().trim();

                        if (!privilegeName.isEmpty()) {

                            //Add privilege as authority
                            authorities.add(new SimpleGrantedAuthority(privilegeName));
                        }
                    }
                }
            }
        }

        return authorities;
    }

    /*
     * Returns the email of the logged in user
     */
    public String getEmail() {
        return user.getEmail();
    }

    /*
     * This returns the encoded password stored in the database
     */
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /*
     * Returns the username of the logged in user by using email
     * if email is null or empty or blank, return phone number
     */
    @Override
    public String getUsername() {
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return user.getEmail();
        }

        return user.getPhone();
    }

    /*
     * Account locking is not separately implemented
     * Active/inactive status is handled in isEnabled()
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /*
     * Returns true if the credentials are not expired
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /*
     * Disabled users cannot access protected endpoints
     */
    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(user.getIsActive());
    }
}