package service;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Penyimpanan pesanan yang dipakai bersama oleh aplikasi pembeli dan penjual.
 * Berkas dikunci saat ditulis sehingga aman walau pembeli dan penjual dijalankan
 * sebagai dua program terpisah di komputer yang sama.
 */
public final class PesananBersama {
    private static final Path FOLDER = Path.of(System.getProperty("user.home"), ".kantinku-desktop");
    private static final Path FILE = FOLDER.resolve("orders.tsv");
    private static final Path FILE_LOCK = FOLDER.resolve("orders.lock");
    private static final Path FILE_PEMBELI = FOLDER.resolve("orders-pembeli.txt");

    private PesananBersama() {}

    public static final class Pesanan {
        public String id, nama, metode, status, rincian;
        public int total;
        public Pesanan(String id, String nama, String metode, int total, String status, String rincian) {
            this.id=id; this.nama=nama; this.metode=metode; this.total=total; this.status=status; this.rincian=rincian;
        }
    }

    // ------------------------------------------------------------------ baca / tulis

    public static synchronized List<Pesanan> baca() throws IOException {
        return bacaTanpaKunci();
    }

    /** Menyimpan pesanan baru. Bila nomor pesanan sudah dipakai, nomor diganti dengan yang masih kosong. */
    public static synchronized Pesanan simpanBaru(Pesanan p) throws IOException {
        return dalamKunci(() -> {
            List<Pesanan> all = bacaTanpaKunci();
            boolean dipakai = false;
            int maks = 0;
            for (Pesanan o : all) {
                maks = Math.max(maks, nomorDari(o.id));
                if (o.id.equals(p.id)) dipakai = true;
            }
            if (dipakai) {
                p.id = formatId(maks + 1);
            }
            all.add(p);
            tulis(all);
            return p;
        });
    }

    public static synchronized void simpan(Pesanan p) throws IOException {
        dalamKunci(() -> {
            List<Pesanan> all = bacaTanpaKunci();
            boolean replaced = false;
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).id.equals(p.id)) { all.set(i, p); replaced = true; break; }
            }
            if (!replaced) all.add(p);
            tulis(all);
            return null;
        });
    }

    public static synchronized void ubahStatus(String id, String status) throws IOException {
        dalamKunci(() -> {
            List<Pesanan> all = bacaTanpaKunci();
            boolean ketemu = false;
            for (Pesanan p : all) {
                if (p.id.equals(id)) { p.status = status; ketemu = true; }
            }
            if (!ketemu) throw new IOException("Pesanan " + id + " tidak ditemukan.");
            tulis(all);
            return null;
        });
    }

    public static synchronized Pesanan cari(String id) throws IOException {
        for (Pesanan p : bacaTanpaKunci()) {
            if (p.id.equals(id)) return p;
        }
        return null;
    }

    /** Nomor urut pesanan berikutnya (tidak pernah menimpa pesanan yang sudah ada). */
    public static synchronized int nomorBerikutnya() throws IOException {
        int maks = 0;
        for (Pesanan p : bacaTanpaKunci()) {
            maks = Math.max(maks, nomorDari(p.id));
        }
        return maks + 1;
    }

    public static String formatId(int nomor) {
        return "#ORD-" + String.format("%03d", nomor);
    }

    private static int nomorDari(String id) {
        String digits = id == null ? "" : id.replaceAll("\\D", "");
        if (digits.isEmpty()) return 0;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ------------------------------------------------------------------ pesanan milik pembeli ini

    public static synchronized List<String> bacaIdPembeli() {
        List<String> ids = new ArrayList<>();
        try {
            if (Files.exists(FILE_PEMBELI)) {
                for (String line : Files.readAllLines(FILE_PEMBELI, StandardCharsets.UTF_8)) {
                    if (!line.isBlank()) ids.add(line.trim());
                }
            }
        } catch (IOException ignored) {
            // daftar kosong: pesanan lama tetap aman di orders.tsv
        }
        return ids;
    }

    public static synchronized void catatIdPembeli(String id) throws IOException {
        List<String> ids = bacaIdPembeli();
        if (!ids.contains(id)) {
            ids.add(id);
            Files.createDirectories(FOLDER);
            Files.write(FILE_PEMBELI, ids, StandardCharsets.UTF_8);
        }
    }

    /** Penanda perubahan berkas (untuk mendeteksi pesanan baru / status baru tanpa membaca isinya). */
    public static long penandaPerubahan() {
        try {
            return Files.exists(FILE) ? Files.getLastModifiedTime(FILE).toMillis() * 31 + Files.size(FILE) : -1;
        } catch (IOException e) {
            return -1;
        }
    }

    // ------------------------------------------------------------------ internal

    private interface Pekerjaan<T> {
        T jalankan() throws IOException;
    }

    private static <T> T dalamKunci(Pekerjaan<T> kerja) throws IOException {
        Files.createDirectories(FOLDER);
        try (RandomAccessFile raf = new RandomAccessFile(FILE_LOCK.toFile(), "rw");
             FileChannel channel = raf.getChannel();
             FileLock lock = channel.lock()) {
            return kerja.jalankan();
        }
    }

    private static List<Pesanan> bacaTanpaKunci() throws IOException {
        List<Pesanan> result = new ArrayList<>();
        if (!Files.exists(FILE)) return result;
        for (String line : Files.readAllLines(FILE, StandardCharsets.UTF_8)) {
            if (line.isBlank()) continue;
            String[] f = line.split("\t", -1);
            if (f.length != 6) continue;
            try {
                result.add(new Pesanan(dec(f[0]), dec(f[1]), dec(f[2]), Integer.parseInt(f[3]), dec(f[4]), dec(f[5])));
            } catch (RuntimeException ignored) {
                // baris rusak dilewati
            }
        }
        return result;
    }

    private static void tulis(List<Pesanan> all) throws IOException {
        Files.createDirectories(FOLDER);
        List<String> lines = new ArrayList<>();
        for (Pesanan p : all) {
            lines.add(String.join("\t", enc(p.id), enc(p.nama), enc(p.metode), Integer.toString(p.total), enc(p.status), enc(p.rincian)));
        }
        Path temp = Files.createTempFile(FOLDER, "orders-", ".tmp");
        try {
            Files.write(temp, lines, StandardCharsets.UTF_8);
            try {
                Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static String enc(String s) { return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8)); }
    private static String dec(String s) { return new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8); }
}
