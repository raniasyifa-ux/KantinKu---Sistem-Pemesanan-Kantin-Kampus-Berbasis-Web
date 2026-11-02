package id.kantinku.desktop;

final class MenuItem {
    private final String id;
    private String name;
    private String description;
    private int price;
    private String category;
    private String imageName;
    private boolean active;

    MenuItem(String id, String name, String description, int price, String category, boolean active) {
        this(id, name, description, price, category, "", active);
    }

    MenuItem(String id, String name, String description, int price, String category, String imageName,
            boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.imageName = imageName;
        this.active = active;
    }

    String getId() {
        return id;
    }

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    String getDescription() {
        return description;
    }

    void setDescription(String description) {
        this.description = description;
    }

    int getPrice() {
        return price;
    }

    void setPrice(int price) {
        this.price = price;
    }

    String getCategory() {
        return category;
    }

    void setCategory(String category) {
        this.category = category;
    }

    String getImageName() {
        return imageName;
    }

    void setImageName(String imageName) {
        this.imageName = imageName;
    }

    boolean isActive() {
        return active;
    }

    void setActive(boolean active) {
        this.active = active;
    }
}
