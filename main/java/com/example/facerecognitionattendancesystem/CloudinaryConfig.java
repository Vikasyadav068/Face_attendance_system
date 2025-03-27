package com.example.facerecognitionattendancesystem;

import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;
import android.content.Context;

public class CloudinaryConfig {
    public static void init(Context context) {
        Map<String, Object> config = new HashMap<>();
        config.put("cloud_name", context.getString(R.string.cloud_name));
        config.put("api_key", context.getString(R.string.api_key));
        config.put("api_secret", context.getString(R.string.api_secret));

        MediaManager.init(context, config);
    }
}
