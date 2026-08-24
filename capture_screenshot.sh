#!/usr/bin/env bash
# capture_screenshot.sh — Capture current device screen for F-Droid fastlane metadata

TARGET_DIR="fastlane/metadata/android/en-US/images/phoneScreenshots"
mkdir -p "$TARGET_DIR"

NAME="${1:-screenshot}"
INDEX=$(ls "$TARGET_DIR" | wc -l)
((INDEX++))

FILENAME="${INDEX}_${NAME}.png"
TMP_PATH="/data/local/tmp/iris_snap.png"

echo "Capturing current screen to $TARGET_DIR/$FILENAME..."
"$HOME/shizuku/rish" -c "screencap -p $TMP_PATH"
cp "$TMP_PATH" "$TARGET_DIR/$FILENAME"
echo "Saved: $TARGET_DIR/$FILENAME"
