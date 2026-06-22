package com.snhu.instock;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * Displays the full details for a selected inventory item.
 * I added this screen so the inventory grid can stay simple while still giving
 * the user access to optional fields like location, threshold, and notes.
 */
public class ItemDetailFragment extends Fragment {

    private static final String ARG_ITEM_ID = "item_id";
    private static final String ARG_ITEM_NAME = "item_name";
    private static final String ARG_ITEM_QUANTITY = "item_quantity";
    private static final String ARG_ITEM_LOCATION = "item_location";
    private static final String ARG_LOW_STOCK_THRESHOLD = "low_stock_threshold";
    private static final String ARG_ITEM_NOTES = "item_notes";
    private static final String ARG_LOW_STOCK_ALERT_SENT = "low_stock_alert_sent";
    private InventoryItem inventoryItem;

    public ItemDetailFragment() {
        // Required empty public constructor
    }

    public static ItemDetailFragment newInstance(InventoryItem item) {
        ItemDetailFragment fragment = new ItemDetailFragment();
        Bundle args = new Bundle();

        // Store the selected item values as fragment arguments so the detail
        // screen has the data it needs when Android creates the fragment.
        args.putInt(ARG_ITEM_ID, item.getId());
        args.putString(ARG_ITEM_NAME, item.getItemName());
        args.putInt(ARG_ITEM_QUANTITY, item.getQuantity());
        args.putString(ARG_ITEM_LOCATION, item.getLocation());
        args.putInt(ARG_LOW_STOCK_THRESHOLD, item.getLowStockThreshold());
        args.putString(ARG_ITEM_NOTES, item.getNotes());
        args.putBoolean(ARG_LOW_STOCK_ALERT_SENT, item.hasLowStockAlertBeenSent());

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();

        if (args != null) {
            inventoryItem = new InventoryItem(
                    args.getInt(ARG_ITEM_ID),
                    args.getString(ARG_ITEM_NAME, ""),
                    args.getInt(ARG_ITEM_QUANTITY, 0),
                    args.getString(ARG_ITEM_LOCATION, ""),
                    args.getInt(ARG_LOW_STOCK_THRESHOLD, 0),
                    args.getString(ARG_ITEM_NOTES, ""),
                    args.getBoolean(ARG_LOW_STOCK_ALERT_SENT, false)
            );
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_item_detail, container, false);

        TextView itemNameTextView = view.findViewById(R.id.detailItemNameTextView);
        TextView quantityTextView = view.findViewById(R.id.detailQuantityTextView);
        TextView locationTextView = view.findViewById(R.id.detailLocationTextView);
        TextView thresholdTextView = view.findViewById(R.id.detailThresholdTextView);
        TextView notesTextView = view.findViewById(R.id.detailNotesTextView);

        Button editButton = view.findViewById(R.id.detailEditButton);
        Button backButton = view.findViewById(R.id.detailBackButton);

        if (inventoryItem != null) {
            // Location and notes are optional, so show a friendly fallback
            // instead of leaving the detail screen with blank values.
            String location = inventoryItem.getLocation().isEmpty() ? "No location added." : inventoryItem.getLocation();
            String notes = inventoryItem.getNotes().isEmpty() ? "No notes added." : inventoryItem.getNotes();

            itemNameTextView.setText(inventoryItem.getItemName());
            quantityTextView.setText(String.format("Quantity: %s", inventoryItem.getQuantity()));
            locationTextView.setText(String.format("Location: %s", location));
            thresholdTextView.setText(String.format("Low Stock Threshold: %s", inventoryItem.getLowStockThreshold()));
            notesTextView.setText(String.format("Notes: %s", notes));
        }

        editButton.setOnClickListener(v -> navigateToEditItem());
        backButton.setOnClickListener(v -> navigateBackToInventory());

        return view;
    }

    private void navigateToEditItem() {
        // Reuse the add item form in edit mode instead of maintaining a second
        // nearly identical form for updates.
        AddItemFragment editFragment = AddItemFragment.newEditInstance(inventoryItem);

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, editFragment)
                .addToBackStack(null)
                .commit();
    }

    private void navigateBackToInventory() {
        requireActivity()
                .getSupportFragmentManager()
                .popBackStack();
    }
}