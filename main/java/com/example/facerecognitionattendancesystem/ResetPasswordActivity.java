package com.example.facerecognitionattendancesystem;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class ResetPasswordActivity extends AppCompatActivity {

    private TextView gotoLogin;
    private EditText textEmail;
    private Button forgetBtn;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private static final String TAG = "ResetPasswordActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        // Initialize UI components
        gotoLogin = findViewById(R.id.goto_login);
        textEmail = findViewById(R.id.email);
        forgetBtn = findViewById(R.id.reset_btn);

        // Firebase Initialization
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Navigate to Login Page
        gotoLogin.setOnClickListener(v -> {
            startActivity(new Intent(ResetPasswordActivity.this, LoginActivity.class));
            finish();
        });

        // Forget Password Click
        forgetBtn.setOnClickListener(v -> validateData());
    }

    private void validateData() {
        String enteredEmail = textEmail.getText().toString().trim();

        if (enteredEmail.isEmpty()) {
            textEmail.setError("Email is required!");
            return;
        }

        checkEmailRegistered(enteredEmail);
    }

    private void checkEmailRegistered(String enteredEmail) {
        db.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean isRegistered = false;
                        String exactEmailMatch = null;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String storedEmail = document.getString("email");

                            if (storedEmail != null && storedEmail.equalsIgnoreCase(enteredEmail)) {
                                isRegistered = true;
                                exactEmailMatch = storedEmail; // Preserve correct case
                                break;
                            }
                        }

                        if (isRegistered && exactEmailMatch != null) {
                            sendPasswordResetEmail(exactEmailMatch);
                        } else {
                            Toast.makeText(ResetPasswordActivity.this, "This email is not registered. Please sign up first.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(ResetPasswordActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Firestore error: " + task.getException().getMessage());
                    }
                });
    }

    private void sendPasswordResetEmail(String email) {
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ResetPasswordActivity.this, "Check your email for reset link.", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(ResetPasswordActivity.this, LoginActivity.class));
                        finish();
                    } else {
                        Toast.makeText(ResetPasswordActivity.this, "Failed to send reset email. Try again!", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Reset email error: " + task.getException().getMessage());
                    }
                });
    }
}