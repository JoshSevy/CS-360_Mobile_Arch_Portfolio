package com.snhu.instock;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Handles the add and edit item form for inventory records.
 * I reused this fragment for both creating and updating items so the app does
 * not need two separate forms with nearly identical fields.
 */
public class AddItemFragment extends Fragment {

    private DatabaseHelper databaseHelper;
    private EditText itemNameEditText;
    private EditText itemQuantityEditText;
    private EditText itemLocationEditText;
    private EditText lowStockThresholdEditText;
    private EditText itemNotesEditText;

    private static final String ARG_ITEM_ID = "item_id";
    private static final String ARG_ITEM_NAME = "item_name";
    private static final String ARG_ITEM_QTY = "item_quantity";
    private static final String ARG_ITEM_LOCATION = "item_location";
    private static final String ARG_LOW_STOCK_THRESHOLD = "low_stock_threshold";
    private static final String ARG_ITEM_NOTES = "item_notes";

    private boolean isEditMode = false;
    private int itemId = -1;

    public AddItemFragment() {
        // Required empty public constructor
    }

    /**
     * Creates the fragment in edit mode by passing the selected item values
     * through a Bundle. This allows the same form to be prefilled for updates.
     */
    public static AddItemFragment newEditInstance(InventoryItem item) {
        AddItemFragment fragment = new AddItemFragment();
        Bundle args = new Bundle();

        args.putInt(ARG_ITEM_ID, item.getId());
        args.putString(ARG_ITEM_NAME, item.getItemName());
        args.putInt(ARG_ITEM_QTY, item.getQuantity());
        args.putString(ARG_ITEM_LOCATION, item.getLocation());
        args.putInt(ARG_LOW_STOCK_THRESHOLD, item.getLowStockThreshold());
        args.putString(ARG_ITEM_NOTES, item.getNotes());

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Creates connection to local SQLite database for add/update operations.
        databaseHelper = new DatabaseHelper(requireContext());

        if (getArguments() != null && getArguments().containsKey(ARG_ITEM_ID)) {
            isEditMode = true;
            itemId = getArguments().getInt(ARG_ITEM_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the add item form users will use to add items to the inventory records
        View view = inflater.inflate(R.layout.fragment_add_item, container, false);

        itemNameEditText = view.findViewById(R.id.itemNameEditText);
        itemQuantityEditText = view.findViewById(R.id.itemQuantityEditText);
        itemLocationEditText = view.findViewById(R.id.itemLocationEditText);
        lowStockThresholdEditText = view.findViewById(R.id.lowStockThresholdEditText);
        itemNotesEditText = view.findViewById(R.id.itemNotesEditText);

        Button saveItemButton = view.findViewById(R.id.saveItemButton);
        Button cancelAddItemButton = view.findViewById(R.id.cancelAddItemButton);

        if (isEditMode) {
            populateEditFields();
            saveItemButton.setText(R.string.update_item);
        }

        // Save adds item to SQLite lcoal database
        saveItemButton.setOnClickListener(v -> saveInventoryItem());

        // Cancel closes the form and returns user to inventory dashboard
        cancelAddItemButton.setOnClickListener(v -> navigateToInventory());

        return view;
    }

    /**
     * Prefills the form fields when the user is editing an existing inventory item.
     */
    private void populateEditFields() {
        Bundle args = getArguments();

        if (args == null) {
            return;
        }

        itemNameEditText.setText(args.getString(ARG_ITEM_NAME, ""));
        itemQuantityEditText.setText(String.valueOf(args.getInt(ARG_ITEM_QTY, 0)));
        itemLocationEditText.setText(args.getString(ARG_ITEM_LOCATION, ""));
        lowStockThresholdEditText.setText(String.valueOf(args.getInt(ARG_LOW_STOCK_THRESHOLD, 0)));
        itemNotesEditText.setText(args.getString(ARG_ITEM_NOTES, ""));
    }

    /**
     * Validates the form, saves the item to SQLite, and checks whether the saved
     * item should trigger a low-stock SMS alert.
     */
    private void saveInventoryItem() {
        String itemName = itemNameEditText.getText().toString().trim();
        String qtyText = itemQuantityEditText.getText().toString().trim();

        // Add input validation when saving inventory items
        if (itemName.isEmpty()) {
            itemNameEditText.setError("Item name is required");
            itemNameEditText.requestFocus();
            return;
        }

        if (qtyText.isEmpty()) {
            itemQuantityEditText.setError("Quantity is required");
            itemQuantityEditText.requestFocus();
            return;
        }

        int qty;

        try {
            qty = Integer.parseInt(qtyText);
        } catch (NumberFormatException error) {
            itemQuantityEditText.setError("Quantity must be a number");
            itemQuantityEditText.requestFocus();
            return;
        }

        if (qty < 0) {
            itemQuantityEditText.setError("Quantity cannot be a negative number");
            itemQuantityEditText.requestFocus();
            return;
        }

        // Location and notes are optional fields, so blank values are saved as empty strings.
        String location = itemLocationEditText.getText().toString().trim();
        String lowStockThresholdText = lowStockThresholdEditText.getText().toString().trim();
        String notes = itemNotesEditText.getText().toString().trim();

        // Optional fields default to an empty string if the user leaves them blank.
        if (location.isEmpty()) {
            location = "";
        }

        if (notes.isEmpty()) {
            notes = "";
        }

        int lowStockThreshold = 0;

        if (!lowStockThresholdText.isEmpty()) {
            try {
                lowStockThreshold = Integer.parseInt(lowStockThresholdText);
            } catch (NumberFormatException error) {
                lowStockThresholdEditText.setError("Low stock threshold must be a number");
                lowStockThresholdEditText.requestFocus();
                return;
            }
        }

        boolean itemSaved;
        long savedItemId = itemId;

        if (isEditMode) {
            itemSaved = databaseHelper.updateInventoryItem(itemId, itemName, qty, location, lowStockThreshold, notes);
        } else {
            savedItemId = databaseHelper.addInventoryItem(itemName, qty, location, lowStockThreshold, notes);

            itemSaved = savedItemId != -1;
        }

        if (itemSaved) {
            // Cast itemId to int from long to resolve type change
            InventoryItem savedItem = new InventoryItem(
                    (int) savedItemId,
                    itemName,
                    qty,
                    location,
                    lowStockThreshold,
                    notes,
                    false
            );

            // SMS alert logic is handled in a helper so this fragment can stay focused
            // on the form flow while still showing the user when an alert was triggered.
            boolean smsAlertTriggered = SmsAlertHelper.sendLowInventoryAlertIfNeeded(requireContext(), databaseHelper, savedItem);

            String message;

            if (isEditMode && smsAlertTriggered) {
                message = "Item updated successfully. Low-stock SMS alert triggered.";
            } else if (isEditMode) {
                message = "Item updated successfully.";
            } else if (smsAlertTriggered) {
                message = "Item added successfully. Low-stock SMS alert triggered.";
            } else {
                message = "Item added successfully.";
            }

            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            navigateToInventory();
        } else {
            String message = isEditMode ? "Failed to update item. Please try again." : "Failed to add item. Please try again.";
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
    private void navigateToInventory() {
        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new InventoryFragment())
                .addToBackStack(null)
                .commit();
    }
}