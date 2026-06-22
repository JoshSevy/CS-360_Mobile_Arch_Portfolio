package com.snhu.instock;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * DatabaseHelper manages the local SQLite database for the app.
 * This keeps the user login data and inventory data in one persistent database
 * so the app can still access saved information after it closes.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "instock.db";
    private static final int DATABASE_VERSION = 2;

    // Users table stores account credentials for the local login flow.
    private static final String TABLE_USERS = "users";
    private static final String USER_ID = "id";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    // Inventory table stores the item data displayed in the dashboard grid.
    private static final String TABLE_INVENTORY = "inventory";
    private final String ITEM_ID = "id";
    private static final String ITEM_NAME = "item_name";
    private static final String ITEM_QUANTITY = "quantity";
    private static final String ITEM_LOCATION = "location";
    private static final String ITEM_LOW_STOCK_THRESHOLD = "low_stock_threshold";
    private static final String ITEM_NOTES = "notes";
    private static final String ITEM_ALERT_SENT = "low_stock_alert_sent";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Creates the database tables when the database is first created.
     * The users table supports login/account creation, and the inventory table
     * supports the CRUD functionality required for the inventory dashboard.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + USERNAME + " TEXT UNIQUE NOT NULL, "
                + PASSWORD + " TEXT NOT NULL)";
        db.execSQL(CREATE_USERS_TABLE);

        String CREATE_INVENTORY_TABLE = "CREATE TABLE " + TABLE_INVENTORY + "("
                + ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ITEM_NAME + " TEXT NOT NULL, "
                + ITEM_QUANTITY + " INTEGER NOT NULL, "
                + ITEM_LOCATION + " TEXT DEFAULT '', "
                + ITEM_LOW_STOCK_THRESHOLD + " INTEGER DEFAULT 0, "
                + ITEM_NOTES + " TEXT DEFAULT '', "
                + ITEM_ALERT_SENT + " INTEGER DEFAULT 0)";
        db.execSQL(CREATE_INVENTORY_TABLE);
    }

    /**
     * Rebuilds the database when the schema version changes.
     * For this class project, dropping and recreating the tables keeps the upgrade
     * logic simple while testing database changes in the emulator.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INVENTORY);
        onCreate(db);
    }

    /**
     * Adds a new user to the local users table.
     */
    public boolean addUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(USERNAME, username);
        values.put(PASSWORD, password);

        long result = db.insert(TABLE_USERS, null, values);
        db.close();

        return result != -1;
    }

    /**
     * Checks if username and password match an existing user.
     */
    public boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_USERS + " WHERE " + USERNAME + " = ? AND " + PASSWORD + " = ?";

        Cursor cursor = db.rawQuery(query, new String[]{username, password});

        boolean userExists = cursor.getCount() > 0;

        cursor.close();
        db.close();

        return userExists;
    }

    /**
     * Checks if username already exists in database before new account creation.
     */
    public boolean usernameExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_USERS + " WHERE " + USERNAME + " = ?";

        Cursor cursor = db.rawQuery(query, new String[]{username});

        boolean exists = cursor.getCount() > 0;

        cursor.close();
        db.close();

        return exists;
    }

    /**
     * Adds an inventory item and returns the new row ID created by SQLite.
     * Returning the ID lets the app immediately check the new item for low-stock
     * SMS alerts without needing another database lookup.
     */
    public long addInventoryItem(String itemName, int qty, String location, int lowStockThreshold, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(ITEM_NAME, itemName);
        values.put(ITEM_QUANTITY, qty);
        values.put(ITEM_LOCATION, location);
        values.put(ITEM_LOW_STOCK_THRESHOLD, lowStockThreshold);
        values.put(ITEM_NOTES, notes);
        values.put(ITEM_ALERT_SENT, 0);

        long newItemId= db.insert(TABLE_INVENTORY, null, values);
        db.close();

        return newItemId;
    }

    /**
     * Returns all saved inventory items so the dashboard can display them in the grid.
     */
    public Cursor getAllInventoryItems() {
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_INVENTORY + " ORDER BY " + ITEM_NAME + " ASC";

        return db.rawQuery(query, null);
    }

    /**
     * Updates an existing inventory item.
     * If the item is restocked above its low-stock threshold, the SMS alert flag
     * is reset so a future low-stock event can trigger a new alert.
     */
    public boolean updateInventoryItem(int itemId, String itemName, int qty, String location, int lowStockThreshold, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(ITEM_NAME, itemName);
        values.put(ITEM_QUANTITY, qty);
        values.put(ITEM_LOCATION, location);
        values.put(ITEM_LOW_STOCK_THRESHOLD, lowStockThreshold);
        values.put(ITEM_NOTES, notes);

        // If item qty is updated above lowStockThreshold reset alert sent
        if (qty > lowStockThreshold) {
            values.put(ITEM_ALERT_SENT, 0);
        }

        int rowsUpdated = db.update(
                TABLE_INVENTORY,
                values,
                ITEM_ID + " = ?",
                new String[]{String.valueOf(itemId)}
        );

        db.close();

        return rowsUpdated > 0;
    }

    /**
     * Deletes one inventory item from the database using its unique item ID.
     */
    public boolean deleteInventoryItem(int itemId) {
        SQLiteDatabase db = this.getWritableDatabase();

        int rowsDeleted = db.delete(
                TABLE_INVENTORY,
                ITEM_ID + " = ?",
                new String[]{String.valueOf(itemId)}
        );

        db.close();

        return rowsDeleted > 0;
    }

    /**
     * Updates whether a low-stock SMS alert has already been sent for an item.
     * This prevents the same item from sending repeated alerts while it remains low.
     */
    public boolean updateLowStockAlertSent(int itemId, boolean alertSent) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(ITEM_ALERT_SENT, alertSent ? 1 : 0);

        int rowsUpdated = db.update(
                TABLE_INVENTORY,
                values,
                ITEM_ID + " = ?",
                new String[]{String.valueOf(itemId)}
        );

        db.close();

        return rowsUpdated > 0;
    }
}
