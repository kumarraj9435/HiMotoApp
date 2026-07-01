#!/bin/bash
# APK Build Script - HiMotoApp folder ke andar se chalao

echo "=============================="
echo "  APK Build Shuru..."
echo "=============================="

# Paths set karo
export ANDROID_HOME=~/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/34.0.0
export PATH=$PATH:~/gradle/gradle-8.4/bin
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))

echo "Java: $(java -version 2>&1 | head -1)"
echo "Gradle: $(gradle --version 2>/dev/null | head -1)"

# Build karo
echo ""
echo "Building APK..."
gradle assembleDebug --no-daemon --stacktrace 2>&1

# Result check karo
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    echo ""
    echo "=============================="
    echo "✅ APK BAN GAYI!"
    echo "=============================="
    echo "APK yahan hai:"
    echo "  ~/HiMotoApp/$APK_PATH"
    echo ""
    echo "Install karne ke liye:"
    echo "  cp $APK_PATH /sdcard/HiMoto.apk"
    echo "Phir Files app se /sdcard/HiMoto.apk kholna"
    
    # Auto copy to sdcard
    cp "$APK_PATH" /sdcard/HiMoto.apk 2>/dev/null && echo "" && echo "✅ /sdcard/HiMoto.apk mein copy ho gayi!"
else
    echo ""
    echo "❌ Build fail hua. Error dekho upar."
fi
