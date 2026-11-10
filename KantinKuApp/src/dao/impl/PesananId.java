package dao.impl;

import java.io.IOException;

final class PesananId {
    private PesananId() {
    }

    static int parse(String orderId) throws IOException {
        if (orderId == null) {
            throw new IOException("Nomor pesanan tidak valid.");
        }
        String value = orderId.trim();
        if (value.startsWith("#ORD-")) {
            value = value.substring(5);
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new NumberFormatException("ID must be positive");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IOException("Nomor pesanan MySQL tidak valid: " + orderId, exception);
        }
    }

    static String format(int orderId) {
        return "#ORD-" + String.format("%03d", orderId);
    }
}
