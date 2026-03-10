package com.example.mailsearch.dto;

public class AuthUserResponse {
    private final String username;
    private final boolean mustChangePassword;

    public AuthUserResponse(String username, boolean mustChangePassword) {
        this.username = username;
        this.mustChangePassword = mustChangePassword;
    }

    public String getUsername() {
        return username;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }
}
