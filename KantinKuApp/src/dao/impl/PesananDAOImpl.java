package dao.impl;

import config.DatabaseConnection;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import models.DetailPesanan;
import models.Pembayaran;
import models.Pembeli;
import models.Pesanan;
import models.RiwayatStatusPesanan;

public final class PesananDAOImpl {
    private static final DateTimeFormatter ORDER_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Pattern DETAIL_PATTERN = Pattern.compile("^(.*?) x(\\d+) \\(Rp ([0-9.,]+)\\)$");
    private final MenuDAOImpl menuRepository = new MenuDAOImpl();
    private final PembayaranDAOImpl paymentRepository = new PembayaranDAOImpl();
    private final MySqlOrderStatusHistoryRepository historyRepository =
            new MySqlOrderStatusHistoryRepository();

    public List<Pesanan> readAll() throws IOException {
        String sql = "SELECT id_pemesanan, nama_pembeli, no_hp_pembeli, tanggal_pemesanan, "
                + "total_jumlah, status FROM detail_pemesanan "
                + "ORDER BY tanggal_pemesanan DESC, id_pemesanan DESC";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet results = statement.executeQuery()) {
            List<Pesanan> orders = new ArrayList<>();
            while (results.next()) {
                orders.add(readOrder(connection, results));
            }
            return orders;
        } catch (SQLException exception) {
            throw new IOException("Pesanan tidak dapat dibaca dari MySQL.", exception);
        }
    }

    public List<Pesanan> readForBuyer(String buyerName) throws IOException {
        List<Pesanan> orders = new ArrayList<>();
        if (buyerName == null || buyerName.isBlank()) {
            return orders;
        }
        for (Pesanan order : readAll()) {
            if (order.nama.equalsIgnoreCase(buyerName.trim())) {
                orders.add(order);
            }
        }
        return orders;
    }

    public Pesanan find(String orderId) throws IOException {
        int id = PesananId.parse(orderId);
        String sql = "SELECT id_pemesanan, nama_pembeli, no_hp_pembeli, tanggal_pemesanan, "
                + "total_jumlah, status FROM detail_pemesanan WHERE id_pemesanan = ?";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? readOrder(connection, results) : null;
            }
        } catch (SQLException exception) {
            throw new IOException("Pesanan tidak dapat dicari di MySQL.", exception);
        }
    }

    public Pesanan saveNew(Pesanan order) throws IOException {
        return saveNew(order, Map.of());
    }

    public Pesanan saveNew(Pesanan order, Map<String, Integer> requestedStock) throws IOException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                List<DetailPesanan> details = resolveDetails(connection, order);
                Map<String, Integer> stock = aggregateQuantities(details);
                validateRequestedStock(requestedStock, stock);
                String stockError = menuRepository.decreaseStock(connection, stock);
                if (stockError != null) {
                    throw new SQLException(stockError);
                }
                int orderId = insertOrder(connection, order, details);
                for (DetailPesanan detail : details) {
                    insertDetail(connection, orderId, detail);
                }
                String status = order.status == null || order.status.isBlank()
                        ? "Menunggu konfirmasi" : order.status;
                historyRepository.add(connection, orderId, status);
                Pembayaran payment = order.getPayment();
                if (payment == null) {
                    payment = new Pembayaran(order.metode, "", "", null, "");
                }
                paymentRepository.add(connection, orderId, payment);
                connection.commit();
                order.setId(PesananId.format(orderId));
                order.setDetails(details);
                order.setStatus(status);
                order.setPayment(payment);
                order.setWaktu(ORDER_TIME.format(readOrderTime(order.waktu)));
                return order;
            } catch (SQLException | RuntimeException | IOException exception) {
                rollback(connection, exception);
                throw new IOException("Pesanan, detail, pembayaran, dan riwayat tidak dapat disimpan ke MySQL.",
                        exception);
            }
        } catch (SQLException exception) {
            throw new IOException("Koneksi transaksi pesanan MySQL gagal.", exception);
        }
    }

    public void save(Pesanan order) throws IOException {
        if (find(order.id) == null) {
            saveNew(order);
        } else {
            updateStatus(order.id, order.status);
        }
    }

    public void updateStatus(String orderId, String status) throws IOException {
        int id = PesananId.parse(orderId);
        if (status == null || status.isBlank()) {
            throw new IOException("Status pesanan wajib diisi.");
        }
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String currentStatus;
                try (PreparedStatement query = connection.prepareStatement(
                        "SELECT status FROM detail_pemesanan WHERE id_pemesanan = ? FOR UPDATE")) {
                    query.setInt(1, id);
                    try (ResultSet results = query.executeQuery()) {
                        if (!results.next()) {
                            throw new SQLException("Pesanan " + PesananId.format(id) + " tidak ditemukan.");
                        }
                        currentStatus = results.getString(1);
                    }
                }
                if (!status.equals(currentStatus)) {
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE detail_pemesanan SET status = ? WHERE id_pemesanan = ?")) {
                        update.setString(1, status);
                        update.setInt(2, id);
                        update.executeUpdate();
                    }
                    historyRepository.add(connection, id, status);
                }
                if ("Diproses".equalsIgnoreCase(status.trim())) {
                    paymentRepository.markAcceptedForProcessing(connection, id);
                }
                connection.commit();
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw new IOException("Status pesanan tidak dapat diperbarui di MySQL.", exception);
            }
        } catch (SQLException exception) {
            throw new IOException("Status pesanan tidak dapat diperbarui di MySQL.", exception);
        }
    }

    public void delete(String orderId) throws IOException {
        int id = PesananId.parse(orderId);
        String sql = "DELETE FROM detail_pemesanan WHERE id_pemesanan = ?";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            if (statement.executeUpdate() == 0) {
                throw new IOException("Pesanan " + PesananId.format(id) + " tidak ditemukan.");
            }
        } catch (SQLException exception) {
            throw new IOException("Pesanan tidak dapat dihapus dari MySQL.", exception);
        }
    }

    public List<RiwayatStatusPesanan> readStatusHistory(String orderId) throws IOException {
        return historyRepository.readByOrderId(orderId);
    }

    public List<Pembeli> readBuyers() throws IOException {
        String sql = "SELECT nama_pembeli, no_hp_pembeli, MAX(id_pemesanan) AS latest_order "
                + "FROM detail_pemesanan GROUP BY nama_pembeli, no_hp_pembeli ORDER BY latest_order";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet results = statement.executeQuery()) {
            List<Pembeli> buyers = new ArrayList<>();
            while (results.next()) {
                buyers.add(new Pembeli(null, results.getString("nama_pembeli"),
                        results.getString("no_hp_pembeli")));
            }
            return buyers;
        } catch (SQLException exception) {
            throw new IOException("Daftar pembeli tidak dapat dibaca dari MySQL.", exception);
        }
    }

    public long changeMarker() throws IOException {
        String sql = "SELECT COALESCE(SUM(CRC32(CONCAT_WS('#', id_pemesanan, nama_pembeli, "
                + "COALESCE(no_hp_pembeli, ''), tanggal_pemesanan, status, total_jumlah))), 0) "
                + "FROM detail_pemesanan";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet results = statement.executeQuery()) {
            results.next();
            return results.getLong(1);
        } catch (SQLException exception) {
            throw new IOException("Perubahan pesanan tidak dapat diperiksa di MySQL.", exception);
        }
    }

    public int nextOrderNumber() throws IOException {
        String sql = "SELECT COALESCE(MAX(id_pemesanan), 0) + 1 FROM detail_pemesanan";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet results = statement.executeQuery()) {
            results.next();
            return results.getInt(1);
        } catch (SQLException exception) {
            throw new IOException("Nomor pesanan berikutnya tidak dapat dibaca dari MySQL.", exception);
        }
    }

    public List<String> readOrderIds() throws IOException {
        String sql = "SELECT id_pemesanan FROM detail_pemesanan ORDER BY id_pemesanan";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet results = statement.executeQuery()) {
            List<String> ids = new ArrayList<>();
            while (results.next()) {
                ids.add(PesananId.format(results.getInt(1)));
            }
            return ids;
        } catch (SQLException exception) {
            throw new IOException("Daftar nomor pesanan tidak dapat dibaca dari MySQL.", exception);
        }
    }

    private Pesanan readOrder(Connection connection, ResultSet results) throws SQLException {
        int orderId = results.getInt("id_pemesanan");
        Timestamp timestamp = results.getTimestamp("tanggal_pemesanan");
        String id = PesananId.format(orderId);
        Pesanan order = new Pesanan(id, results.getString("nama_pembeli"), "",
                results.getBigDecimal("total_jumlah").intValue(), results.getString("status"), "",
                results.getString("no_hp_pembeli"),
                timestamp == null ? ORDER_TIME.format(LocalDateTime.now())
                        : ORDER_TIME.format(timestamp.toLocalDateTime()));
        List<DetailPesanan> details = readDetails(connection, orderId);
        order.setDetails(details);
        order.setRincian(formatDetails(details));
        Pembayaran payment = paymentRepository.findByOrderId(connection, orderId);
        order.setPayment(payment);
        if (payment != null) {
            order.metode = payment.getMethod();
        }
        return order;
    }

    private static List<DetailPesanan> readDetails(Connection connection, int orderId)
            throws SQLException {
        String sql = "SELECT oi.menu_id, COALESCE(m.nama, CONCAT('Menu #', oi.menu_id)) AS menu_name, "
                + "oi.jumlah, oi.harga_satuan, oi.subtotal FROM order_item oi "
                + "LEFT JOIN menu m ON m.id_menu = oi.menu_id WHERE oi.id_pemesanan = ? "
                + "ORDER BY oi.id_order_item";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, orderId);
            try (ResultSet results = statement.executeQuery()) {
                List<DetailPesanan> details = new ArrayList<>();
                while (results.next()) {
                    details.add(new DetailPesanan(Integer.toString(results.getInt("menu_id")),
                            results.getString("menu_name"), results.getInt("jumlah"),
                            results.getBigDecimal("harga_satuan").intValue(),
                            results.getBigDecimal("subtotal").intValue(), ""));
                }
                return details;
            }
        }
    }

    private static List<DetailPesanan> resolveDetails(Connection connection, Pesanan order)
            throws SQLException, IOException {
        List<DetailPesanan> details = order.getDetails();
        if (!details.isEmpty()) {
            return details;
        }
        if (order.rincian == null || order.rincian.isBlank()) {
            throw new IOException("Pesanan harus memiliki rincian menu.");
        }
        String findMenu = "SELECT id_menu FROM menu WHERE LOWER(nama) = LOWER(?)";
        for (String line : order.rincian.split("\\R")) {
            Matcher matcher = DETAIL_PATTERN.matcher(line.trim());
            if (!matcher.matches()) {
                throw new IOException("Rincian pesanan tidak dapat dipetakan ke menu: " + line);
            }
            String name = matcher.group(1).trim();
            int quantity = Integer.parseInt(matcher.group(2));
            int subtotal = Integer.parseInt(matcher.group(3).replaceAll("[^0-9]", ""));
            if (quantity <= 0 || subtotal < 0 || subtotal % quantity != 0) {
                throw new IOException("Jumlah atau subtotal menu \"" + name + "\" tidak valid.");
            }
            try (PreparedStatement statement = connection.prepareStatement(findMenu)) {
                statement.setString(1, name);
                try (ResultSet results = statement.executeQuery()) {
                    if (!results.next()) {
                        throw new IOException("Menu \"" + name + "\" tidak ditemukan.");
                    }
                    String menuId = Integer.toString(results.getInt(1));
                    if (results.next()) {
                        throw new IOException("Nama menu \"" + name + "\" tidak unik; ID menu wajib disediakan.");
                    }
                    details.add(new DetailPesanan(menuId, name, quantity, subtotal / quantity,
                            subtotal, ""));
                }
            }
        }
        return details;
    }

    private static int insertOrder(Connection connection, Pesanan order, List<DetailPesanan> details)
            throws SQLException, IOException {
        long total = 0;
        try {
            for (DetailPesanan detail : details) {
                if (detail.getQuantity() <= 0 || detail.getUnitPrice() < 0) {
                    throw new IOException("Jumlah atau harga menu dalam pesanan tidak valid.");
                }
                long subtotal = Math.multiplyExact((long) detail.getQuantity(), detail.getUnitPrice());
                if (subtotal != detail.getSubtotal()) {
                    throw new IOException("Subtotal menu \"" + detail.getMenuName() + "\" tidak sesuai.");
                }
                total = Math.addExact(total, subtotal);
            }
        } catch (ArithmeticException exception) {
            throw new IOException("Total pesanan melebihi batas angka yang didukung.", exception);
        }
        if (total != order.total) {
            throw new IOException("Total pesanan tidak sesuai dengan jumlah rincian menu.");
        }
        String sql = "INSERT INTO detail_pemesanan "
                + "(nama_pembeli, no_hp_pembeli, tanggal_pemesanan, status, total_jumlah, tracking_token) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql,
                PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, order.nama);
            statement.setString(2, order.hp == null || order.hp.isBlank() ? null : order.hp);
            statement.setTimestamp(3, Timestamp.valueOf(readOrderTime(order.waktu)));
            statement.setString(4, order.status == null || order.status.isBlank()
                    ? "Menunggu konfirmasi" : order.status);
            statement.setBigDecimal(5, BigDecimal.valueOf(total));
            statement.setString(6, UUID.randomUUID().toString().replace("-", "")
                    + UUID.randomUUID().toString().replace("-", ""));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("MySQL tidak mengembalikan ID pesanan baru.");
                }
                return keys.getInt(1);
            }
        }
    }

    private static void insertDetail(Connection connection, int orderId, DetailPesanan detail)
            throws SQLException, IOException {
        String sql = "INSERT INTO order_item "
                + "(id_pemesanan, menu_id, jumlah, harga_satuan, subtotal) VALUES (?, ?, ?, ?, ?)";
        int menuId;
        try {
            menuId = Integer.parseInt(detail.getMenuId());
        } catch (NumberFormatException exception) {
            throw new IOException("ID menu MySQL tidak valid: " + detail.getMenuId(), exception);
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, orderId);
            statement.setInt(2, menuId);
            statement.setInt(3, detail.getQuantity());
            statement.setBigDecimal(4, BigDecimal.valueOf(detail.getUnitPrice()));
            statement.setBigDecimal(5, BigDecimal.valueOf(detail.getSubtotal()));
            statement.executeUpdate();
        }
    }

    private static Map<String, Integer> aggregateQuantities(List<DetailPesanan> details)
            throws IOException {
        Map<String, Integer> stock = new LinkedHashMap<>();
        try {
            for (DetailPesanan detail : details) {
                int menuId = Integer.parseInt(detail.getMenuId());
                stock.merge(Integer.toString(menuId), detail.getQuantity(), Math::addExact);
            }
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new IOException("ID menu atau jumlah pesanan tidak valid.", exception);
        }
        return stock;
    }

    private static void validateRequestedStock(Map<String, Integer> requestedStock,
            Map<String, Integer> actualStock) throws IOException {
        if (requestedStock == null || requestedStock.isEmpty()) {
            return;
        }
        Map<String, Integer> normalized = new LinkedHashMap<>();
        try {
            for (Map.Entry<String, Integer> entry : requestedStock.entrySet()) {
                normalized.put(Integer.toString(Integer.parseInt(entry.getKey())), entry.getValue());
            }
        } catch (NumberFormatException exception) {
            throw new IOException("ID menu pada keranjang tidak valid.", exception);
        }
        if (!normalized.equals(actualStock)) {
            throw new IOException("Permintaan stok tidak sesuai dengan rincian pesanan.");
        }
    }

    private static String formatDetails(List<DetailPesanan> details) {
        StringBuilder text = new StringBuilder();
        for (DetailPesanan detail : details) {
            if (text.length() > 0) {
                text.append('\n');
            }
            text.append(detail.getMenuName()).append(" x").append(detail.getQuantity())
                    .append(" (Rp ").append(detail.getSubtotal()).append(')');
        }
        return text.toString();
    }

    private static LocalDateTime readOrderTime(String value) {
        if (value != null && !value.isBlank()) {
            try {
                return LocalDateTime.parse(value, ORDER_TIME);
            } catch (DateTimeParseException ignored) {
                return LocalDateTime.now();
            }
        }
        return LocalDateTime.now();
    }

    private static void rollback(Connection connection, Exception cause) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            cause.addSuppressed(rollbackException);
        }
    }
}
