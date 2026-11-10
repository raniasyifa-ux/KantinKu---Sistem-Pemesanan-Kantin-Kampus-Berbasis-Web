package controllers;

import java.io.IOException;
import java.util.List;
import models.MenuItem;
import models.Pesanan;
import services.KatalogBersama;
import services.PesananBersama;
import services.SellerAuthService;
import services.SellerDataStore;

public final class PenjualController {
    private final SellerAuthService authService;
    private final SellerDataStore dataStore;

    public PenjualController(SellerAuthService authService, SellerDataStore dataStore) {
        this.authService = authService;
        this.dataStore = dataStore;
    }

    public String authenticate(String usernameOrEmail, char[] password) throws IOException {
        return authService.authenticate(usernameOrEmail, password);
    }

    public void register(String username, String email, char[] password) throws IOException {
        authService.register(username, email, password);
    }

    public void resetPassword(String email, char[] password) throws IOException {
        authService.resetPassword(email, password);
    }

    public List<MenuItem> bacaMenu() {
        return dataStore.getCatalog();
    }

    public MenuItem cariMenu(String id) {
        return dataStore.findById(id);
    }

    public void tambahMenu(MenuItem menu) throws IOException {
        dataStore.addMenu(menu);
    }

    public void simpanMenu(MenuItem menu) throws IOException {
        dataStore.saveMenu(menu);
    }

    public void ubahStatusMenu(String id, boolean aktif) throws IOException {
        dataStore.setActive(id, aktif);
    }

    public void hapusMenu(String id) throws IOException {
        dataStore.removeMenu(id);
    }

    public MenuItem buatMenu(String nama, String deskripsi, int harga, String kategori,
            String namaGambar) {
        return SellerDataStore.newMenu(nama, deskripsi, harga, kategori, namaGambar);
    }

    public long penandaKatalog() {
        return KatalogBersama.penanda();
    }

    public long penandaPesanan() {
        return PesananBersama.penandaPerubahan();
    }

    public List<Pesanan> bacaPesanan() throws IOException {
        return PesananBersama.baca();
    }

    public void ubahStatusPesanan(String id, String status) throws IOException {
        PesananBersama.ubahStatus(id, status);
    }
}
