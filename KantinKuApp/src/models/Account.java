package models;

import java.util.Arrays;

public final class Account {
    private final String username;
    private final String email;
    private byte[] salt;
    private byte[] passwordHash;
    private String passwordHashText;
    private final int userId;
    private final String role;

    public Account(String username, String email, byte[] salt, byte[] passwordHash) {
        this.username = username;
        this.email = email;
        this.userId = 0;
        this.role = "penjual";
        setCredentials(salt, passwordHash);
    }

    public Account(int userId, String username, String email, String passwordHash, String role) {
        this.username = username;
        this.email = email;
        this.userId = userId;
        this.role = role;
        this.passwordHashText = passwordHash;
        this.salt = new byte[0];
        this.passwordHash = new byte[0];
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public int getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public String getPasswordHashText() {
        return passwordHashText;
    }

    public void setPasswordHashText(String passwordHashText) {
        this.passwordHashText = passwordHashText;
    }

    public byte[] getSalt() {
        return Arrays.copyOf(salt, salt.length);
    }

    public byte[] getPasswordHash() {
        return Arrays.copyOf(passwordHash, passwordHash.length);
    }

    public void setCredentials(byte[] salt, byte[] passwordHash) {
        this.salt = Arrays.copyOf(salt, salt.length);
        this.passwordHash = Arrays.copyOf(passwordHash, passwordHash.length);
    }
}
