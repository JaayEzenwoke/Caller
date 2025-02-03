package com.aid.caller;

import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.aid.caller.service.Servicer;

// MainActivity.java
public class MainActivity extends AppCompatActivity {
    private static final String[] PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.FOREGROUND_SERVICE
    };
    private static final int PERMISSION_REQUEST_CODE = 123;
    private Button listener;
    private boolean granted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listener = findViewById(R.id.listener);
        listener.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (!hasPermissions()) {
                    ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, PERMISSION_REQUEST_CODE);
                } else {
                    startSpeechService();
                }
            }
        });


    }

    private boolean hasPermissions() {
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }

            if (granted) {
                startSpeechService();
            } else {
                Toast.makeText(MainActivity.this, "Permissions required for this feature",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startSpeechService() {
        Intent intent = new Intent(this, Servicer.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }
}