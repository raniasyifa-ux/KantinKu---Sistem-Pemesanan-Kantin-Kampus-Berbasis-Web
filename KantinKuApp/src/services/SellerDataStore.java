package services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import dao.impl.MenuDAOImpl;
import models.MenuItem;

/** Seller-facing catalog operations retained for compatibility with the existing UI. */
public final class SellerDataStore {
    private final MenuDAOImpl catalogRepository = new MenuDAOImpl();
    private final List<MenuItem> catalog;

    public SellerDataStore() throws IOException {
        catalog = new ArrayList<>();
        refreshCatalog();
    }

    public List<MenuItem> getCatalog() {
        try {
            refreshCatalog();
        } catch (IOException exception) {
            System.err.println("Katalog belum dapat disinkronkan: " + exception.getMessage());
        }
        return new ArrayList<>(catalog);
    }

    public int getActiveMenuCount() {
        return (int) getCatalog().stream().filter(MenuItem::isActive).count();
    }

    public MenuItem findById(String id) {
        for (MenuItem item : getCatalog()) {
            if (item.getId().equals(id)) {
                return item;
            }
        }
        return null;
    }

    public void addMenu(MenuItem item) throws IOException {
        catalogRepository.add(item);
        catalog.add(item);
    }

    public void updateMenu(MenuItem updated) throws IOException {
        catalogRepository.update(updated);
        for (int index = 0; index < catalog.size(); index++) {
            if (catalog.get(index).getId().equals(updated.getId())) {
                catalog.set(index, updated);
                return;
            }
        }
        catalog.add(updated);
    }

    public void setActive(String id, boolean active) throws IOException {
        catalogRepository.setActive(id, active);
        for (MenuItem item : catalog) {
            if (item.getId().equals(id)) {
                item.setActive(active);
                return;
            }
        }
    }

    public void removeMenu(String id) throws IOException {
        catalogRepository.delete(id);
        catalog.removeIf(item -> item.getId().equals(id));
    }

    public void saveMenu(MenuItem item) throws IOException {
        updateMenu(item);
    }

    public static MenuItem newMenu(String name, String description, int price, String category) {
        String imageName = category.equals("Makanan") ? "PEMPEK.jpeg" : "ES TEH.jpeg";
        return newMenu(name, description, price, category, imageName);
    }

    public static MenuItem newMenu(String name, String description, int price, String category,
            String imageName) {
        return new MenuItem(UUID.randomUUID().toString(), name, description, price, category, imageName, true);
    }

    private void refreshCatalog() throws IOException {
        catalog.clear();
        catalog.addAll(catalogRepository.readAll());
    }
}
