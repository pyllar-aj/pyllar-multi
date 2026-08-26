 #!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# deploy-ios.sh – Build and run the iOS Pyllar app on the simulator
#
# Usage:
#   bash deploy-ios.sh [--flavor release|debug] [-f release|debug]
#
# Flavors:
#   release     (default) → https://api.pyllar.in
#   debug                 → http://localhost:8080
#   debug-prod            → https://api.pyllar.in (Debug build pointing to production)
# ─────────────────────────────────────────────────────────────────────────────
set -e

# ── Parse arguments ────────────────────────────────────────────────────────
FLAVOR="release"  # default flavor

while [[ $# -gt 0 ]]; do
    case "$1" in
        --flavor|-f)
            FLAVOR="${2:?'--flavor requires an argument: release|debug|debug-prod'}"
            shift 2
            ;;
        *)
            echo "Unknown argument: $1" >&2
            echo "Usage: bash deploy-ios.sh [--flavor release|debug|debug-prod]" >&2
            exit 1
            ;;
    esac
done

if [[ "$FLAVOR" != "release" && "$FLAVOR" != "debug" && "$FLAVOR" != "debug-prod" ]]; then
    echo "Invalid flavor '$FLAVOR'. Must be 'release', 'debug', or 'debug-prod'." >&2
    exit 1
fi

BASE_URL="https://api.pyllar.in"
if [[ "$FLAVOR" == "debug" ]]; then
    BASE_URL="http://localhost:8080"
fi

echo "==> Flavor: $FLAVOR  (baseUrl: $BASE_URL)"

# ── Simulator config ───────────────────────────────────────────────────────
SIMULATOR_UDID="A6D26BBA-A765-4A93-9E72-F7730AC04F8A"
SIMULATOR_NAME="Test iPhone 26.4"
DERIVED_DATA="/tmp/pyllar-ios-build"
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"

echo "==> Booting simulator if needed..."
STATUS=$(xcrun simctl list devices | grep "$SIMULATOR_UDID" | grep -o "(Booted\|Shutdown)" || true)
if [ "$STATUS" != "(Booted)" ]; then
    xcrun simctl boot "$SIMULATOR_UDID" || true
    open -a Simulator
    echo "    Waiting for simulator to boot..."
    xcrun simctl bootstatus "$SIMULATOR_UDID" -b
else
    echo "    $SIMULATOR_NAME already booted."
fi

if [[ "$FLAVOR" == "debug" ]]; then
    XCODE_CONFIGURATION="Debug"
    PYLLAR_FLAVOR="debug"
    BUNDLE_ID="com.pyllar.consumer.debug"
elif [[ "$FLAVOR" == "debug-prod" ]]; then
    XCODE_CONFIGURATION="Debug"
    PYLLAR_FLAVOR="release"
    BUNDLE_ID="com.pyllar.consumer.debug"
else
    XCODE_CONFIGURATION="Release"
    PYLLAR_FLAVOR="release"
    BUNDLE_ID="com.pyllar.consumer"
fi

echo "==> Building Xcode project..."
cd "$PROJECT_ROOT/iosApp"
xcodebuild \
    -project iosApp.xcodeproj \
    -scheme iosApp \
    -configuration "$XCODE_CONFIGURATION" \
    -destination "id=$SIMULATOR_UDID" \
    -derivedDataPath "$DERIVED_DATA" \
    PYLLAR_FLAVOR="$PYLLAR_FLAVOR" \
    clean build CODE_SIGNING_ALLOWED=YES CODE_SIGN_IDENTITY="-" | grep -E "(error:|warning: |BUILD (SUCCEEDED|FAILED))"

echo "==> Uninstalling and Installing app..."
xcrun simctl terminate "$SIMULATOR_UDID" "$BUNDLE_ID" 2>/dev/null || true
#xcrun simctl uninstall "$SIMULATOR_UDID" "$BUNDLE_ID" 2>/dev/null || true
xcrun simctl install "$SIMULATOR_UDID" "$DERIVED_DATA/Build/Products/${XCODE_CONFIGURATION}-iphonesimulator/Pyllar.app"

echo "==> Launching app (console output follows)..."
open -a Simulator
xcrun simctl launch --console-pty "$SIMULATOR_UDID" "$BUNDLE_ID"