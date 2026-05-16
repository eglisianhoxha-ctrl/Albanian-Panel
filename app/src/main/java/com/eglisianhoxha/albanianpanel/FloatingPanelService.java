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
import android.widget.Toast;
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
    private View miniBoxView;
    private WindowManager.LayoutParams params;
    private WindowManager.LayoutParams miniBoxParams;
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
        floatingView = inflater.inflate(R.layout.floating_layout, null);

        // Get UI components
        statusTextView = floatingView.findViewById(R.id.tv_status);
        versionTextView = floatingView.findViewById(R.id.tv_version);

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
            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

        // Set initial position and size to match parent
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        params.x = 0;
        params.y = 0;

        // Add view to WindowManager
        try {
            windowManager.addView(floatingView, params);
            Log.d(TAG, "Floating view added to WindowManager");
        } catch (Exception e) {
            Log.e(TAG, "Error adding floating view: " + e.getMessage(), e);
        }

        // Bind all buttons with click listeners
        bindAllButtons();
    }

    private void bindAllButtons() {
        // MOVEMENT & ACTIONS BUTTONS - Row 1
        bindButton(R.id.btn_unlimited_jump, "Unlimited Jump");
        bindButton(R.id.btn_fly, "Fly");
        bindButton(R.id.btn_fly_parachute, "Fly Parachute");
        bindButton(R.id.btn_fast_jump, "Fast Jump");
        bindButton(R.id.btn_high_jump, "High Jump");

        // Row 2
        bindButton(R.id.btn_long_jump, "Long Jump");
        bindButton(R.id.btn_speed, "Speed");
        bindButton(R.id.btn_blink, "Blink");
        bindButton(R.id.btn_teleport_click, "Teleport Click");
        bindButton(R.id.btn_teleport_killer, "Teleport Killer");

        // Row 3
        bindButton(R.id.btn_respawn_same_place, "Respawn Same Place");
        bindButton(R.id.btn_auto_respawn, "Auto Respawn");
        bindButton(R.id.btn_respawn, "Respawn");
        bindButton(R.id.btn_free_camera, "Free Camera");
        bindButton(R.id.btn_aim_bot, "Aim Bot");

        // Row 4
        bindButton(R.id.btn_aim_bow, "Aim Bow");
        bindButton(R.id.btn_kill_aura, "Kill Aura");
        bindButton(R.id.btn_infinity_kill_aura, "Infinity Kill Aura");
        bindButton(R.id.btn_anti_kill_aura, "Anti Kill Aura");
        bindButton(R.id.btn_auto_click, "Auto Click");

        // Row 5
        bindButton(R.id.btn_attack_button, "Attack Button");
        bindButton(R.id.btn_hit_box, "Hit Box");
        bindButton(R.id.btn_hit_box_v2, "Hit Box v2");
        bindButton(R.id.btn_infinity_hit_box, "Infinity Hit Box");
        bindButton(R.id.btn_reach, "Reach");

        // Row 6
        bindButton(R.id.btn_infinity_reach_players, "Infinity Reach Players");
        bindButton(R.id.btn_auto_knock_back, "Auto Knockback");
        bindButton(R.id.btn_raket_button, "Raket Button");
        bindButton(R.id.btn_cannon_button, "Cannon Button");
        bindButton(R.id.btn_break_block, "Break Block");

        // Row 7
        bindButton(R.id.btn_fast_break, "Fast Break");
        bindButton(R.id.btn_drop_speed, "Drop Speed");
        bindButton(R.id.btn_anti_void, "Anti Void");
        bindButton(R.id.btn_no_fall_damage, "No Fall Damage");
        bindButton(R.id.btn_ban_click_cd, "Ban Click CD");

        // Row 8
        bindButton(R.id.btn_no_clip, "No Clip");
        bindButton(R.id.btn_no_fall, "No Fall");

        // COSMETIC & NAME CUSTOMIZATION BUTTONS - Row 1
        bindButton(R.id.btn_change_name, "Change Name");
        bindButton(R.id.btn_hide_name, "Hide Name");

        // Row 2
        bindButton(R.id.btn_rainbow_name, "Rainbow Name");
        bindButton(R.id.btn_red_name, "Red Name");
        bindButton(R.id.btn_blue_name, "Blue Name");
        bindButton(R.id.btn_black_name, "Black Name");
        bindButton(R.id.btn_white_name, "White Name");

        // Row 3
        bindButton(R.id.btn_green_name, "Green Name");
        bindButton(R.id.btn_yellow_name, "Yellow Name");
        bindButton(R.id.btn_purple_name, "Purple Name");
        bindButton(R.id.btn_gold_name, "Gold Name");
        bindButton(R.id.btn_cyan_name, "Cyan Name");

        // CLOSE BUTTONS
        Button closeButton = floatingView.findViewById(R.id.btn_close_panel);
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> closeFloatingPanel());
        }

        Button closeButtonBottom = floatingView.findViewById(R.id.btn_close_panel_bottom);
        if (closeButtonBottom != null) {
            closeButtonBottom.setOnClickListener(v -> closeFloatingPanel());
        }

        Log.d(TAG, "All buttons bound successfully");
    }

    private void bindButton(int buttonId, String buttonLabel) {
        try {
            Button button = floatingView.findViewById(buttonId);
            if (button != null) {
                button.setOnClickListener(v -> onButtonClicked(buttonLabel));
            } else {
                Log.w(TAG, "Button with ID " + buttonId + " not found in layout");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error binding button " + buttonLabel + ": " + e.getMessage(), e);
        }
    }

    private void onButtonClicked(String buttonLabel) {
        Log.d(TAG, "Button clicked: " + buttonLabel);
        
        // Show Toast notification
        Toast.makeText(
            this,
            buttonLabel + " Toggled",
            Toast.LENGTH_SHORT
        ).show();

        // Log the event
        Log.i(TAG, "Event: " + buttonLabel + " action triggered by user");

        // Handle button action
        handleButtonAction(buttonLabel);
    }

    private void handleButtonAction(String buttonLabel) {
        // This is where you would implement actual logic for each button
        // For now, it's just logging the action
        switch (buttonLabel) {
            case "Unlimited Jump":
                Log.d(TAG, "Executing: Unlimited Jump feature");
                break;
            case "Fly":
                Log.d(TAG, "Executing: Fly feature");
                break;
            case "Fly Parachute":
                Log.d(TAG, "Executing: Fly Parachute feature");
                break;
            case "Fast Jump":
                Log.d(TAG, "Executing: Fast Jump feature");
                break;
            case "High Jump":
                Log.d(TAG, "Executing: High Jump feature");
                break;
            case "Long Jump":
                Log.d(TAG, "Executing: Long Jump feature");
                break;
            case "Speed":
                Log.d(TAG, "Executing: Speed feature");
                break;
            case "Blink":
                Log.d(TAG, "Executing: Blink feature");
                break;
            case "Teleport Click":
                Log.d(TAG, "Executing: Teleport Click feature");
                break;
            case "Teleport Killer":
                Log.d(TAG, "Executing: Teleport Killer feature");
                break;
            case "Respawn Same Place":
                Log.d(TAG, "Executing: Respawn Same Place feature");
                break;
            case "Auto Respawn":
                Log.d(TAG, "Executing: Auto Respawn feature");
                break;
            case "Respawn":
                Log.d(TAG, "Executing: Respawn feature");
                break;
            case "Free Camera":
                Log.d(TAG, "Executing: Free Camera feature");
                break;
            case "Aim Bot":
                Log.d(TAG, "Executing: Aim Bot feature");
                break;
            case "Kill Aura":
                Log.d(TAG, "Executing: Kill Aura feature");
                break;
            case "Auto Click":
                Log.d(TAG, "Executing: Auto Click feature");
                break;
            case "Change Name":
                Log.d(TAG, "Executing: Change Name feature");
                break;
            case "Rainbow Name":
                Log.d(TAG, "Executing: Rainbow Name feature");
                break;
            default:
                Log.d(TAG, "Executing: " + buttonLabel + " feature");
                break;
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

        // Show mini box in middle top with black color
        showMiniStatusBox();

        // Stop the service after a delay to show the mini box
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
            this::stopSelf,
            2000 // Show mini box for 2 seconds before stopping service
        );
    }

    private void showMiniStatusBox() {
        try {
            // Create a new LinearLayout for the mini box
            android.widget.LinearLayout miniBox = new android.widget.LinearLayout(this);
            miniBox.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            miniBox.setBackgroundColor(android.graphics.Color.BLACK);
            miniBox.setPadding(24, 16, 24, 16);
            miniBox.setOrientation(android.widget.LinearLayout.HORIZONTAL);

            // Create TextViews for the mini box
            android.widget.TextView textView = new android.widget.TextView(this);
            textView.setText("PANEL CLOSED");
            textView.setTextColor(android.graphics.Color.WHITE);
            textView.setTextSize(14);
            textView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            miniBox.addView(textView);

            // Configure mini box layout params
            miniBoxParams = new WindowManager.LayoutParams();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                miniBoxParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                miniBoxParams.type = WindowManager.LayoutParams.TYPE_PHONE;
            }

            miniBoxParams.format = android.graphics.PixelFormat.TRANSLUCENT;
            miniBoxParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;

            // Get screen dimensions to center horizontally and position at top
            android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
            int screenWidth = displayMetrics.widthPixels;

            miniBoxParams.width = android.widget.LinearLayout.LayoutParams.WRAP_CONTENT;
            miniBoxParams.height = android.widget.LinearLayout.LayoutParams.WRAP_CONTENT;
            miniBoxParams.x = screenWidth / 2; // Center horizontally
            miniBoxParams.y = 100; // Position near top
            miniBoxParams.gravity = android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.TOP;

            // Add mini box to WindowManager
            windowManager.addView(miniBox, miniBoxParams);
            miniBoxView = miniBox;
            Log.d(TAG, "Mini status box displayed");

            // Auto-remove mini box after 2 seconds
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                try {
                    if (miniBoxView != null && windowManager != null) {
                        windowManager.removeView(miniBoxView);
                        miniBoxView = null;
                        Log.d(TAG, "Mini status box removed");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error removing mini box: " + e.getMessage(), e);
                }
            }, 2000);

        } catch (Exception e) {
            Log.e(TAG, "Error showing mini status box: " + e.getMessage(), e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "FloatingPanelService destroyed");

        try {
            if (floatingView != null && windowManager != null) {
                windowManager.removeView(floatingView);
            }
            if (miniBoxView != null && windowManager != null) {
                windowManager.removeView(miniBoxView);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup: " + e.getMessage(), e);
        }
    }
}
