package service;

import model.ItemKeranjang;
import model.Menu;
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
