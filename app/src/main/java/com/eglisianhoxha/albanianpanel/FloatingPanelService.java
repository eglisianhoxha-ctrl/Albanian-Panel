package com.eglisianhoxha.albanianpanel;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class FloatingPanelService extends Service {

    private static final String TAG = "FloatingPanelService";
    private static final String GITHUB_CONFIG_URL = 
        "https://raw.githubusercontent.com/eglisianhoxha-ctrl/Albanian-Panel/main/panel_config.json";

    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    private TextView statusTextView;
    private TextView versionTextView;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "FloatingPanelService started");

        if (floatingView == null) {
            initializeFloatingView();
            fetchConfigFromGitHub();
        }

        return START_STICKY;
    }

    private void initializeFloatingView() {
        // Initialize WindowManager
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // Inflate the layout for floating panel
        LayoutInflater inflater = LayoutInflater.from(this);
        floatingView = inflater.inflate(R.layout.layout_floating_panel, null);

        // Get UI components
        statusTextView = floatingView.findViewById(R.id.tv_status);
        versionTextView = floatingView.findViewById(R.id.tv_version);
        Button closeButton = floatingView.findViewById(R.id.btn_close);

        // Set close button listener
        closeButton.setOnClickListener(v -> closeFloatingPanel());

        // Configure WindowManager.LayoutParams
        params = new WindowManager.LayoutParams();

        // Set type based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        // Set layout flags
        params.format = android.graphics.PixelFormat.TRANSLUCENT;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE 
            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE 
            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

        // Set initial position and size
        params.width = 300;
        params.height = 200;
        params.x = 0;
        params.y = 0;

        // Add view to WindowManager
        try {
            windowManager.addView(floatingView, params);
            Log.d(TAG, "Floating view added to WindowManager");
        } catch (Exception e) {
            Log.e(TAG, "Error adding floating view: " + e.getMessage(), e);
        }
    }

    private void fetchConfigFromGitHub() {
        new Thread(() -> {
            try {
                String jsonResponse = downloadJSON(GITHUB_CONFIG_URL);
                if (jsonResponse != null) {
                    parseAndUpdateUI(jsonResponse);
                } else {
                    Log.e(TAG, "Failed to download configuration from GitHub");
                    updateStatusUI("Error: No response", "N/A");
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception during GitHub fetch: " + e.getMessage(), e);
                updateStatusUI("Error: " + e.getMessage(), "N/A");
            }
        }).start();
    }

    private String downloadJSON(String urlString) {
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);
            urlConnection.setRequestMethod("GET");

            int responseCode = urlConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = urlConnection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();
                inputStream.close();

                Log.d(TAG, "Successfully downloaded JSON: " + response.toString());
                return response.toString();
            } else {
                Log.e(TAG, "HTTP Error: " + responseCode);
                return null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Network error: " + e.getMessage(), e);
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private void parseAndUpdateUI(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);

            String latestVersion = jsonObject.optString("latest_version", "Unknown");
            String panelStatus = jsonObject.optString("panel_status", "Unknown");
            String maintenanceMode = jsonObject.optString("maintenance_mode", "false");

            String statusMessage = "Status: " + panelStatus;
            if ("true".equalsIgnoreCase(maintenanceMode)) {
                statusMessage += " (Maintenance Mode)";
            }

            Log.d(TAG, "Parsed config - Version: " + latestVersion + 
                  ", Status: " + panelStatus + ", Maintenance: " + maintenanceMode);

            updateStatusUI(statusMessage, "v" + latestVersion);

        } catch (JSONException e) {
            Log.e(TAG, "JSON parsing error: " + e.getMessage(), e);
            updateStatusUI("Error parsing config", "N/A");
        }
    }

    private void updateStatusUI(String status, String version) {
        runOnUiThread(() -> {
            try {
                if (statusTextView != null) {
                    statusTextView.setText(status);
                }
                if (versionTextView != null) {
                    versionTextView.setText(version);
                }
                Log.d(TAG, "UI updated - Status: " + status + ", Version: " + version);
            } catch (Exception e) {
                Log.e(TAG, "Error updating UI: " + e.getMessage(), e);
            }
        });
    }

    private void runOnUiThread(Runnable runnable) {
        // Post to main thread if we're not already on it
        if (android.os.Looper.getMainLooper() == android.os.Looper.myLooper()) {
            runnable.run();
        } else {
            android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
            handler.post(runnable);
        }
    }

    private void closeFloatingPanel() {
        Log.d(TAG, "Closing floating panel");
        try {
            if (floatingView != null && windowManager != null) {
                windowManager.removeView(floatingView);
                floatingView = null;
                Log.d(TAG, "Floating view removed from WindowManager");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error removing floating view: " + e.getMessage(), e);
        }

        // Stop the service
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "FloatingPanelService destroyed");

        try {
            if (floatingView != null && windowManager != null) {
                windowManager.removeView(floatingView);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup: " + e.getMessage(), e);
        }
    }
}
