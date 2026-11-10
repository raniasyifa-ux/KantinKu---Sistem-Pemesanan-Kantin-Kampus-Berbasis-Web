package models;

import models.Menu;

public final class MenuItem {
    private String id;
    private String name;
    private String description;
    private int price;
    private String category;
    private String imageName;
    private boolean active;
    private int stock = Menu.STOK_AWAL;

    public MenuItem(String id, String name, String description, int price, String category, boolean active) {
        this(id, name, description, price, category, "", active);
    }

    public MenuItem(String id, String name, String description, int price, String category, String imageName,
            boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.imageName = imageName;
        this.active = active;
    }

    public MenuItem(String id, String name, String description, int price, String category, String imageName,
            boolean active, int stock) {
        this(id, name, description, price, category, imageName, active);
        this.stock = stock;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = Math.max(0, stock);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
