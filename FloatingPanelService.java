package com.example.floatingpanel;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;
import org.json.JSONException;

/**
 * FloatingPanelService - A professional floating menu overlay service for Android.
 * 
 * This service creates a system-level floating panel that:
 * - Floats above other applications using WindowManager
 * - Fetches and displays configuration from GitHub
 * - Handles user interactions and graceful shutdown
 * - Respects Android permission requirements
 */
public class FloatingPanelService extends Service {

    private static final String TAG = "FloatingPanelService";
    
    // GitHub configuration constants
    private static final String GITHUB_RAW_URL = 
        "https://raw.githubusercontent.com/eglisianhoxha-ctrl/Albanian-Panel/main/panel_config.json";
    private static final int NETWORK_TIMEOUT = 10000; // 10 seconds
    
    // UI Components
    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams layoutParams;
    
    // UI References
    private TextView statusTextView;
    private TextView versionTextView;
    private TextView maintenanceTextView;
    private Button closeButton;
    
    // Touch handling variables
    private float initialX;
    private float initialY;
    private float initialTouchX;
    private float initialTouchY;
    private long touchDownTime;
    private static final int CLICK_THRESHOLD = 200; // ms
    private static final int MOVEMENT_THRESHOLD = 10; // pixels

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!canDrawOverlays()) {
            Toast.makeText(this, "Missing SYSTEM_ALERT_WINDOW permission", Toast.LENGTH_SHORT).show();
            stopSelf();
            return START_NOT_STICKY;
        }

        if (floatingView == null) {
            createFloatingView();
            fetchGitHubConfiguration();
        }

        return START_STICKY;
    }

    /**
     * Creates the floating view and initializes WindowManager parameters.
     */
    private void createFloatingView() {
        try {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            
            // Inflate the floating panel layout
            LayoutInflater inflater = LayoutInflater.from(this);
            floatingView = inflater.inflate(R.layout.floating_panel_layout, null);
            
            // Initialize UI components
            statusTextView = floatingView.findViewById(R.id.tv_status);
            versionTextView = floatingView.findViewById(R.id.tv_version);
            maintenanceTextView = floatingView.findViewById(R.id.tv_maintenance);
            closeButton = floatingView.findViewById(R.id.btn_close);
            
            // Set initial status
            statusTextView.setText("Loading...");
            versionTextView.setText("Version: --");
            maintenanceTextView.setText("Maintenance: --");
            
            // Setup close button listener
            closeButton.setOnClickListener(v -> closeFloatingPanel());
            
            // Setup touch listener for drag functionality
            floatingView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return handleTouchEvent(event);
                }
            });
            
            // Configure WindowManager layout parameters
            layoutParams = new WindowManager.LayoutParams();
            
            // Set window type based on Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
            }
            
            // Set layout flags
            layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            
            // Set initial position and size
            layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
            layoutParams.x = 0;
            layoutParams.y = 100;
            layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            
            // Set window format
            layoutParams.format = android.graphics.PixelFormat.TRANSLUCENT;
            
            // Add view to window manager
            windowManager.addView(floatingView, layoutParams);
            
            // Re-enable touch after initial setup
            updateTouchability(true);
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error creating floating view", e);
            stopSelf();
        }
    }

    /**
     * Handles touch events for dragging the floating panel.
     * Distinguishes between drag and click interactions.
     */
    private boolean handleTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialX = layoutParams.x;
                initialY = layoutParams.y;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                touchDownTime = System.currentTimeMillis();
                return true;
                
            case MotionEvent.ACTION_MOVE:
                float deltaX = event.getRawX() - initialTouchX;
                float deltaY = event.getRawY() - initialTouchY;
                
                // Only update position if movement exceeds threshold
                if (Math.abs(deltaX) > MOVEMENT_THRESHOLD || Math.abs(deltaY) > MOVEMENT_THRESHOLD) {
                    layoutParams.x = (int) (initialX + deltaX);
                    layoutParams.y = (int) (initialY + deltaY);
                    
                    // Update view position
                    if (windowManager != null) {
                        windowManager.updateViewLayout(floatingView, layoutParams);
                    }
                }
                return true;
                
            case MotionEvent.ACTION_UP:
                long touchDuration = System.currentTimeMillis() - touchDownTime;
                
                // Check if this was a click (short duration and minimal movement)
                float finalDeltaX = event.getRawX() - initialTouchX;
                float finalDeltaY = event.getRawY() - initialTouchY;
                
                boolean isClick = touchDuration < CLICK_THRESHOLD &&
                                Math.abs(finalDeltaX) < MOVEMENT_THRESHOLD &&
                                Math.abs(finalDeltaY) < MOVEMENT_THRESHOLD;
                
                if (isClick) {
                    floatingView.performClick();
                }
                return true;
        }
        return false;
    }

    /**
     * Updates touchability of the floating view.
     */
    private void updateTouchability(boolean touchable) {
        if (layoutParams != null && windowManager != null) {
            if (touchable) {
                layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            } else {
                layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                                   WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            }
            windowManager.updateViewLayout(floatingView, layoutParams);
        }
    }

    /**
     * Fetches configuration from GitHub in a background thread.
     * Parses JSON and updates UI with fetched values.
     */
    private void fetchGitHubConfiguration() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String jsonResponse = downloadJsonFromGitHub(GITHUB_RAW_URL);
                    
                    if (jsonResponse != null && !jsonResponse.isEmpty()) {
                        parseAndUpdateUI(jsonResponse);
                    } else {
                        updateUIOnError("Failed to fetch configuration");
                    }
                    
                } catch (Exception e) {
                    android.util.Log.e(TAG, "Error fetching GitHub configuration", e);
                    updateUIOnError("Network error: " + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * Downloads JSON content from GitHub raw URL using HttpURLConnection.
     * Includes timeout and error handling.
     */
    private String downloadJsonFromGitHub(String urlString) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        
        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setConnectTimeout(NETWORK_TIMEOUT);
            urlConnection.setReadTimeout(NETWORK_TIMEOUT);
            urlConnection.setRequestProperty("Accept", "application/json");
            
            // Check response code
            if (urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                android.util.Log.w(TAG, "GitHub API returned status code: " + 
                                  urlConnection.getResponseCode());
                return null;
            }
            
            // Read response
            reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            
            return sb.toString();
            
        } catch (IOException e) {
            android.util.Log.e(TAG, "IOException while downloading from GitHub", e);
            return null;
            
        } finally {
            // Ensure proper resource cleanup
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    android.util.Log.e(TAG, "Error closing reader", e);
                }
            }
            
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    /**
     * Parses JSON response and extracts configuration values.
     * Updates UI components on the main thread.
     */
    private void parseAndUpdateUI(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            
            // Extract configuration values
            String latestVersion = jsonObject.optString("latest_version", "Unknown");
            String panelStatus = jsonObject.optString("panel_status", "Offline");
            boolean maintenanceMode = jsonObject.optBoolean("maintenance_mode", false);
            
            // Update UI on main thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (statusTextView != null) {
                        statusTextView.setText("Status: " + panelStatus);
                    }
                    
                    if (versionTextView != null) {
                        versionTextView.setText("Version: " + latestVersion);
                    }
                    
                    if (maintenanceTextView != null) {
                        maintenanceTextView.setText(
                            "Maintenance: " + (maintenanceMode ? "ON" : "OFF")
                        );
                    }
                    
                    android.util.Log.d(TAG, "UI updated with GitHub configuration");
                }
            });
            
        } catch (JSONException e) {
            android.util.Log.e(TAG, "Error parsing JSON response", e);
            updateUIOnError("Invalid configuration format");
        }
    }

    /**
     * Updates UI to show error state on the main thread.
     */
    private void updateUIOnError(String errorMessage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (statusTextView != null) {
                    statusTextView.setText("Status: Error");
                }
                android.util.Log.w(TAG, errorMessage);
            }
        });
    }

    /**
     * Closes the floating panel safely.
     * Removes view from WindowManager and stops the service.
     */
    private void closeFloatingPanel() {
        try {
            if (floatingView != null && windowManager != null) {
                windowManager.removeView(floatingView);
                floatingView = null;
                android.util.Log.d(TAG, "Floating view removed from WindowManager");
            }
            
            stopSelf();
            Toast.makeText(this, "Panel closed", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error closing floating panel", e);
        }
    }

    /**
     * Checks if the app has permission to draw over other apps.
     * Required for TYPE_APPLICATION_OVERLAY on Android 6.0+
     */
    private boolean canDrawOverlays() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        
        return Settings.canDrawOverlays(this);
    }

    /**
     * Helper method to run code on the main thread.
     */
    private void runOnUiThread(Runnable runnable) {
        if (floatingView != null) {
            floatingView.post(runnable);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        try {
            if (floatingView != null && windowManager != null) {
                windowManager.removeView(floatingView);
                floatingView = null;
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error in onDestroy", e);
        }
        
        android.util.Log.d(TAG, "FloatingPanelService destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
