package com.example.facerecognitionattendancesystem;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class AttendanceListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AttendanceAdapter adapter;
    private ArrayList<AttendanceModel> attendanceList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_list);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AttendanceAdapter(attendanceList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        fetchAttendanceHistory();
    }

    private void fetchAttendanceHistory() {
        db.collection("attendance")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    attendanceList.clear(); // List clear karo pehle

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String userId = doc.getString("userId");
                        Long timestamp = doc.getLong("timestamp");

                        if (userId != null && timestamp != null) {
                            String formattedDate = formatDate(timestamp);
                            attendanceList.add(new AttendanceModel(userId, formattedDate));
                        }
                    }

                    adapter.notifyDataSetChanged(); // UI update karo
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error Fetching Attendance: " + e.getMessage());
                    Toast.makeText(this, "⚠ Error Fetching Attendance", Toast.LENGTH_SHORT).show();
                });
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
