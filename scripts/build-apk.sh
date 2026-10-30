#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_DIR"

echo "=== 1. Esecuzione test nativi Java ==="
mkdir -p /tmp/ferro-tests
javac -d /tmp/ferro-tests \
    android/app/src/main/java/eu/sabotino/ferrorimbalzi/Game.java \
    tests/GameTest.java
java -ea -cp /tmp/ferro-tests eu.sabotino.ferrorimbalzi.GameTest

echo "=== 2. Verifica keystore debug ==="
if [ ! -f android/app/debug.keystore ]; then
    keytool -genkeypair -keystore android/app/debug.keystore \
        -storepass android -keypass android -alias androiddebugkey \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -dname "CN=Ferro e Rimbalzi Debug,O=Sabotino,C=IT"
fi

echo "=== 3. Compilazione APK Android (Gradle nativo verticale) ==="
mkdir -p /tmp/.android
export ANDROID_USER_HOME=/tmp/.android
android/gradlew -p android --offline --no-daemon assembleDebug

echo "=== 4. Preparazione pacchetto downloads ==="
mkdir -p downloads
cp android/app/build/outputs/apk/debug/app-debug.apk downloads/ferro-e-rimbalzi-0.8.1-debug.apk

echo "=== 5. Verifica integrità APK ==="
python3 scripts/check-apk.py

echo "=== 6. Generazione checksum SHA256 ==="
sha256sum downloads/ferro-e-rimbalzi-0.8.1-debug.apk > downloads/ferro-e-rimbalzi-0.8.1-debug.apk.sha256
cat downloads/ferro-e-rimbalzi-0.8.1-debug.apk.sha256
echo "Compilazione e verifica completate con successo!"

