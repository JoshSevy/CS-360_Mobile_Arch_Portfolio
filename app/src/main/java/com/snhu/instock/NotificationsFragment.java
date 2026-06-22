package com.snhu.instock;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Handles the SMS notification settings for low-stock inventory alerts.
 * SMS alerts are optional, so the user can enable or disable them without
 * blocking the main inventory tracking features of the app.
 */
public class NotificationsFragment extends Fragment {

    public static final String PREFS_NAME = "inStockPreferences";
    public static final String KEY_SMS_ENABLED = "sms_enabled";
    public static final String KEY_SMS_PHONE_NUMBER = "sms_phone_number";
    private Button smsToggleButton;
    private EditText phoneNumberEditText;
    private SharedPreferences sharedPreferences;

    private ActivityResultLauncher<String> requestPermissionLauncher;

    public NotificationsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register the permission launcher once when the fragment is created.
        // The result updates the saved SMS preference based on the user's choice.
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        saveSmsPreferences(true);
                        updateSmsToggleButton();
                        Toast.makeText(requireContext(), "SMS alerts enabled", Toast.LENGTH_SHORT).show();

                        // Navigate after the permission result is handled so the saved preference
                        // reflects the user's device-level permission choice.
                        navigateToInventoryDashboard();
                    } else {
                        saveSmsPreferences(false);
                        updateSmsToggleButton();
                        Toast.makeText(requireContext(), "SMS alerts permission denied, Inventory tracking will still work.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the SMS notification settings layout and load saved preferences
        // so the screen reflects the user's current alert choice.
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        TextView permissionStatusTextView = view.findViewById(R.id.permissionStatusTextView);

        phoneNumberEditText = view.findViewById(R.id.phoneNumberEditText);
        smsToggleButton = view.findViewById(R.id.enableSmsAlertsButton);
        Button maybeLaterButton = view.findViewById(R.id.maybeLaterButton);

        updateSmsToggleButton();

        // Show the current SMS alert status based on saved SharedPreferences.
        if (sharedPreferences.getBoolean(KEY_SMS_ENABLED, false)) {
            permissionStatusTextView.setText(String.format("%s%s", getString(R.string.sms_alerts_enabled), sharedPreferences.getString(KEY_SMS_PHONE_NUMBER, "")));
        } else {
            permissionStatusTextView.setText(R.string.sms_alerts_disabled);
        }

        String savedPhoneNumber = sharedPreferences.getString(KEY_SMS_PHONE_NUMBER, "");
        phoneNumberEditText.setText(savedPhoneNumber);

        // Let the toggle flow decide when to navigate so Android permission results
        // can finish before leaving the notification screen.
        smsToggleButton.setOnClickListener(v -> handleSmsToggle());

        // Allow user to navigate back to inventory dashboard without enabling SMS alerts.
        // inStock remains fully functional regardless of alerts enabled or disabled.
        maybeLaterButton.setOnClickListener(v -> navigateToInventoryDashboard());

        return view;
    }

    /**
     * Validates the phone number field, saves it, and requests SEND_SMS permission when needed.
     */
    private void requestSmsPermission() {
        String phoneNumber = phoneNumberEditText.getText().toString().trim();

        if (phoneNumber.isEmpty()) {
            phoneNumberEditText.setError("Please enter a valid sms enabled phone number");
            phoneNumberEditText.requestFocus();
            return;
        }

        sharedPreferences.edit().putString(KEY_SMS_PHONE_NUMBER, phoneNumber).apply();

        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.SEND_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            saveSmsPreferences(true);
            updateSmsToggleButton();
            Toast.makeText(requireContext(), "SMS alerts enabled", Toast.LENGTH_SHORT).show();

            // Permission was already granted, so the app can save the setting and return
            // to the inventory dashboard immediately.
            navigateToInventoryDashboard();
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.SEND_SMS);
        }
    }

    private void saveSmsPreferences(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_SMS_ENABLED, enabled).apply();
    }

    private void navigateToInventoryDashboard() {
        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new InventoryFragment())
                .commit();
    }

    /**
     * Updates the SMS toggle button so it matches the saved alert preference.
     */
    private void updateSmsToggleButton() {
        boolean smsEnabled = sharedPreferences.getBoolean(KEY_SMS_ENABLED, false);

        if (smsEnabled) {
            smsToggleButton.setText(R.string.disabled_sms_alerts);
        } else {
            smsToggleButton.setText(R.string.enable_sms_alerts);
        }
    }

    /**
     * Turns off SMS alerts while keeping the saved phone number for easier re-enabling later.
     */
    private void disableSmsAlerts() {
        sharedPreferences.edit().putBoolean(KEY_SMS_ENABLED, false).apply();

        updateSmsToggleButton();

        Toast.makeText(requireContext(), "SMS alerts disabled. Inventory tracking will still work.", Toast.LENGTH_SHORT).show();

        // Disabling does not need a device-level permission result, so it can return
        // to the dashboard after updating the saved preference.
        navigateToInventoryDashboard();
    }

    /**
     * Uses the saved preference to decide whether the button should enable or disable SMS alerts.
     */
    private void handleSmsToggle() {
        boolean smsEnabled = sharedPreferences.getBoolean(KEY_SMS_ENABLED, false);

        if (smsEnabled) {
            disableSmsAlerts();
        } else {
            requestSmsPermission();
        }
    }
}