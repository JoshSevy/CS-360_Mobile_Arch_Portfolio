package com.snhu.instock;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

/**
 * Handles low-stock SMS alert logic for inventory items.
 * Keeping this logic in a helper class prevents the inventory and add item fragments
 * from being responsible for permission checks, SMS preferences, and sending messages.
 */
public class SmsAlertHelper {

    private static final String TAG = "SMS_ALERT";

    /**
     * Checks whether an inventory item should trigger a low-stock SMS alert.
     * An alert is only attempted when SMS alerts are enabled, permission has been granted,
     * the item is at or below its threshold, and an alert has not already been sent.
     *
     * @return true when the SMS alert is successfully triggered; otherwise false.
     */
    public static boolean sendLowInventoryAlertIfNeeded(
            Context context,
            DatabaseHelper databaseHelper,
            InventoryItem item
    ) {
        boolean itemIsLowStock = item.getQuantity() <= item.getLowStockThreshold();

        if (!itemIsLowStock || item.hasLowStockAlertBeenSent()) {
            return false;
        }

        SharedPreferences sharedPreferences = context.getSharedPreferences(
                NotificationsFragment.PREFS_NAME,
                Context.MODE_PRIVATE
        );

        boolean smsEnabled = sharedPreferences.getBoolean(
                NotificationsFragment.KEY_SMS_ENABLED,
                false
        );

        String phoneNumber = sharedPreferences.getString(
                NotificationsFragment.KEY_SMS_PHONE_NUMBER,
                ""
        );

        if (!smsEnabled || phoneNumber.isEmpty()) {
            return false;
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        String message = "inStock Alert: " + item.getItemName()
                + " is low. Current quantity: " + item.getQuantity();

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);

            // Mark the item so the app does not send repeat alerts while it remains low.
            boolean alertFlagUpdated = databaseHelper.updateLowStockAlertSent(item.getId(), true);

            // Log a warning if the SMS was sent but the database flag was not updated.
            if (!alertFlagUpdated) {
                Log.d(TAG, "SMS alert sent, but alert flag was not updated for " + item.getItemName());
            }

            Log.d(TAG, "Low inventory SMS alert triggered for " + item.getItemName());

            return true;

        } catch (Exception error) {
            Log.e(TAG, "SMS alert could not be sent", error);
            return false;
        }
    }
}