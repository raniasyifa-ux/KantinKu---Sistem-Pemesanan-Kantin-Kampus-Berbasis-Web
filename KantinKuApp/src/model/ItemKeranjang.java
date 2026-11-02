package model;

public class ItemKeranjang {
    private Menu menu;
    private int jumlah;

    public ItemKeranjang(Menu menu, int jumlah) {
        this.menu = menu;
        this.jumlah = jumlah;
    }

    public Menu getMenu() { return menu; }
    public int getJumlah() { return jumlah; }
    public void setJumlah(int jumlah) { this.jumlah = jumlah; }

    public int getSubtotal() {
        return menu.getHarga() * jumlah;
    }
}
