package models;

import java.time.LocalDateTime;

public final class RiwayatStatusPesanan {
    private final String orderId;
    private final String status;
    private final LocalDateTime changedAt;
    private final String note;
    private final String sellerId;

    public RiwayatStatusPesanan(String orderId, String status, LocalDateTime changedAt,
            String note, String sellerId) {
        this.orderId = orderId;
        this.status = status;
        this.changedAt = changedAt;
        this.note = note == null ? "" : note;
        this.sellerId = sellerId;
    }

    public String getOrderId() { return orderId; }
    public String getStatus() { return status; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public String getNote() { return note; }
    public String getSellerId() { return sellerId; }
}
