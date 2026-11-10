package models;

import java.time.LocalDateTime;

public final class RiwayatStatusPembayaran {
    private final String orderId;
    private final String status;
    private final LocalDateTime changedAt;

    public RiwayatStatusPembayaran(String orderId, String status, LocalDateTime changedAt) {
        this.orderId = orderId;
        this.status = status;
        this.changedAt = changedAt;
    }

    public String getOrderId() { return orderId; }
    public String getStatus() { return status; }
    public LocalDateTime getChangedAt() { return changedAt; }
}
