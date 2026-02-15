package com.byteknights.com.resturarent_system.auth.dto;

public class LoginResponse {
    private Long id;
    private String email;
    private String role;
    private String fullName;
    private boolean active;

    public LoginResponse(Long id, String email, String role, String fullName, boolean active) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.fullName = fullName;
        this.active = active;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getFullName() { return fullName; }
    public boolean isActive() { return active; }
}
