package services;

import models.ItemKeranjang;
import models.Menu;
import java.util.ArrayList;
import java.util.List;

public class Keranjang {
    private List<ItemKeranjang> listItems;

    public Keranjang() {
        this.listItems = new ArrayList<>();
    }

    public void tambahItem(Menu menu, int qty) {
        for (ItemKeranjang item : listItems) {
            if (item.getMenu().getNama().equalsIgnoreCase(menu.getNama())) {
                item.setJumlah(item.getJumlah() + qty);
                return;
            }
        }
        listItems.add(new ItemKeranjang(menu, qty));
    }

    public void hapusItem(int index) {
        if (index >= 0 && index < listItems.size()) {
            listItems.remove(index);
        }
    }

    /** Mengubah jumlah item pada baris tertentu (minimal 1). */
    public void ubahJumlah(int index, int jumlahBaru) {
        if (index >= 0 && index < listItems.size() && jumlahBaru >= 1) {
            listItems.get(index).setJumlah(jumlahBaru);
        }
    }

    /** Jumlah porsi menu tertentu yang sudah ada di keranjang. */
    public int jumlahDi(String namaMenu) {
        for (ItemKeranjang item : listItems) {
            if (item.getMenu().getNama().equalsIgnoreCase(namaMenu)) {
                return item.getJumlah();
            }
        }
        return 0;
    }

    public void kosongkan() {
        listItems.clear();
    }

    public List<ItemKeranjang> getListItems() {
        return listItems;
    }

    public int getTotalHarga() {
        int total = 0;
        for (ItemKeranjang item : listItems) {
            total += item.getSubtotal();
        }
        return total;
    }

    public int getTotalQty() {
        int total = 0;
        for (ItemKeranjang item : listItems) {
            total += item.getJumlah();
        }
        return total;
    }
}
