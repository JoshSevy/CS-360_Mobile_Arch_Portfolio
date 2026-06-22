package com.snhu.instock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

/**
 * RecyclerView adapter for the inventory dashboard grid.
 * This class keeps the row display logic separate from InventoryFragment while
 * still letting the fragment handle actions like view details, edit, and delete.
 */
public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {
    private ArrayList<InventoryItem> inventoryItems;
    private final OnDeleteClickHandler deleteClickHandler;
    private final OnEditClickHandler editClickHandler;
    private final OnItemClickHandler itemClickHandler;
    public interface OnDeleteClickHandler {
        void onDeleteClick(InventoryItem item);
    }

    public interface OnEditClickHandler {
        void onEditClick(InventoryItem item);
    }

    public interface OnItemClickHandler {
        void onItemClick(InventoryItem item);
    }

    public InventoryAdapter(ArrayList<InventoryItem> inventoryItems, OnDeleteClickHandler deleteClickHandler, OnEditClickHandler editClickHandler, OnItemClickHandler itemClickHandler) {
        this.inventoryItems = inventoryItems;
        this.deleteClickHandler = deleteClickHandler;
        this.editClickHandler = editClickHandler;
        this.itemClickHandler = itemClickHandler;
    }

    /**
     * Inflates one inventory row layout and wraps it in a ViewHolder.
     * RecyclerView reuses these row views as the user scrolls through inventory items.
     */
    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item layout and create a new view holder
        View rowView = LayoutInflater.from(parent.getContext()).inflate(R.layout.inventory_item_row, parent, false);
        return new InventoryViewHolder(rowView);
    }

    /**
     * Binds one InventoryItem to the row UI and connects the row action buttons.
     */
    @Override
    public void onBindViewHolder(InventoryViewHolder holder, int position) {
        // Bind the inventory item data to the view holder
        InventoryItem item = inventoryItems.get(position);
        holder.itemNameTextView.setText(item.getItemName());
        holder.itemQuantityTextView.setText(String.valueOf(item.getQuantity()));
        holder.itemLocationTextView.setText(item.getLocation());

        holder.deleteItemButton.setOnClickListener(v -> {
            if (deleteClickHandler != null) {
                deleteClickHandler.onDeleteClick(item);
            }
        });

        holder.editItemButton.setOnClickListener(v -> {
            if (editClickHandler != null) {
                editClickHandler.onEditClick(item);
            }
        });

        // Tapping the row opens the detail screen so the grid can stay compact
        // while still giving access to notes, thresholds, and full item details.
        holder.itemView.setOnClickListener(v -> {
            if (itemClickHandler != null) {
                itemClickHandler.onItemClick(item);
            }
        });

        // Alternating row colors make the inventory grid easier to scan.
        if (position % 2 == 1) {
            holder.itemView.setBackgroundResource(R.color.grid_alt_row_bg);
        } else {
            holder.itemView.setBackgroundResource(android.R.color.transparent);
        }
    }

    @Override
    public int getItemCount() {
        return inventoryItems.size();
    }

    /**
     * Replaces the adapter data after the SQLite inventory records are reloaded.
     */
    public void setInventoryItems(ArrayList<InventoryItem> updatedItems) {
        this.inventoryItems = updatedItems;
        // TODO: Per IDE look into more performant ways to handle the change detection here.
        notifyDataSetChanged();
    }

    public static class InventoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView itemNameTextView;
        private final TextView itemQuantityTextView;
        private final TextView itemLocationTextView;
        private final Button deleteItemButton;
        private final Button editItemButton;

        public InventoryViewHolder(View itemView) {
            super(itemView);
            itemNameTextView = itemView.findViewById(R.id.itemNameTextView);
            itemQuantityTextView = itemView.findViewById(R.id.itemQuantityTextView);
            itemLocationTextView = itemView.findViewById(R.id.itemLocationTextView);
            deleteItemButton = itemView.findViewById(R.id.deleteItemButton);
            editItemButton = itemView.findViewById(R.id.editItemButton);
        }
    }
}
