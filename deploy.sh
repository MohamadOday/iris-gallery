#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

RISH="${HOME}/shizuku/rish"
SDK="${PREFIX}/opt/android-sdk"
APK="$SCRIPT_DIR/app/build/outputs/apk/optimized/app-optimized.apk"
DEVICE_APK="/data/local/tmp/iris-gallery-debug.apk"
PACKAGE="com.iris.gallery"

if [ ! -f "$RISH" ]; then
    echo "Shizuku rish was not found at $RISH"
    exit 1
fi

GRADLEW="$SCRIPT_DIR/gradlew"
if [ ! -f "$GRADLEW" ]; then
    GRADLEW="$SCRIPT_DIR/../iris/gradlew"
fi

echo "Building Iris Gallery (assembleOptimized)…"
ANDROID_HOME="$SDK" "$GRADLEW" assembleOptimized

if [ ! -f "$APK" ]; then
    echo "Build failed: APK not found at $APK"
    exit 1
fi

echo "Transferring APK…"
"$RISH" -c "cat > $DEVICE_APK" < "$APK"

echo "Installing and launching…"
"$RISH" -c "pm install -r -d $DEVICE_APK"
"$RISH" -c "am start -n $PACKAGE/.MainActivity"

echo "Iris Gallery is deployed and running."
