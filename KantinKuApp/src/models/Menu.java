package models;

public class Menu {
    private String id;
    private String nama;
    private int harga;
    private String namaGambar;
    private String kategori;
    private String deskripsi;
    private int stok;

    public static final int STOK_AWAL = 20;

    public Menu(String nama, int harga, String namaGambar, String kategori, String deskripsi) {
        this(nama.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "-"),
                nama, harga, namaGambar, kategori, deskripsi, STOK_AWAL);
    }

    public Menu(String nama, int harga, String namaGambar, String kategori, String deskripsi, int stok) {
        this(nama.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "-"),
                nama, harga, namaGambar, kategori, deskripsi, stok);
    }

    public Menu(String id, String nama, int harga, String namaGambar, String kategori,
            String deskripsi, int stok) {
        this.id = id;
        this.stok = stok;
        this.nama = nama;
        this.harga = harga;
        this.namaGambar = namaGambar;
        this.kategori = kategori;
        this.deskripsi = deskripsi;
    }

    public static String gambarBawaanUntukId(String id) {
        switch (id) {
            case "nasi-goreng": return "NASI GORENG.jpeg";
            case "mie-ayam": return "MIE AYAM.jpeg";
            case "pempek": return "PEMPEK.jpeg";
            case "siomay": return "SIOMAY.jpeg";
            case "batagor": return "BATAGOR.jpeg";
            case "risol-mayo": return "RISOL MAYO.jpeg";
            case "ayam-geprek": return "GEPREK.jpeg";
            case "soto-ayam": return "SOTO.jpeg";
            case "nasi-ayam-bakar": return "AYAM BAKAR.jpeg";
            case "kentang-goreng": return "KENTANG GORENG.jpeg";
            case "es-teh": return "ES TEH.jpeg";
            case "es-jeruk": return "ES JERUK.jpeg";
            case "es-milo": return "milo.jpeg";
            case "milky-regal": return "REGAL.jpeg";
            case "es-kopi-susu": return "KOPI SUSU.jpeg";
            case "thai-tea": return "THAI TEA.jpeg";
            case "matcha-latte": return "MATCHA.jpeg";
            case "cappuccino": return "CAPPUCINO.jpeg";
            case "chocolate": return "COKLAT.jpeg";
            case "jus-alpukat": return "ALPUKAT.jpeg";
            default: return "";
        }
    }

    public String getNama() { return nama; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getHarga() { return harga; }
    public String getNamaGambar() { return namaGambar; }
    public String getKategori() { return kategori; }
    public String getDeskripsi() { return deskripsi; }
    public int getStok() { return stok; }
}
