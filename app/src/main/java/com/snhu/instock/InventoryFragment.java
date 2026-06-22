package com.snhu.instock;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

/**
 * Displays the main inventory dashboard after the user logs in.
 * This fragment loads saved inventory items from SQLite, displays them in a grid,
 * and provides navigation to add, edit, delete, detail, and notification settings flows.
 */
public class InventoryFragment extends Fragment {
    private DatabaseHelper databaseHelper;
    private ArrayList<InventoryItem> inventoryItems;
    private InventoryAdapter inventoryAdapter;
    private TextView totalItemsValueTextView;
    private TextView lowStockValueTextView;

    public InventoryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up the database helper and list before the view is created so the
        // RecyclerView can use the same inventory collection when the screen loads.
        databaseHelper = new DatabaseHelper(requireContext());
        inventoryItems = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_inventory, container, false);

        Button addItemButton = view.findViewById(R.id.addItemButton);
        Button notificationsSettingsButton = view.findViewById(R.id.notificationSettingsButton);

        totalItemsValueTextView = view.findViewById(R.id.totalItemsValueTextView);
        lowStockValueTextView = view.findViewById(R.id.lowStockValueTextView);

        RecyclerView inventoryRecyclerView = view.findViewById(R.id.inventoryRecyclerView);
        inventoryRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // The adapter handles each row display, while this fragment handles the
        // larger dashboard actions for view details, edit, and delete.
        inventoryAdapter = new InventoryAdapter(
                inventoryItems,
                this::deleteInventoryItem,
                this::navigateToEditItem,
                this::navigateToItemDetail
        );

        inventoryRecyclerView.setAdapter(inventoryAdapter);

        // Loads the existing inventory records from the local database
        loadInventoryItems();

        // Navigates user to the add item form layout.
        addItemButton.setOnClickListener(v -> navigateToAddItem());

        // Opens the screen for the user to manage notifications settings
        notificationsSettingsButton.setOnClickListener(v -> navigateToNotificationsSettings());

        return view;
    }

    /**
     * Reloads the inventory list from SQLite and refreshes the dashboard summary cards.
     */
    private void loadInventoryItems() {
        inventoryItems.clear();
        inventoryItems.addAll(getInventoryItemsFromDatabase());

        inventoryAdapter.setInventoryItems(inventoryItems);
        updateSummaryCards();
    }

    /**
     * Converts the SQLite Cursor results into InventoryItem objects for the RecyclerView.
     */
    private ArrayList<InventoryItem> getInventoryItemsFromDatabase() {
        ArrayList<InventoryItem> itemsFromDb = new ArrayList<>();

        Cursor cursor = databaseHelper.getAllInventoryItems();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String itemName = cursor.getString(1);
                int quantity = cursor.getInt(2);
                String location = cursor.getString(3);
                int lowStockThreshold = cursor.getInt(4);
                String notes = cursor.getString(5);
                boolean lowStockAlertSent = cursor.getInt(6) == 1;

                InventoryItem item = new InventoryItem(id, itemName, quantity, location, lowStockThreshold, notes, lowStockAlertSent);
                itemsFromDb.add(item);

            } while (cursor.moveToNext());

            cursor.close();
        }

        return itemsFromDb;
    }

    private void navigateToAddItem() {
        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new AddItemFragment())
                .addToBackStack(null)
                .commit();
    }

    private void navigateToEditItem(InventoryItem item) {
        AddItemFragment editFragment = AddItemFragment.newEditInstance(item);

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, editFragment)
                .addToBackStack(null)
                .commit();
    }

    private void navigateToNotificationsSettings() {
        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new NotificationsFragment())
                .addToBackStack(null)
                .commit();
    }

    private void navigateToItemDetail(InventoryItem item) {
        ItemDetailFragment detailFragment = ItemDetailFragment.newInstance(item);

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, detailFragment)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Updates the dashboard summary cards based on the current inventory list.
     * Low-stock items are counted when their quantity is at or below the saved threshold.
     */
    private void updateSummaryCards() {
        int lowStockCount = 0;

        for (InventoryItem item : inventoryItems) {
            if (item.getQuantity() <= item.getLowStockThreshold()) {
                lowStockCount++;
            }
        }

        totalItemsValueTextView.setText(String.valueOf(inventoryItems.size()));
        lowStockValueTextView.setText(String.valueOf(lowStockCount));
    }

    /**
     * Deletes the selected item from SQLite and reloads the dashboard if successful.
     */
    private void deleteInventoryItem(InventoryItem item) {
        boolean itemDeleted = databaseHelper.deleteInventoryItem(item.getId());

        if (itemDeleted) {
            Toast.makeText(requireContext(), "Item deleted successfully", Toast.LENGTH_SHORT).show();
            loadInventoryItems();
        } else {
            Toast.makeText(requireContext(), "Failed to delete item. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }
}