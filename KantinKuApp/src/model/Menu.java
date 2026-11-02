package model;

public class Menu {
    private String nama;
    private int harga;
    private String namaGambar;
    private String kategori;
    private String deskripsi;

    public Menu(String nama, int harga, String namaGambar, String kategori, String deskripsi) {
        this.nama = nama;
        this.harga = harga;
        this.namaGambar = namaGambar;
        this.kategori = kategori;
        this.deskripsi = deskripsi;
    }

    public String getNama() { return nama; }
    public int getHarga() { return harga; }
    public String getNamaGambar() { return namaGambar; }
    public String getKategori() { return kategori; }
    public String getDeskripsi() { return deskripsi; }
}
