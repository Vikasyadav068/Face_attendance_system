package com.example.facerecognitionattendancesystem;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private Button btnCapture, btnCheckAttendance;
    private Bitmap capturedImage;
    private FirebaseFirestore db;

    Button btnViewAttendance;
    private static final int CAMERA_REQUEST_CODE = 101;
    private static final int IMAGE_CAPTURE_CODE = 100;

    private Button  btnLogout;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mAuth = FirebaseAuth.getInstance();

        // Button Initialization
        btnCapture = findViewById(R.id.btnCapture);
        btnCheckAttendance = findViewById(R.id.btnCheckAttendance);
        btnViewAttendance = findViewById(R.id.btnViewAttendance);
        btnLogout = findViewById(R.id.btnLogout);  // Logout Button

        // Logout Button Click Listener
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut(); // Firebase Logout
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Prevents user from coming back to MainActivity after logout
        });

        btnViewAttendance = findViewById(R.id.btnViewAttendance);

        btnViewAttendance.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AttendanceListActivity.class);
            startActivity(intent);
        });


        imageView = findViewById(R.id.imageView);
        btnCapture = findViewById(R.id.btnCapture);
        btnCheckAttendance = findViewById(R.id.btnCheckAttendance);
        db = FirebaseFirestore.getInstance();

        // Camera Permission Check
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        }

        btnCapture.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera Permission Denied!", Toast.LENGTH_SHORT).show();
            }
        });

        btnCheckAttendance.setOnClickListener(v -> fetchAttendance());
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE);
        } else {
            Toast.makeText(this, "No Camera App Found!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMAGE_CAPTURE_CODE && resultCode == RESULT_OK) {
            if (data != null && data.getExtras() != null) {
                capturedImage = (Bitmap) data.getExtras().get("data");
                if (capturedImage != null) {
                    imageView.setImageBitmap(capturedImage);
                    detectFace(capturedImage);
                } else {
                    Toast.makeText(this, "Image Capture Failed!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void detectFace(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build();

        FaceDetector detector = FaceDetection.getClient(options);

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if (faces.size() > 0) {
                        saveFaceToFirestore(bitmap);
                    } else {
                        Toast.makeText(this, "No Face Detected!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FaceDetection", "Detection Failed: " + e.getMessage());
                    Toast.makeText(this, "Detection Failed!", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveFaceToFirestore(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageData = baos.toByteArray();
        String encodedImage = Base64.encodeToString(imageData, Base64.DEFAULT);

        Map<String, Object> attendance = new HashMap<>();
        attendance.put("userId", "USER123");
        attendance.put("timestamp", System.currentTimeMillis());
        attendance.put("image", encodedImage);

        db.collection("attendance").add(attendance)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "✅ Attendance Marked!", Toast.LENGTH_SHORT).show();
                    imageView.setImageDrawable(null);  // ImageView reset
//                    imageView.setVisibility(View.GONE); // Hide ImageView
                    capturedImage = null;  // Bitmap reset
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error Saving Attendance: " + e.getMessage());
                    Toast.makeText(this, "⚠ Error Saving Attendance", Toast.LENGTH_SHORT).show();
                });

    }

    private void fetchAttendance() {
        long currentTime = System.currentTimeMillis();
        long twentyFourHoursAgo = currentTime - (24 * 60 * 60 * 1000);

        db.collection("attendance")
                .whereEqualTo("userId", "USER123")
                .whereGreaterThan("timestamp", twentyFourHoursAgo)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING) // Latest first
                .limit(1) // Only fetch latest record
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Long timestamp = doc.getLong("timestamp");
                            String encodedImage = doc.getString("image"); // Image string from Firestore

                            if (timestamp != null && encodedImage != null) {
                                String formattedDate = formatDate(timestamp);
                                Toast.makeText(this, "✅ Latest Attendance: " + formattedDate, Toast.LENGTH_LONG).show();
                                imageView.setImageBitmap(decodeBase64(encodedImage)); // Show captured image
                            } else {
                                Toast.makeText(this, "⚠ Attendance Found but Image Missing!", Toast.LENGTH_SHORT).show();
                            }
                            return;
                        }
                    } else {
                        Toast.makeText(this, "⚠ No Attendance Marked in Last 24 Hours!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error Fetching Attendance: " + e.getMessage());
                    Toast.makeText(this, "⚠ Error Fetching Attendance", Toast.LENGTH_SHORT).show();
                });
    }
    private Bitmap decodeBase64(String encodedImage) {
        byte[] decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }


    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera Permission Granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Camera Permission Denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
