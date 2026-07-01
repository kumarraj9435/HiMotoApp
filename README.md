# Hi Moto - Voice Assistant App 🎤

## Kya kya karta hai ye app:
- **"Hi Moto"** bolne par activate ho jaata hai
- **Apps kholna**: "Open WhatsApp", "Open YouTube", "Open Settings" etc.
- **Call karna**: "Call Sourav" (contacts se automatic number milega)
- **Call katna**: "Call band karo" / "Hang up"
- **Sawaal poochna**: AI se jawab milega (Claude API)
- **Phone on hote hi**: Automatically background mein start
- **Background mein**: Hamesha sun raha hai

---

## STEP 1: Android Studio Download Karo
1. https://developer.android.com/studio par jaao
2. Download aur install karo
3. Install hone ke baad kholna

---

## STEP 2: Project Import Karo
1. Android Studio mein: **File → Open**
2. Is folder ko select karo: `HiMotoApp/`
3. **OK** dabao
4. Gradle build hone do (kuch minutes lagenge)

---

## STEP 3: Gemini API Key Lagao (Sawaalon ke liye - BILKUL FREE!)

**VoiceAssistantService.java** file kholna (app/src/main/java/com/himoto/assistant/)

Line dhundho:
```java
private static final String GEMINI_API_KEY = "YOUR_GEMINI_API_KEY_HERE";
```

Apni key daalein:
```java
private static final String GEMINI_API_KEY = "AIzaSyXXXXXXXXXXXXXXXXXX";
```

### Gemini API Key Kaise Milegi (FREE):
1. **https://aistudio.google.com** par jaao
2. Google account se login karo
3. **"Get API Key"** button dabao
4. **"Create API Key"** dabao
5. Key copy karo (AIzaSy... se shuru hogi)
6. Paste karo code mein

> ✅ **Gemini bilkul FREE hai!** Google ka hai, bahut fast hai.

---

## STEP 4: APK Build Karo
1. Android Studio mein: **Build → Build Bundle(s)/APK(s) → Build APK(s)**
2. Wait karo build ke liye
3. "APK(s) generated" notification aayegi
4. **"locate"** dabao APK file dhundne ke liye
5. APK typically yahan milega:
   ```
   HiMotoApp/app/build/outputs/apk/debug/app-debug.apk
   ```

---

## STEP 5: Phone Mein Install Karo

### Method 1: USB se
1. Phone mein **Developer Options** on karo:
   - Settings → About Phone → Build Number par 7 baar tap karo
2. **USB Debugging** on karo:
   - Settings → Developer Options → USB Debugging
3. Phone computer se connect karo
4. Android Studio mein Run button dabao ▶

### Method 2: APK file se
1. APK file WhatsApp ya email se phone par bhejo
2. Phone mein file kholne ka try karo
3. "Unknown sources" allow karne ka option aayega → Allow karo
4. Install karo

---

## STEP 6: Phone Mein Setup

App khulne par:
1. **Saari permissions allow karo** (microphone, contacts, phone)
2. **Battery optimization** - "Don't optimize" select karo (important!)
3. **Start** button dabao
4. Bolke dekho: **"Hi Moto"**

---

## Kaise Use Karo:

| Aap bolein | Kya hoga |
|-----------|---------|
| "Hi Moto" | Activate hoga, "Haan boliye" bolega |
| "Open WhatsApp" | WhatsApp khulega |
| "Open YouTube" | YouTube khulega |
| "Open Settings" | Settings khulegi |
| "Call Sourav" | Sourav ko call lagega (contacts mein hona chahiye) |
| "Call band karo" | Chalu call kaat dega |
| "Aaj ka time kya hai?" | Time batayega |
| "Aaj ki date?" | Date batayega |
| "Capital of India?" | AI se jawab dega |
| Koi bhi sawaal | AI jawab dega Hindi mein |

---

## Supported Apps (Voice se Open):
- WhatsApp, Instagram, Facebook, Telegram, Snapchat
- YouTube, Spotify, Netflix
- Gmail, Chrome, Maps
- Calculator, Clock, Calendar, Camera
- Paytm, GPay, PhoneP
- Play Store, Settings

---

## Troubleshooting:

**App sun nahi raha?**
- Check karo ki service start hai (green status dikhe)
- Internet connection check karo (Google speech recognition ke liye)
- Microphone permission check karo

**Call nahi lag raha?**
- Contacts permission check karo
- Naam exactly waisa bolo jaisa contact mein save hai

**Background mein band ho jaata hai?**
- Battery optimization off karo: Settings → Battery → App Battery Usage → Hi Moto → Unrestricted
- MIUI/One UI mein: Settings → Apps → Hi Moto → Battery → No restrictions

**AI sawaalon ke jawab nahi aate?**
- API key check karo
- Internet connection check karo

---

## Technical Details:
- **Language**: Java (Android Native)
- **Min Android**: 8.0 (API 26)
- **Speech**: Android SpeechRecognizer (Google)
- **TTS**: Android TextToSpeech
- **AI**: Claude Haiku API
- **Background**: Foreground Service
- **Auto-start**: Boot Broadcast Receiver
