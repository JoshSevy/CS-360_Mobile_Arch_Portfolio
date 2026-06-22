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
 * Handles the login and local account creation flow for the app.
 * For this project, credentials are stored in the local SQLite database so the
 * user can create an account and return to the inventory dashboard.
 */
public class LoginFragment extends Fragment {

    private DatabaseHelper databaseHelper;
    private EditText usernameEditText;
    private EditText passwordEditText;


    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        databaseHelper = new DatabaseHelper(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the login layout for this fragment that supports the required username,
        // password, login button, and create account button for the login fragment
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        usernameEditText = view.findViewById(R.id.usernameEditText);
        passwordEditText = view.findViewById(R.id.passwordEditText);

        Button loginButton = view.findViewById(R.id.loginButton);
        Button createAccountButton = view.findViewById(R.id.createAccountButton);

        loginButton.setOnClickListener(v -> loginUser());
        createAccountButton.setOnClickListener(v -> createUserAccount());

        return view;
    }

    /**
     * Checks the entered username and password against the local database.
     */
    private void loginUser() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // short circuit if login form validation fails
        if (isLoginFormInvalid(username, password)) {
            return;
        }

        boolean loginSuccessful = databaseHelper.checkUser(username, password);

        if (loginSuccessful) {
            Toast.makeText(requireContext(), "Welcome!", Toast.LENGTH_SHORT).show();
            navigateToInventory();
        } else {
            Toast.makeText(requireContext(), "Invalid username or password. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Creates a local user account after validating that the username is not already taken.
     */
    private void createUserAccount() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Short circuit if login form is invalid
        if (isLoginFormInvalid(username, password)) {
            return;
        }

        if (databaseHelper.usernameExists(username)) {
            Toast.makeText(requireContext(), "Username already exists. Please choose a different username.", Toast.LENGTH_SHORT).show();
            usernameEditText.requestFocus();
            return;
        }

        boolean accountCreated = databaseHelper.addUser(username, password);

        if (accountCreated) {
            Toast.makeText(requireContext(), "Account created successfully! Welcome.", Toast.LENGTH_SHORT).show();
            navigateToInventory();
        } else {
            Toast.makeText(requireContext(), "Account creation failed. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Validates the required login fields before login or account creation.
     * I kept this check shared so both flows follow the same input rules.
     *
     * FixMe: Post Project: look into more verbose credential validations
     * FixMe: and how these are handled in commercial native applications
     */
    private boolean isLoginFormInvalid(String username, String password) {
        if (username.isEmpty()) {
            usernameEditText.setError("Username is required");
            usernameEditText.requestFocus();
            return true;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            return true;
        }

        return false;
    }

    /**
     *  After login or account creation navigate the user to the inventory screen
     */
    private void navigateToInventory() {
        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new InventoryFragment())
                .addToBackStack(null)
                .commit();
    }
}