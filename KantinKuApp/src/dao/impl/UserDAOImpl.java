package dao.impl;

import config.DatabaseConnection;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import models.Account;

public final class UserDAOImpl {
    public List<Account> loadSellers() throws IOException {
        String sql = "SELECT id_user, username, email, password, role FROM users "
                + "WHERE role = 'penjual' ORDER BY id_user";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet results = statement.executeQuery()) {
            List<Account> accounts = new ArrayList<>();
            while (results.next()) {
                accounts.add(readAccount(results));
            }
            return accounts;
        } catch (SQLException exception) {
            throw new IOException("Akun penjual tidak dapat dibaca dari MySQL.", exception);
        }
    }

    public Account findSeller(String usernameOrEmail) throws IOException {
        String sql = "SELECT id_user, username, email, password, role FROM users "
                + "WHERE role = 'penjual' AND (LOWER(username) = LOWER(?) OR LOWER(email) = LOWER(?)) LIMIT 1";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, usernameOrEmail);
            statement.setString(2, usernameOrEmail);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? readAccount(results) : null;
            }
        } catch (SQLException exception) {
            throw new IOException("Akun penjual tidak dapat dicari di MySQL.", exception);
        }
    }

    public boolean exists(String username, String email) throws IOException {
        String sql = "SELECT 1 FROM users WHERE LOWER(username) = LOWER(?) OR LOWER(email) = LOWER(?) LIMIT 1";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, email);
            try (ResultSet results = statement.executeQuery()) {
                return results.next();
            }
        } catch (SQLException exception) {
            throw new IOException("Keunikan akun tidak dapat diperiksa di MySQL.", exception);
        }
    }

    public void addSeller(String username, String email, String passwordHash) throws IOException {
        String sql = "INSERT INTO users (email, username, password, role) VALUES (?, ?, ?, 'penjual')";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            statement.setString(2, username);
            statement.setString(3, passwordHash);
            statement.executeUpdate();
        } catch (SQLException exception) {
            if (exception.getErrorCode() == 1062) {
                throw new IOException("Username atau email sudah terdaftar.", exception);
            }
            throw new IOException("Akun penjual tidak dapat disimpan ke MySQL.", exception);
        }
    }

    public void updateSellerPassword(String email, String passwordHash) throws IOException {
        String sql = "UPDATE users SET password = ? WHERE LOWER(email) = LOWER(?) AND role = 'penjual'";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, passwordHash);
            statement.setString(2, email);
            if (statement.executeUpdate() == 0) {
                throw new IOException("Akun penjual dengan email tersebut tidak ditemukan.");
            }
        } catch (SQLException exception) {
            throw new IOException("Password akun penjual tidak dapat diperbarui di MySQL.", exception);
        }
    }

    private static Account readAccount(ResultSet results) throws SQLException {
        return new Account(results.getInt("id_user"), results.getString("username"),
                results.getString("email"), results.getString("password"), results.getString("role"));
    }
}
