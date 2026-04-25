#!/bin/bash
set -e

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
APK_PATH="$PROJECT_ROOT/composeApp/build/outputs/apk/debug/composeApp-debug.apk"

echo "==> Checking for connected Android device..."
DEVICES=$(adb devices | tail -n +2 | grep -v "^$" | grep "device$")
if [ -z "$DEVICES" ]; then
    echo "ERROR: No Android device connected. Connect a device with USB debugging enabled."
    exit 1
fi
echo "    Found device(s):"
echo "$DEVICES" | awk '{print "    " $0}'

echo "==> Building debug APK..."
cd "$PROJECT_ROOT"
./gradlew :composeApp:assembleDebug --no-daemon

echo "==> Installing APK..."
adb install -r "$APK_PATH"

echo "==> Launching app..."
adb shell am start -n "com.pyllar.consumer/.MainActivity"

echo "==> Done."
