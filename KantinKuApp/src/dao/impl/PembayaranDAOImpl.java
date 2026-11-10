package dao.impl;

import config.DatabaseConnection;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Locale;
import java.util.List;
import models.Pembayaran;
import models.RiwayatStatusPembayaran;

public final class PembayaranDAOImpl {
    private final MySqlPaymentStatusHistoryRepository historyRepository =
            new MySqlPaymentStatusHistoryRepository();

    public Pembayaran findByOrderId(String orderId) throws IOException {
        int parsedOrderId = PesananId.parse(orderId);
        try (Connection connection = DatabaseConnection.getConnection()) {
            return findByOrderId(connection, parsedOrderId);
        } catch (SQLException exception) {
            throw new IOException("Pembayaran tidak dapat dibaca dari MySQL.", exception);
        }
    }

    public List<RiwayatStatusPembayaran> readStatusHistory(String orderId) throws IOException {
        return historyRepository.readByOrderId(orderId);
    }

    public void updateStatus(String orderId, String status) throws IOException {
        int parsedOrderId = PesananId.parse(orderId);
        if (status == null || status.isBlank()) {
            throw new IOException("Status pembayaran wajib diisi.");
        }
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String currentStatus;
                try (PreparedStatement query = connection.prepareStatement(
                        "SELECT status_pembayaran FROM pembayaran WHERE id_pemesanan = ? FOR UPDATE")) {
                    query.setInt(1, parsedOrderId);
                    try (ResultSet results = query.executeQuery()) {
                        if (!results.next()) {
                            throw new SQLException("Pembayaran untuk pesanan "
                                    + PesananId.format(parsedOrderId) + " tidak ditemukan.");
                        }
                        currentStatus = results.getString(1);
                    }
                }
                if (!status.equals(currentStatus)) {
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE pembayaran SET status_pembayaran = ? WHERE id_pemesanan = ?")) {
                        update.setString(1, status);
                        update.setInt(2, parsedOrderId);
                        update.executeUpdate();
                    }
                    historyRepository.add(connection, parsedOrderId, status);
                }
                connection.commit();
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw new IOException("Status pembayaran tidak dapat diperbarui di MySQL.", exception);
            }
        } catch (SQLException exception) {
            throw new IOException("Status pembayaran tidak dapat diperbarui di MySQL.", exception);
        }
    }

    Pembayaran findByOrderId(Connection connection, int orderId) throws SQLException {
        String sql = "SELECT metode_pembayaran, status_pembayaran, tanggal_pembayaran "
                + "FROM pembayaran WHERE id_pemesanan = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, orderId);
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    return null;
                }
                Timestamp timestamp = results.getTimestamp("tanggal_pembayaran");
                String method = results.getString("metode_pembayaran");
                return new Pembayaran(method, bankFromMethod(method),
                        results.getString("status_pembayaran"),
                        timestamp == null ? null : timestamp.toLocalDateTime(), "");
            }
        }
    }

    void add(Connection connection, int orderId, Pembayaran payment) throws SQLException {
        String method = payment.getMethod();
        if (method == null || method.isBlank()) {
            throw new SQLException("Metode pembayaran wajib diisi.");
        }
        String status = payment.getStatus();
        if (status == null || status.isBlank()) {
            status = method.toLowerCase(Locale.ROOT).contains("cod")
                    || method.toLowerCase(Locale.ROOT).contains("tunai")
                    ? "Menunggu pembayaran di tempat" : "Menunggu verifikasi";
        }
        String sql = "INSERT INTO pembayaran (id_pemesanan, metode_pembayaran, status_pembayaran) "
                + "VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, orderId);
            statement.setString(2, method.trim());
            statement.setString(3, status);
            statement.executeUpdate();
        }
        historyRepository.add(connection, orderId, status);
    }

    void markAcceptedForProcessing(Connection connection, int orderId) throws SQLException {
        String method;
        String currentStatus;
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT metode_pembayaran, status_pembayaran FROM pembayaran "
                        + "WHERE id_pemesanan = ? FOR UPDATE")) {
            query.setInt(1, orderId);
            try (ResultSet results = query.executeQuery()) {
                if (!results.next()) {
                    return;
                }
                method = results.getString("metode_pembayaran");
                currentStatus = results.getString("status_pembayaran");
            }
        }
        if (!"Menunggu verifikasi".equalsIgnoreCase(currentStatus)
                && !"Menunggu pembayaran di tempat".equalsIgnoreCase(currentStatus)) {
            return;
        }
        String normalizedMethod = method == null ? "" : method.toLowerCase(Locale.ROOT);
        String nextStatus = normalizedMethod.contains("cod") || normalizedMethod.contains("tunai")
                ? "Berhasil" : "Disetujui";
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE pembayaran SET status_pembayaran = ? WHERE id_pemesanan = ?")) {
            update.setString(1, nextStatus);
            update.setInt(2, orderId);
            update.executeUpdate();
        }
        historyRepository.add(connection, orderId, nextStatus);
    }

    private static String bankFromMethod(String method) {
        if (method == null) {
            return "";
        }
        int start = method.indexOf('(');
        int end = method.indexOf(')', start + 1);
        return start >= 0 && end > start ? method.substring(start + 1, end).trim() : "";
    }

    private static void rollback(Connection connection, SQLException cause) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            cause.addSuppressed(rollbackException);
        }
    }
}
