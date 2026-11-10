package models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class Pesanan {
    public String id;
    public String nama;
    public String metode;
    public String status;
    public String rincian;
    public String hp;
    public String waktu;
    public int total;
    private String buyerId;
    private String sellerId;
    private List<DetailPesanan> details = new ArrayList<>();
    private Pembayaran payment;

    public Pesanan(String id, String nama, String metode, int total, String status, String rincian) {
        this(id, nama, metode, total, status, rincian, "", sekarang());
    }

    public Pesanan(String id, String nama, String metode, int total, String status, String rincian,
            String hp, String waktu) {
        this.id = id;
        this.nama = nama;
        this.metode = metode;
        this.total = total;
        this.status = status;
        this.rincian = rincian;
        this.hp = hp == null ? "" : hp;
        this.waktu = waktu == null || waktu.isBlank() ? sekarang() : waktu;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }
    public String getMetode() { return metode; }
    public void setMetode(String metode) { this.metode = metode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRincian() { return rincian; }
    public void setRincian(String rincian) { this.rincian = rincian; }
    public String getHp() { return hp; }
    public void setHp(String hp) { this.hp = hp == null ? "" : hp; }
    public String getWaktu() { return waktu; }
    public void setWaktu(String waktu) { this.waktu = waktu == null || waktu.isBlank() ? sekarang() : waktu; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public List<DetailPesanan> getDetails() { return new ArrayList<>(details); }
    public void setDetails(List<DetailPesanan> details) {
        this.details = details == null ? new ArrayList<>() : new ArrayList<>(details);
    }
    public Pembayaran getPayment() { return payment; }
    public void setPayment(Pembayaran payment) { this.payment = payment; }

    private static String sekarang() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
