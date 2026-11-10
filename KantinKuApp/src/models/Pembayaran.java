package models;

import java.time.LocalDateTime;

public final class Pembayaran {
    private final String method;
    private final String bank;
    private final String status;
    private final LocalDateTime paidAt;
    private final String reference;

    public Pembayaran(String method, String bank, String status, LocalDateTime paidAt, String reference) {
        this.method = method;
        this.bank = bank == null ? "" : bank;
        this.status = status;
        this.paidAt = paidAt;
        this.reference = reference == null ? "" : reference;
    }

    public String getMethod() { return method; }
    public String getBank() { return bank; }
    public String getStatus() { return status; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public String getReference() { return reference; }
}
