package com.aid.caller.service;

// SpeechRecognitionService.java
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import androidx.core.app.NotificationCompat;

import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.widget.Toast;

import com.aid.caller.MainActivity;
import com.aid.caller.R;

import java.util.ArrayList;
import java.util.Arrays;

public class Servicer extends Service {

    private static final String CHANNEL_ID = "SpeechRecognitionChannel";
    private static final int NOTIFICATION_ID = 1;
    private SpeechRecognizer recognizer;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        initializeSpeechRecognizer();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Speech Recognition Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Voice Recognition Active")
                .setContentText("Listening for voice commands")
                .setSmallIcon(R.drawable.mic) // Make sure to add this icon to your drawable resources
                .setContentIntent(pendingIntent)
                .build();
    }

    private void initializeSpeechRecognizer() {
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        System.out.println("initializeSpeechRecognizer()");
        recognizer.setRecognitionListener(new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle params) {
                System.out.println("onReadyForSpeech()");
            }

            @Override
            public void onBeginningOfSpeech() {
                System.out.println("onBeginningOfSpeech()");
            }

            @Override
            public void onRmsChanged(float rmsdB) {
//                System.out.println("onRmsChanged()");
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                System.out.println("onBufferReceived()");
            }

            @Override
            public void onEndOfSpeech() {
                System.out.println("onEndOfSpeech()");
                // Restart listening when speech ends
//                recognizer.startListening(speechRecognizerIntent);
            }

            @Override
            public void onError(int error) {
                System.out.println("onError(): " + "error-code: " + error);
                // Restart listening on error after a short delay
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        recognizer.startListening(speechRecognizerIntent);
                    }
                }, 1000);
            }

            @Override
            public void onResults(Bundle results) {
                System.out.println("onResults()");
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                System.out.println("results of speech: " + Arrays.toString(matches.toArray()));
                if (matches != null && !matches.isEmpty()) {
                    String spokenText = matches.get(0);
                    searchContactAndCall(spokenText);
                }
                // Continue listening
                recognizer.startListening(speechRecognizerIntent);
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                System.out.println("onPartialResults()");
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                System.out.println("onEvent()");
            }
        });

        // Start listening
        recognizer.startListening(speechRecognizerIntent);
    }

    private void searchContactAndCall(String contactName) {
        Cursor cursor = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?",
                new String[]{"%" + contactName + "%"},
                null
        );

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    String contact = cursor.getString(0);
                    Toast.makeText(this, "Contact found calling: " + cursor.getString(0), Toast.LENGTH_SHORT).show();
                    makePhoneCall(contact);
                } else {
                    Toast.makeText(this, "Contact not found", Toast.LENGTH_SHORT).show();
                }
            } finally {
                cursor.close();
            }
        }
    }

    private void makePhoneCall(String contact) {
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + contact));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onRebind(Intent intent) {
        System.out.println(intent + "");
        super.onRebind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (recognizer != null) {
            recognizer.destroy();
        }
    }
}
