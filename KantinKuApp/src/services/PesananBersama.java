package services;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import models.Pembeli;
import models.Pesanan;
import models.RiwayatStatusPembayaran;
import models.RiwayatStatusPesanan;
import dao.impl.PesananDAOImpl;
import dao.impl.PembayaranDAOImpl;

/** Business-facing API for orders shared by the buyer and seller applications. */
public final class PesananBersama {
    private static final PesananDAOImpl ORDER_REPOSITORY = new PesananDAOImpl();
    private static final PembayaranDAOImpl PAYMENT_REPOSITORY = new PembayaranDAOImpl();

    private PesananBersama() {
    }

    public static String sekarang() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    public static synchronized List<Pembeli> bacaPembeli() {
        try {
            return ORDER_REPOSITORY.readBuyers();
        } catch (IOException exception) {
            System.err.println("Data pembeli tidak dapat dibaca: " + exception.getMessage());
            return new ArrayList<>();
        }
    }

    public static synchronized Pembeli cariPembeli(String nama) {
        if (nama == null) {
            return null;
        }
        for (Pembeli pembeli : bacaPembeli()) {
            if (pembeli.nama.equalsIgnoreCase(nama.trim())) {
                return pembeli;
            }
        }
        return null;
    }

    public static synchronized Pembeli pembeliTerakhir() {
        List<Pembeli> pembeli = bacaPembeli();
        return pembeli.isEmpty() ? null : pembeli.get(pembeli.size() - 1);
    }

    public static synchronized List<Pesanan> pesananDari(String nama) throws IOException {
        return ORDER_REPOSITORY.readForBuyer(nama);
    }

    public static synchronized List<Pesanan> baca() throws IOException {
        return ORDER_REPOSITORY.readAll();
    }

    public static synchronized Pesanan simpanBaru(Pesanan pesanan) throws IOException {
        return ORDER_REPOSITORY.saveNew(pesanan);
    }

    public static synchronized Pesanan simpanBaru(Pesanan pesanan, Map<String, Integer> permintaanStok)
            throws IOException {
        return ORDER_REPOSITORY.saveNew(pesanan, permintaanStok);
    }

    public static synchronized void simpan(Pesanan pesanan) throws IOException {
        ORDER_REPOSITORY.save(pesanan);
    }

    public static synchronized void ubahStatus(String id, String status) throws IOException {
        ORDER_REPOSITORY.updateStatus(id, status);
    }

    public static synchronized void ubahStatusPembayaran(String id, String status) throws IOException {
        PAYMENT_REPOSITORY.updateStatus(id, status);
    }

    public static synchronized void hapus(String id) throws IOException {
        ORDER_REPOSITORY.delete(id);
    }

    public static synchronized Pesanan cari(String id) throws IOException {
        return ORDER_REPOSITORY.find(id);
    }

    public static synchronized int nomorBerikutnya() throws IOException {
        return ORDER_REPOSITORY.nextOrderNumber();
    }

    public static String formatId(int nomor) {
        return "#ORD-" + String.format("%03d", nomor);
    }

    public static synchronized List<String> bacaIdPembeli() {
        try {
            return ORDER_REPOSITORY.readOrderIds();
        } catch (IOException exception) {
            System.err.println("Daftar pesanan pembeli tidak dapat dibaca: " + exception.getMessage());
            return new ArrayList<>();
        }
    }

    public static synchronized void catatIdPembeli(String id) throws IOException {
        if (!ORDER_REPOSITORY.readOrderIds().contains(id)) {
            throw new IOException("Nomor pesanan " + id + " tidak ditemukan di MySQL.");
        }
    }

    public static long penandaPerubahan() {
        try {
            return ORDER_REPOSITORY.changeMarker();
        } catch (IOException exception) {
            throw new IllegalStateException("Perubahan pesanan MySQL tidak dapat diperiksa.", exception);
        }
    }

    public static synchronized List<RiwayatStatusPesanan> riwayatStatus(String orderId) throws IOException {
        return ORDER_REPOSITORY.readStatusHistory(orderId);
    }

    public static synchronized List<RiwayatStatusPembayaran> riwayatStatusPembayaran(String orderId)
            throws IOException {
        return PAYMENT_REPOSITORY.readStatusHistory(orderId);
    }
}
