package models;

public final class Pembeli {
    public String id;
    public String nama;
    public String hp;

    public Pembeli(String nama, String hp) {
        this(null, nama, hp);
    }

    public Pembeli(String id, String nama, String hp) {
        this.id = id;
        this.nama = nama;
        this.hp = hp == null ? "" : hp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }
    public String getHp() { return hp; }
    public void setHp(String hp) { this.hp = hp == null ? "" : hp; }
}
