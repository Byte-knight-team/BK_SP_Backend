package com.ByteKnights.com.resturarent_system.dto;

public class LoginResponse {

    private Long id;
    private String username;
    private String email;
    private String role;
    private Boolean active;

    public LoginResponse(Long id, String username, String email, String role, Boolean active) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public Boolean getActive() {
        return active;
    }
}
