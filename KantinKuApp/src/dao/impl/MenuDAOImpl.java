package dao.impl;

import config.DatabaseConnection;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import models.Menu;
import models.MenuItem;

public final class MenuDAOImpl {
    public List<MenuItem> readAll() throws IOException {
        String sql = "SELECT id_menu, nama, deskripsi, kategori, harga, gambar, aktif, stok "
                + "FROM menu ORDER BY id_menu";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet results = statement.executeQuery()) {
            List<MenuItem> items = new ArrayList<>();
            while (results.next()) {
                items.add(readMenuItem(results));
            }
            return items;
        } catch (SQLException exception) {
            throw new IOException("Katalog menu tidak dapat dibaca dari MySQL.", exception);
        }
    }

    public Optional<List<Menu>> readActiveBuyerCatalog() throws IOException {
        String sql = "SELECT id_menu, nama, deskripsi, harga, kategori, gambar, stok "
                + "FROM menu WHERE aktif = 1 ORDER BY id_menu";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet results = statement.executeQuery()) {
            List<Menu> menus = new ArrayList<>();
            while (results.next()) {
                menus.add(new Menu(Integer.toString(results.getInt("id_menu")), results.getString("nama"),
                        results.getBigDecimal("harga").intValue(), results.getString("gambar"),
                        results.getString("kategori"), results.getString("deskripsi"),
                        results.getInt("stok")));
            }
            return Optional.of(menus);
        } catch (SQLException exception) {
            throw new IOException("Menu aktif tidak dapat dibaca dari MySQL.", exception);
        }
    }

    public long changeMarker() throws IOException {
        String sql = "SELECT COALESCE(BIT_XOR(CRC32(CONCAT_WS('#', id_menu, nama, "
                + "COALESCE(deskripsi, ''), kategori, harga, COALESCE(gambar, ''), aktif, stok))), 0) "
                + "FROM menu";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet results = statement.executeQuery()) {
            results.next();
            return results.getLong(1);
        } catch (SQLException exception) {
            throw new IOException("Perubahan katalog tidak dapat diperiksa di MySQL.", exception);
        }
    }

    public void add(MenuItem item) throws IOException {
        String sql = "INSERT INTO menu (nama, deskripsi, kategori, harga, gambar, aktif, stok) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql,
                        PreparedStatement.RETURN_GENERATED_KEYS)) {
            bindMenu(statement, item);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("MySQL tidak mengembalikan ID menu baru.");
                }
                item.setId(Integer.toString(keys.getInt(1)));
            }
        } catch (SQLException exception) {
            throw new IOException("Menu tidak dapat ditambahkan ke MySQL.", exception);
        }
    }

    public void update(MenuItem item) throws IOException {
        String sql = "UPDATE menu SET nama = ?, deskripsi = ?, kategori = ?, harga = ?, "
                + "gambar = ?, aktif = ?, stok = ? WHERE id_menu = ?";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            bindMenu(statement, item);
            statement.setInt(8, parseMenuId(item.getId()));
            if (statement.executeUpdate() == 0) {
                throw new IOException("Menu yang akan diperbarui tidak ditemukan di MySQL.");
            }
        } catch (SQLException exception) {
            throw new IOException("Menu tidak dapat diperbarui di MySQL.", exception);
        }
    }

    public void setActive(String id, boolean active) throws IOException {
        String sql = "UPDATE menu SET aktif = ? WHERE id_menu = ?";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, active);
            statement.setInt(2, parseMenuId(id));
            if (statement.executeUpdate() == 0) {
                throw new IOException("Menu yang akan diperbarui tidak ditemukan di MySQL.");
            }
        } catch (SQLException exception) {
            throw new IOException("Status menu tidak dapat diperbarui di MySQL.", exception);
        }
    }

    public void delete(String id) throws IOException {
        String sql = "DELETE FROM menu WHERE id_menu = ?";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, parseMenuId(id));
            if (statement.executeUpdate() == 0) {
                throw new IOException("Menu yang akan dihapus tidak ditemukan di MySQL.");
            }
        } catch (SQLException exception) {
            if ("23000".equals(exception.getSQLState())) {
                throw new IOException("Menu sudah digunakan pada riwayat pesanan; nonaktifkan menu agar "
                        + "riwayat transaksi tetap utuh.", exception);
            }
            throw new IOException("Menu tidak dapat dihapus dari MySQL.", exception);
        }
    }

    String decreaseStock(Connection connection, Map<String, Integer> requests) throws SQLException, IOException {
        String lockSql = "SELECT nama, aktif, stok FROM menu WHERE id_menu = ? FOR UPDATE";
        String updateSql = "UPDATE menu SET stok = stok - ? WHERE id_menu = ? AND stok >= ?";
        try (PreparedStatement lock = connection.prepareStatement(lockSql);
                PreparedStatement update = connection.prepareStatement(updateSql)) {
            for (Map.Entry<String, Integer> request : requests.entrySet()) {
                int id = parseMenuId(request.getKey());
                Integer quantity = request.getValue();
                if (quantity == null || quantity <= 0) {
                    throw new IOException("Jumlah pesanan untuk menu ID " + id + " tidak valid.");
                }
                lock.setInt(1, id);
                try (ResultSet result = lock.executeQuery()) {
                    if (!result.next()) {
                        return "Menu dengan ID " + id + " sudah tidak tersedia.";
                    }
                    String name = result.getString("nama");
                    if (!result.getBoolean("aktif")) {
                        return "Menu \"" + name + "\" sedang tidak dijual.";
                    }
                    if (result.getInt("stok") < quantity) {
                        return result.getInt("stok") <= 0 ? "Stok \"" + name + "\" sudah habis."
                                : "Stok \"" + name + "\" tinggal " + result.getInt("stok") + " porsi.";
                    }
                }
                update.setInt(1, quantity);
                update.setInt(2, id);
                update.setInt(3, quantity);
                if (update.executeUpdate() != 1) {
                    return "Stok menu ID " + id + " berubah. Silakan periksa keranjang.";
                }
            }
        }
        return null;
    }

    private static MenuItem readMenuItem(ResultSet results) throws SQLException {
        return new MenuItem(Integer.toString(results.getInt("id_menu")), results.getString("nama"),
                results.getString("deskripsi"), results.getBigDecimal("harga").intValue(),
                results.getString("kategori"), results.getString("gambar"), results.getBoolean("aktif"),
                results.getInt("stok"));
    }

    private static void bindMenu(PreparedStatement statement, MenuItem item) throws SQLException {
        statement.setString(1, item.getName());
        statement.setString(2, item.getDescription());
        statement.setString(3, item.getCategory());
        statement.setInt(4, item.getPrice());
        statement.setString(5, item.getImageName());
        statement.setBoolean(6, item.isActive());
        statement.setInt(7, item.getStock());
    }

    private static int parseMenuId(String id) throws IOException {
        try {
            int parsed = Integer.parseInt(id);
            if (parsed <= 0) {
                throw new NumberFormatException("ID must be positive");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IOException("ID menu MySQL tidak valid: " + id, exception);
        }
    }
}
