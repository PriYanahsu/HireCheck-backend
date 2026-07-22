package com.hirecheck.security;

public class UserPrincipal {
    private final Integer id;
    private final String username;
    public UserPrincipal(Integer id, String username) { this.id = id; this.username = username; }
    public Integer getId() { return id; }
    public String getUsername() { return username; }
}
