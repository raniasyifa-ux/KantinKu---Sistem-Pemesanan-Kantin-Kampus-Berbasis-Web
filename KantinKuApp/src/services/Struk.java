package services;

import config.AppConfig;
import java.io.IOException;
import java.nio.file.Path;
import models.Pesanan;
import dao.impl.StrukRepository;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Membuat struk pembelian berbentuk teks dari sebuah pesanan. */
public final class Struk {
    private static final int LEBAR = 40;
    private static final Pattern BARIS_ITEM = Pattern.compile("^(.*) x(\\d+) \\(Rp ([^)]*)\\)$");
    private static final StrukRepository REPOSITORY = new StrukRepository();

    private Struk() {}

    public static String buat(Pesanan p) {
        StringBuilder sb = new StringBuilder();
        garis(sb, '=');
        tengah(sb, "KANTINKU");
        tengah(sb, "Struk Pembelian");
        garis(sb, '=');
        kiriKanan(sb, "No. Pesanan", p.id);
        kiriKanan(sb, "Tanggal", p.waktu);
        kiriKanan(sb, "Pembeli", p.nama);
        if (p.hp != null && !p.hp.isBlank()) kiriKanan(sb, "No. HP", p.hp);
        kiriKanan(sb, "Pembayaran", p.metode);
        garis(sb, '-');
        for (String baris : p.rincian.split("\n")) {
            if (baris.isBlank()) continue;
            Matcher m = BARIS_ITEM.matcher(baris.trim());
            if (m.matches()) {
                sb.append(potong(m.group(1), LEBAR)).append('\n');
                kiriKanan(sb, "  " + m.group(2) + " porsi", "Rp " + m.group(3));
            } else {
                sb.append(potong(baris.trim(), LEBAR)).append('\n');
            }
        }
        garis(sb, '-');
        kiriKanan(sb, "TOTAL", "Rp " + String.format("%,d", p.total));
        garis(sb, '=');
        tengah(sb, "Terima kasih sudah memesan!");
        tengah(sb, "Tunjukkan struk saat mengambil pesanan.");
        garis(sb, '=');
        return sb.toString();
    }

    /** Menyimpan struk ke berkas teks; mengembalikan lokasi berkas. */
    public static Path simpan(Pesanan p, Path tujuan) throws IOException {
        return REPOSITORY.save(buat(p), tujuan);
    }

    /** Lokasi salinan struk otomatis di folder data aplikasi. */
    public static Path lokasiBawaan(Pesanan p) {
        String aman = p.id.replaceAll("[^A-Za-z0-9-]", "");
        return AppConfig.dataDirectory().resolve("struk").resolve("struk-" + aman + ".txt");
    }

    public static String namaBerkasUsulan(Pesanan p) {
        return "struk-" + p.id.replaceAll("[^A-Za-z0-9-]", "") + ".txt";
    }

    private static void garis(StringBuilder sb, char c) {
        sb.append(String.valueOf(c).repeat(LEBAR)).append('\n');
    }

    private static void tengah(StringBuilder sb, String teks) {
        String t = potong(teks, LEBAR);
        int pad = Math.max(0, (LEBAR - t.length()) / 2);
        sb.append(" ".repeat(pad)).append(t).append('\n');
    }

    private static void kiriKanan(StringBuilder sb, String kiri, String kanan) {
        String r = kanan == null ? "" : kanan;
        int sisa = LEBAR - kiri.length() - 1;
        if (r.length() > sisa) r = potong(r, Math.max(1, sisa));
        int spasi = Math.max(1, LEBAR - kiri.length() - r.length());
        sb.append(kiri).append(" ".repeat(spasi)).append(r).append('\n');
    }

    private static String potong(String s, int maks) {
        return s.length() <= maks ? s : s.substring(0, Math.max(0, maks - 1)) + "…";
    }
}
