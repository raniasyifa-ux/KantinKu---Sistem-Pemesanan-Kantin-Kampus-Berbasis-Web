package controllers;

import java.io.IOException;
import java.util.List;
import models.Pesanan;
import services.PesananBersama;

public final class AntrianController {
    public long penandaPesanan() {
        return PesananBersama.penandaPerubahan();
    }

    public List<Pesanan> bacaPesanan() throws IOException {
        return PesananBersama.baca();
    }
}
