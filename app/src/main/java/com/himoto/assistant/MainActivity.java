package com.himoto.assistant;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private TextView statusText;
    private Button startBtn;
    private Button stopBtn;

    private static final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_PHONE_STATE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        startBtn = findViewById(R.id.startBtn);
        stopBtn = findViewById(R.id.stopBtn);

        startBtn.setOnClickListener(v -> {
            if (checkPermissions()) {
                startVoiceService();
            } else {
                requestPermissions();
            }
        });

        stopBtn.setOnClickListener(v -> stopVoiceService());

        // Battery optimization ignore (for background running)
        askBatteryOptimization();

        // Auto-check permissions on launch
        if (checkPermissions()) {
            statusText.setText("✅ Sab permissions mil gayi!\nService start karo.");
        } else {
            statusText.setText("⚠️ Pehle permissions do\nfir Start dabao");
            requestPermissions();
        }
    }

    private boolean checkPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestPermissions() {
        List<String> needed = new ArrayList<>();
        for (String p : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                needed.add(p);
            }
        }
        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                needed.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                statusText.setText("✅ Sab permissions mil gayi!\nService start karo.");
                Toast.makeText(this, "Permissions mil gayi! Ab Start karo", Toast.LENGTH_SHORT).show();
            } else {
                statusText.setText("❌ Kuch permissions nahi mili.\nSettings mein jaake manually do.");
                // Open settings
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
    }

    private void startVoiceService() {
        Intent serviceIntent = new Intent(this, VoiceAssistantService.class);
        serviceIntent.setAction("START");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        statusText.setText("🟢 Hi Moto Active Hai!\n\nBoliye:\n• 'Hi Moto' - Wake karo\n• 'Open WhatsApp'\n• 'Call [naam]'\n• 'Call band karo'\n• Koi bhi sawaal pucho");
        startBtn.setEnabled(false);
        stopBtn.setEnabled(true);
    }

    private void stopVoiceService() {
        Intent serviceIntent = new Intent(this, VoiceAssistantService.class);
        stopService(serviceIntent);
        statusText.setText("🔴 Service Band\n\nStart karo dobara sunne ke liye.");
        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
    }

    private void askBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                android.os.PowerManager pm = (android.os.PowerManager) getSystemService(POWER_SERVICE);
                if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (VoiceAssistantService.isRunning) {
            statusText.setText("🟢 Hi Moto Active Hai!\n\nBoliye:\n• 'Hi Moto' - Wake karo\n• 'Open WhatsApp'\n• 'Call [naam]'\n• 'Call band karo'\n• Koi bhi sawaal pucho");
            startBtn.setEnabled(false);
            stopBtn.setEnabled(true);
        }
    }
}
