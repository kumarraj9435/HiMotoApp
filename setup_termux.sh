#!/bin/bash
# Hi Moto APK - Termux Build Script
# Ek ek command copy karke chalao

echo "=============================="
echo "  Hi Moto APK Builder"
echo "=============================="

# Step 1: Packages install
echo "[1/6] Packages install ho rahe hain..."
pkg update -y && pkg upgrade -y
pkg install -y openjdk-17 wget unzip git

# Step 2: Android SDK download
echo "[2/6] Android SDK download ho raha hai..."
mkdir -p ~/android-sdk/cmdline-tools
cd ~/android-sdk/cmdline-tools
wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O tools.zip
unzip -q tools.zip
mv cmdline-tools latest
rm tools.zip

# Step 3: Environment variables set karo
echo "[3/6] Environment setup..."
export ANDROID_HOME=~/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/34.0.0

# Save to bashrc (permanent)
echo 'export ANDROID_HOME=~/android-sdk' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/34.0.0' >> ~/.bashrc

# Step 4: SDK components install
echo "[4/6] Android SDK components install ho rahe hain (10-15 min lagenge)..."
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

echo "[5/6] Gradle download..."
wget -q https://services.gradle.org/distributions/gradle-8.4-bin.zip -O ~/gradle.zip
mkdir -p ~/gradle
unzip -q ~/gradle.zip -d ~/gradle
export PATH=$PATH:~/gradle/gradle-8.4/bin
echo 'export PATH=$PATH:~/gradle/gradle-8.4/bin' >> ~/.bashrc

echo "[6/6] Setup complete!"
echo ""
echo "Ab build karne ke liye chalao:"
echo "  cd ~/HiMotoApp && bash build.sh"
