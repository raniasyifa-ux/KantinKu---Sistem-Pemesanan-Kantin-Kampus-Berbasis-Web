package models;

public final class DetailPesanan {
    private final String menuId;
    private final String menuName;
    private final int quantity;
    private final int unitPrice;
    private final int subtotal;
    private final String note;

    public DetailPesanan(String menuId, String menuName, int quantity, int unitPrice, int subtotal, String note) {
        this.menuId = menuId;
        this.menuName = menuName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
        this.note = note == null ? "" : note;
    }

    public String getMenuId() { return menuId; }
    public String getMenuName() { return menuName; }
    public int getQuantity() { return quantity; }
    public int getUnitPrice() { return unitPrice; }
    public int getSubtotal() { return subtotal; }
    public String getNote() { return note; }
}
