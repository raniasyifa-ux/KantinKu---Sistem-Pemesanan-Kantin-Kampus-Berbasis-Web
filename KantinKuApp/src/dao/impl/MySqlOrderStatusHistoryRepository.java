package dao.impl;

import config.DatabaseConnection;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import models.RiwayatStatusPesanan;

public final class MySqlOrderStatusHistoryRepository {
    public List<RiwayatStatusPesanan> readByOrderId(String orderId) throws IOException {
        int parsedOrderId = PesananId.parse(orderId);
        try (Connection connection = DatabaseConnection.getConnection()) {
            return readByOrderId(connection, parsedOrderId);
        } catch (SQLException exception) {
            throw new IOException("Riwayat status pesanan tidak dapat dibaca dari MySQL.", exception);
        }
    }

    void add(Connection connection, int orderId, String status) throws SQLException {
        String sql = "INSERT INTO riwayat_status_pesanan (id_pemesanan, status) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, orderId);
            statement.setString(2, status);
            statement.executeUpdate();
        }
    }

    List<RiwayatStatusPesanan> readByOrderId(Connection connection, int orderId)
            throws SQLException {
        String sql = "SELECT status, dibuat_pada FROM riwayat_status_pesanan "
                + "WHERE id_pemesanan = ? ORDER BY dibuat_pada, id_riwayat";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, orderId);
            try (ResultSet results = statement.executeQuery()) {
                List<RiwayatStatusPesanan> history = new ArrayList<>();
                while (results.next()) {
                    Timestamp timestamp = results.getTimestamp("dibuat_pada");
                    history.add(new RiwayatStatusPesanan(PesananId.format(orderId),
                            results.getString("status"),
                            timestamp == null ? LocalDateTime.now() : timestamp.toLocalDateTime(),
                            "", null));
                }
                return history;
            }
        }
    }
}
