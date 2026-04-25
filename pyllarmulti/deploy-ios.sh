 #!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# deploy-ios.sh – Build and run the iOS Pyllar app on the simulator
#
# Usage:
#   bash deploy-ios.sh [--flavor release|debug] [-f release|debug]
#
# Flavors:
#   release  (default) → https://api.pyllar.in
#   debug              → http://10.222.186.212:8080
# ─────────────────────────────────────────────────────────────────────────────
set -e

# ── Parse arguments ────────────────────────────────────────────────────────
FLAVOR="release"  # default flavor

while [[ $# -gt 0 ]]; do
    case "$1" in
        --flavor|-f)
            FLAVOR="${2:?'--flavor requires an argument: release|debug'}"
            shift 2
            ;;
        *)
            echo "Unknown argument: $1" >&2
            echo "Usage: bash deploy-ios.sh [--flavor release|debug]" >&2
            exit 1
            ;;
    esac
done

if [[ "$FLAVOR" != "release" && "$FLAVOR" != "debug" ]]; then
    echo "Invalid flavor '$FLAVOR'. Must be 'release' or 'debug'." >&2
    exit 1
fi

BASE_URL="https://api.pyllar.in"
if [[ "$FLAVOR" == "debug" ]]; then
    BASE_URL="http://10.222.186.212:8080"
fi

echo "==> Flavor: $FLAVOR  (baseUrl: $BASE_URL)"

# ── Simulator config ───────────────────────────────────────────────────────
SIMULATOR_UDID="F6DC6730-7DCA-4536-AEE0-F730A91AE88B"
SIMULATOR_NAME="iPhone 17 Pro"
BUNDLE_ID="com.pyllar.consumer.Pyllar"
DERIVED_DATA="/tmp/pyllar-ios-build"
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"

echo "==> Booting simulator if needed..."
STATUS=$(xcrun simctl list devices | grep "$SIMULATOR_UDID" | grep -o "(Booted\|Shutdown)")
if [ "$STATUS" != "(Booted)" ]; then
    xcrun simctl boot "$SIMULATOR_UDID" || true
    open -a Simulator
    echo "    Waiting for simulator to boot..."
    xcrun simctl bootstatus "$SIMULATOR_UDID" -b
else
    echo "    $SIMULATOR_NAME already booted."
fi

XCODE_CONFIGURATION=$([ "$FLAVOR" = "debug" ] && echo "Debug" || echo "Release")

echo "==> Building Xcode project..."
cd "$PROJECT_ROOT/iosApp"
xcodebuild \
    -project iosApp.xcodeproj \
    -scheme iosApp \
    -configuration "$XCODE_CONFIGURATION" \
    -destination "id=$SIMULATOR_UDID" \
    -derivedDataPath "$DERIVED_DATA" \
    clean build | grep -E "(error:|warning: |BUILD (SUCCEEDED|FAILED))"

echo "==> Installing app..."
xcrun simctl terminate "$SIMULATOR_UDID" "$BUNDLE_ID" 2>/dev/null || true
xcrun simctl install "$SIMULATOR_UDID" "$DERIVED_DATA/Build/Products/${XCODE_CONFIGURATION}-iphonesimulator/Pyllar.app"

echo "==> Launching app (console output follows)..."
open -a Simulator
xcrun simctl launch --console-pty "$SIMULATOR_UDID" "$BUNDLE_ID"