#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

PACKAGE="com.iris.gallery"
APK="$SCRIPT_DIR/app/build/outputs/apk/optimized/app-optimized.apk"

NO_DEPLOY=false

if [[ "${1:-}" == "--no-deploy" ]]; then
    NO_DEPLOY=true
    shift
fi

echo "Building Iris Gallery (desktop / non-Termux host)..."

# On desktop Linux/macOS/Windows, override the Termux-specific AAPT2 path so AGP uses standard binaries
if command -v aapt2 >/dev/null 2>&1; then
    AAPT2_PATH="$(command -v aapt2)"
    ./gradlew -Pandroid.aapt2FromMavenOverride="$AAPT2_PATH" assembleOptimized "$@"
else
    # Passing empty override allows AGP to fetch and execute standard desktop AAPT2 from Maven
    ./gradlew -Pandroid.aapt2FromMavenOverride="" assembleOptimized "$@"
fi

if [ ! -f "$APK" ]; then
    echo "Build failed: APK not found at $APK"
    exit 1
fi

echo "Build successful: $APK"

if $NO_DEPLOY; then
    echo "Deployment skipped (--no-deploy). APK is ready at: $APK"
    exit 0
fi

if command -v adb >/dev/null 2>&1; then
    echo "Deploying to device via ADB..."
    adb install -r -d "$APK"
    adb shell am start -n "$PACKAGE/.MainActivity"
    echo "Iris Gallery deployed and running."
else
    echo "ADB not found. APK is ready at: $APK"
fi
