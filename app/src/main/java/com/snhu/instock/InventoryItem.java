package com.snhu.instock;

/**
 * Represents one inventory item from the SQLite inventory table.
 * This model keeps the database fields together so the fragments and adapter
 * can pass item data around without working directly with Cursor values.
 */
public class InventoryItem {
    private final int id;
    private String itemName;
    private int qty;
    private final String location;
    private final int lowStockThreshold;
    private final String notes;
    private final boolean lowStockAlertSent;

    public InventoryItem(int id, String itemName, int qty, String location, int lowStockThreshold, String notes, boolean lowStockAlertSent) {
        this.id = id;
        this.itemName = itemName;
        this.qty = qty;
        this.location = location;
        this.lowStockThreshold = lowStockThreshold;
        this.notes = notes;
        this.lowStockAlertSent = lowStockAlertSent;
    }

    public int getId() {
        return id;
    }

    public String getItemName() {
        return itemName;
    }

    public int getQuantity() {
        return qty;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public void setQuantity(int qty) {
        this.qty = qty;
    }

    public String getLocation() {
        return location;
    }

    public int getLowStockThreshold() {
        return lowStockThreshold;
    }

    public String getNotes() {
        return notes;
    }

    public boolean hasLowStockAlertBeenSent() {
        return lowStockAlertSent;
    }
}
