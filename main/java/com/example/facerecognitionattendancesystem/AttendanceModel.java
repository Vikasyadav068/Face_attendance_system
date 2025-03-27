package com.example.facerecognitionattendancesystem;

public class AttendanceModel {
    private String userId;
    private String timestamp;

    public AttendanceModel(String userId, String timestamp) {
        this.userId = userId;
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
