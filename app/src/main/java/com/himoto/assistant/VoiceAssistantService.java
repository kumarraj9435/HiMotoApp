package com.himoto.assistant;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.ContactsContract;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.telephony.TelephonyManager;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class VoiceAssistantService extends Service implements TextToSpeech.OnInitListener {

    private static final String TAG = "HiMoto";
    private static final String CHANNEL_ID = "HiMotoChannel";
    private static final String WAKE_WORD_1 = "hi moto";
    private static final String WAKE_WORD_2 = "he moto";
    private static final String WAKE_WORD_3 = "himoto";
    private static final String GEMINI_API_KEY = "YOUR_GEMINI_API_KEY_HERE"; // Yahan apni Gemini key dalo

    public static boolean isRunning = false;
    private boolean isWakeWordDetected = false;
    private boolean isListeningForCommand = false;

    private SpeechRecognizer speechRecognizer;
    private TextToSpeech tts;
    private Handler mainHandler;
    private boolean ttsReady = false;

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
        tts = new TextToSpeech(this, this);
        isRunning = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, buildNotification("🎤 Hi Moto sun raha hai..."));
        mainHandler.postDelayed(this::startListening, 1500);
        return START_STICKY;
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(new Locale("hi", "IN"));
            ttsReady = true;
            speak("Hi Moto ready hai. Hi Moto boliye activate karne ke liye.");
            startListening();
        }
    }

    private void startListening() {
        mainHandler.post(() -> {
            if (speechRecognizer != null) {
                speechRecognizer.destroy();
            }
            if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                Log.e(TAG, "Speech recognition not available");
                return;
            }
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) {
                    Log.d(TAG, isWakeWordDetected ? "Listening for command..." : "Listening for wake word...");
                }
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() {}

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String spokenText = matches.get(0).toLowerCase().trim();
                        Log.d(TAG, "Heard: " + spokenText);
                        handleSpeech(spokenText);
                    }
                    // Restart listening after processing
                    mainHandler.postDelayed(VoiceAssistantService.this::startListening, 500);
                }

                @Override
                public void onError(int error) {
                    Log.d(TAG, "Recognition error: " + error);
                    // Restart listening on any error
                    mainHandler.postDelayed(VoiceAssistantService.this::startListening, 1000);
                }

                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });

            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN");
            intent.putExtra("android.speech.extra.PREFER_OFFLINE", false);
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500);
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
            speechRecognizer.startListening(intent);
        });
    }

    private void handleSpeech(String text) {
        Log.d(TAG, "Processing: " + text);

        // CHECK WAKE WORD
        if (!isWakeWordDetected) {
            if (containsWakeWord(text)) {
                isWakeWordDetected = true;
                updateNotification("✅ Activated! Command boliye...");
                speak("Haan, boliye!");
                // Reset wake word after 10 seconds of inactivity
                mainHandler.postDelayed(() -> {
                    if (isWakeWordDetected) {
                        isWakeWordDetected = false;
                        updateNotification("🎤 Hi Moto sun raha hai...");
                    }
                }, 10000);
            }
            return;
        }

        // PROCESS COMMAND after wake word
        isWakeWordDetected = false; // Reset for next time
        updateNotification("🎤 Hi Moto sun raha hai...");
        processCommand(text);
    }

    private boolean containsWakeWord(String text) {
        return text.contains(WAKE_WORD_1) ||
               text.contains(WAKE_WORD_2) ||
               text.contains(WAKE_WORD_3) ||
               text.contains("hi motto") ||
               text.contains("he motto") ||
               text.contains("high moto");
    }

    private void processCommand(String command) {
        Log.d(TAG, "Command: " + command);

        // CALL COMMANDS
        if (command.startsWith("call ") || command.contains(" ko call") || command.contains("call karo")) {
            String contactName = extractContactName(command);
            if (contactName != null && !contactName.isEmpty()) {
                makeCall(contactName);
                return;
            }
        }

        // HANG UP / CALL END
        if (command.contains("call band") || command.contains("hang up") ||
            command.contains("call kaat") || command.contains("phone rakh") ||
            command.contains("call cut") || command.contains("band kar")) {
            endCall();
            return;
        }

        // OPEN APP COMMANDS
        if (command.startsWith("open ") || command.contains("kholo") || command.contains("chalao")) {
            String appName = extractAppName(command);
            if (appName != null) {
                openApp(appName);
                return;
            }
        }

        // WHATSAPP SPECIFIC
        if (command.contains("whatsapp")) {
            openSpecificApp("com.whatsapp");
            return;
        }

        // SETTINGS
        if (command.contains("settings") || command.contains("setting")) {
            Intent i = new Intent(android.provider.Settings.ACTION_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            speak("Settings khul gayi");
            return;
        }

        // CAMERA
        if (command.contains("camera")) {
            Intent cam = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            cam.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(cam);
            speak("Camera khul gaya");
            return;
        }

        // YOUTUBE
        if (command.contains("youtube")) {
            openSpecificApp("com.google.android.youtube");
            return;
        }

        // MAPS
        if (command.contains("maps") || command.contains("google map")) {
            openSpecificApp("com.google.android.apps.maps");
            return;
        }

        // CHROME / BROWSER
        if (command.contains("chrome") || command.contains("browser")) {
            openSpecificApp("com.android.chrome");
            return;
        }

        // GMAIL
        if (command.contains("gmail") || command.contains("mail")) {
            openSpecificApp("com.google.android.gm");
            return;
        }

        // PLAY STORE
        if (command.contains("play store") || command.contains("playstore")) {
            openSpecificApp("com.android.vending");
            return;
        }

        // INSTAGRAM
        if (command.contains("instagram")) {
            openSpecificApp("com.instagram.android");
            return;
        }

        // FACEBOOK
        if (command.contains("facebook")) {
            openSpecificApp("com.facebook.katana");
            return;
        }

        // TELEGRAM
        if (command.contains("telegram")) {
            openSpecificApp("org.telegram.messenger");
            return;
        }

        // SPOTIFY
        if (command.contains("spotify") || command.contains("music")) {
            openSpecificApp("com.spotify.music");
            return;
        }

        // TIME / DATE
        if (command.contains("time") || command.contains("samay") || command.contains("baje")) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("hh:mm a", Locale.getDefault());
            String time = sdf.format(new Date());
            speak("Abhi " + time + " baje hain");
            return;
        }

        if (command.contains("date") || command.contains("tarikh") || command.contains("aaj")) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMMM yyyy", new Locale("hi", "IN"));
            String date = sdf.format(new Date());
            speak("Aaj ki tarikh hai " + date);
            return;
        }

        // FLASHLIGHT
        if (command.contains("torch") || command.contains("flashlight") || command.contains("light")) {
            speak("Torch ke liye please Settings mein jaayein");
            return;
        }

        // DEFAULT - Ask Claude AI
        askGeminiAI(command);
    }

    private String extractContactName(String command) {
        // "call sourav" -> "sourav"
        // "sourav ko call karo" -> "sourav"
        String name = command
            .replace("call karo", "")
            .replace("ko call", "")
            .replace("call kar", "")
            .replace("call", "")
            .trim();
        return name;
    }

    private String extractAppName(String command) {
        return command
            .replace("open", "")
            .replace("kholo", "")
            .replace("chalao", "")
            .replace("start", "")
            .trim();
    }

    private void makeCall(String contactName) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            speak("Contacts permission nahi hai");
            return;
        }

        String phoneNumber = findContactNumber(contactName);
        if (phoneNumber != null) {
            speak(contactName + " ko call kar raha hoon");
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + phoneNumber));
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                    == PackageManager.PERMISSION_GRANTED) {
                startActivity(callIntent);
            } else {
                speak("Call karne ki permission nahi hai");
            }
        } else {
            speak(contactName + " contacts mein nahi mila. Dobara boliye.");
        }
    }

    private String findContactNumber(String name) {
        String number = null;
        try {
            Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
            String[] projection = {
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            };
            String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?";
            String[] selectionArgs = {"%" + name + "%"};

            Cursor cursor = getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                number = cursor.getString(cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Phone.NUMBER));
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Contact search error: " + e.getMessage());
        }
        return number;
    }

    private void endCall() {
        try {
            // Android 9+ method
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    android.telecom.TelecomManager tm = (android.telecom.TelecomManager) getSystemService(TELECOM_SERVICE);
                    if (tm != null) {
                        tm.endCall();
                        speak("Call kaat diya");
                        return;
                    }
                } catch (Exception ex) {
                    // fallback below
                }
            }
            // Fallback - KeyEvent
            Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
            i.putExtra(Intent.EXTRA_KEY_EVENT,
                new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP,
                    android.view.KeyEvent.KEYCODE_HEADSETHOOK));
            sendBroadcast(i);
            speak("Call kaat diya");
        } catch (Exception e) {
            speak("Call nahi kaat paya, please manually kaatein");
        }
    }

    private void openApp(String appName) {
        appName = appName.toLowerCase().trim();

        // Map common names to package names
        Map<String, String> appMap = new HashMap<>();
        appMap.put("whatsapp", "com.whatsapp");
        appMap.put("instagram", "com.instagram.android");
        appMap.put("youtube", "com.google.android.youtube");
        appMap.put("facebook", "com.facebook.katana");
        appMap.put("telegram", "org.telegram.messenger");
        appMap.put("chrome", "com.android.chrome");
        appMap.put("gmail", "com.google.android.gm");
        appMap.put("maps", "com.google.android.apps.maps");
        appMap.put("camera", "android.media.action.IMAGE_CAPTURE");
        appMap.put("calculator", "com.android.calculator2");
        appMap.put("clock", "com.android.deskclock");
        appMap.put("calendar", "com.google.android.calendar");
        appMap.put("spotify", "com.spotify.music");
        appMap.put("snapchat", "com.snapchat.android");
        appMap.put("twitter", "com.twitter.android");
        appMap.put("tiktok", "com.zhiliaoapp.musically");
        appMap.put("netflix", "com.netflix.mediaclient");
        appMap.put("amazon", "com.amazon.mShop.android.shopping");
        appMap.put("flipkart", "com.flipkart.android");
        appMap.put("paytm", "net.one97.paytm");
        appMap.put("gpay", "com.google.android.apps.nbu.paisa.user");
        appMap.put("phonepe", "com.phonepe.app");
        appMap.put("settings", "android.settings");
        appMap.put("play store", "com.android.vending");

        if (appMap.containsKey(appName)) {
            openSpecificApp(appMap.get(appName));
        } else {
            // Try to find by app name in installed apps
            PackageManager pm = getPackageManager();
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> apps = pm.queryIntentActivities(mainIntent, 0);

            for (ResolveInfo info : apps) {
                String label = info.loadLabel(pm).toString().toLowerCase();
                if (label.contains(appName) || appName.contains(label)) {
                    openSpecificApp(info.activityInfo.packageName);
                    return;
                }
            }
            speak(appName + " app nahi mili. Kya yeh phone mein install hai?");
        }
    }

    private void openSpecificApp(String packageName) {
        try {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchIntent);
                speak("App khul gayi");
            } else {
                speak("Yeh app phone mein nahi hai");
            }
        } catch (Exception e) {
            speak("App nahi khul paya");
        }
    }

    private void askGeminiAI(String question) {
        speak("Sooch raha hoon...");
        updateNotification("🤔 Jawab dhoondh raha hai...");

        new Thread(() -> {
            try {
                // Gemini API endpoint
                String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + GEMINI_API_KEY;

                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);

                // Gemini ka JSON format
                String safeQuestion = question.replace("\\", "\\\\").replace("\"", "\\\"");
                String jsonBody = "{"
                    + "\"contents\":[{"
                    + "\"parts\":[{"
                    + "\"text\":\"Tum ek helpful Hindi voice assistant ho. Sirf Hindi mein jawab do. "
                    + "Jawab bahut chhota rakh - sirf 1-2 sentences. Simple aur clear bolo. "
                    + "Sawaal hai: " + safeQuestion + "\""
                    + "}]"
                    + "}],"
                    + "\"generationConfig\":{"
                    + "\"maxOutputTokens\":150,"
                    + "\"temperature\":0.7"
                    + "}"
                    + "}";

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonBody.getBytes("UTF-8"));
                }

                int responseCode = conn.getResponseCode();
                InputStream is = responseCode == 200
                    ? conn.getInputStream()
                    : conn.getErrorStream();

                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                }

                String answer = extractGeminiText(response.toString());

                mainHandler.post(() -> {
                    speak(answer);
                    updateNotification("🎤 Hi Moto sun raha hai...");
                });

            } catch (Exception e) {
                Log.e(TAG, "Gemini Error: " + e.getMessage());
                mainHandler.post(() -> {
                    speak("Maafi chahta hoon, abhi internet ya AI se connect nahi ho paya.");
                    updateNotification("🎤 Hi Moto sun raha hai...");
                });
            }
        }).start();
    }

    private String extractGeminiText(String json) {
        try {
            // Gemini response: {"candidates":[{"content":{"parts":[{"text":"..."}]}}]}
            int textStart = json.indexOf("\"text\":\"");
            if (textStart != -1) {
                textStart += 8;
                // Find closing quote (handle escaped quotes)
                StringBuilder result = new StringBuilder();
                int i = textStart;
                while (i < json.length()) {
                    char c = json.charAt(i);
                    if (c == '\\' && i + 1 < json.length()) {
                        char next = json.charAt(i + 1);
                        if (next == '"') { result.append('"'); i += 2; continue; }
                        if (next == 'n') { result.append(' '); i += 2; continue; }
                        if (next == '\\') { result.append('\\'); i += 2; continue; }
                    }
                    if (c == '"') break;
                    result.append(c);
                    i++;
                }
                String answer = result.toString().trim();
                if (!answer.isEmpty()) return answer;
            }
            if (json.contains("API_KEY_INVALID") || json.contains("API key not valid")) {
                return "Gemini API key galat hai. Sahi key daalein.";
            }
            if (json.contains("error")) {
                return "Kuch galat ho gaya. API key aur internet check karein.";
            }
        } catch (Exception e) {
            Log.e(TAG, "Gemini JSON parse error: " + e.getMessage());
        }
        return "Mujhe samajh nahi aaya. Dobara poochiye.";
    }

    private void speak(String text) {
        if (ttsReady && tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "HiMoto_" + System.currentTimeMillis());
        }
        Log.d(TAG, "Speaking: " + text);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Hi Moto Assistant", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Voice Assistant Service");
            channel.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Hi Moto")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build();
    }

    private void updateNotification(String text) {
        Notification notification = buildNotification(text);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(1, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
