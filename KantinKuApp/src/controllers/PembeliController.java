package controllers;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import models.Menu;
import models.Pesanan;
import services.KatalogBersama;
import services.PesananBersama;
import services.Struk;

public final class PembeliController {
    public long penandaKatalog() {
        return KatalogBersama.penanda();
    }

    public long penandaPesanan() {
        return PesananBersama.penandaPerubahan();
    }

    public Optional<List<Menu>> bacaMenuAktif() throws IOException {
        return KatalogBersama.bacaMenuAktif();
    }

    public int nomorBerikutnya() throws IOException {
        return PesananBersama.nomorBerikutnya();
    }

    public String formatId(int nomor) {
        return PesananBersama.formatId(nomor);
    }

    public String sekarang() {
        return PesananBersama.sekarang();
    }

    public Pesanan simpanPesanan(Pesanan pesanan, Map<String, Integer> permintaanStok)
            throws IOException {
        return PesananBersama.simpanBaru(pesanan, permintaanStok);
    }

    public String buatStruk(Pesanan pesanan) {
        return Struk.buat(pesanan);
    }

    public List<Pesanan> pesananPembeli(String nama) throws IOException {
        return PesananBersama.pesananDari(nama);
    }

    public List<Pesanan> bacaSemuaPesanan() throws IOException {
        return PesananBersama.baca();
    }

    public List<String> bacaIdPembeli() {
        return PesananBersama.bacaIdPembeli();
    }

    public Pesanan cariPesanan(String id) throws IOException {
        return PesananBersama.cari(id);
    }
}
