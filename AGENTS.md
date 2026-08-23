# AGENTS.md — Iris Gallery Context & Developer Guide

This document contains critical project context, environment quirks, workarounds, and architecture decisions so future agents can immediately pick up work without re-investigating.

---

## 1. Project Identity & Concept

- **App Name**: Iris Gallery (`com.iris.gallery`)
- **Core Concept**: Named after the **Iris flower** (botanical flower with purple/violet/indigo/amber petals), **NOT** the anatomical eye or camera aperture.
- **Privacy Policy**: 100% offline gallery app with **zero** `INTERNET` permission in `AndroidManifest.xml`.
- **License**: Apache-2.0.

---

## 2. Termux / Android Build Environment & Workarounds

Building Android apps directly inside Termux on `aarch64` has specific quirks:

### A. Android SDK & AAPT2 Override
- **Android SDK Path**: `${PREFIX}/opt/android-sdk` (`/data/data/com.termux/files/usr/opt/android-sdk`)
- **AAPT2 Binary**: Termux `aarch64` cannot execute x86_64 binaries fetched by Gradle from Maven.
- **`gradle.properties` Override**:
  ```properties
  android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/opt/android-sdk/build-tools/36.0.0/aapt2
  ```
  *Do not remove this override or resource processing (`processOptimizedResources`) will fail.*

### B. Gradle Wrapper
- The Gradle wrapper is available directly in the project (`./gradlew`) and also falls back to `../iris/gradlew`.
- Always run Gradle with `ANDROID_HOME` set:
  ```bash
  ANDROID_HOME="${PREFIX}/opt/android-sdk" ./gradlew assembleOptimized
  ```

### C. Shizuku / Rish Deployment (`deploy.sh`)
- Deploying directly to the host device uses Shizuku `rish` located at `~/shizuku/rish`.
- Running `bash deploy.sh` automatically compiles/builds `assembleOptimized`, transfers the APK to `/data/local/tmp/iris-gallery-debug.apk`, installs it via `pm install -r -d`, and launches `.MainActivity` in a single command.
- **Launcher Icon Cache Workaround**:
  Modern Android launchers (like Smart Launcher / `ginlemon.flowerfree` or Pixel Launcher) aggressively cache app icons in memory/disk. When updating app icons, always restart the launcher to refresh the icon cache:
  ```bash
  "$HOME/shizuku/rish" -c "am force-stop ginlemon.flowerfree"
  ```

---

## 3. Signing Keys & Keystores

### Iris Gallery Release Key
- **Keystore File**: `~/iris-gallery-release.jks` (`/data/data/com.termux/files/home/iris-gallery-release.jks`)
- **Alias**: `iris-gallery`
- **Keystore Password**: `IrisGallery@2026`
- **Key Password**: `IrisGallery@2026`
- **Algorithm**: 2048-bit RSA (`SHA384withRSA`)
- **Validity**: Until 2054 (10,000 days)
- **SHA-256 Fingerprint**: `90:EF:CB:06:86:7F:89:A5:D7:1B:5A:5D:5A:E6:8F:1A:1D:F1:C9:52:D9:03:65:34:80:FF:96:C1:7F:45:AF:D2`

### Sibling Project Key (Iris Keyboard)
- **Keystore File**: `~/iris-release-key.jks` (`/data/data/com.termux/files/home/iris-release-key.jks`)
- **Alias**: `iris`

---

## 4. App Icon Architecture & Rules

The app icon follows the **Samsung / Pixel Gallery Style**: a sharp, geometric white Iris flower glyph on a vibrant violet-to-indigo gradient background.

### Directory Layout
- **Adaptive Definition**: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
  ```xml
  <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
      <background android:drawable="@drawable/ic_launcher_background" />
      <foreground android:drawable="@mipmap/ic_launcher_foreground" />
      <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
  </adaptive-icon>
  ```
- **Manifest**: `AndroidManifest.xml` points to `android:icon="@mipmap/ic_launcher"` and `android:roundIcon="@mipmap/ic_launcher"`.
- **Background**: `app/src/main/res/drawable/ic_launcher_background.xml` (Full-bleed linear gradient `#9D34F5` -> `#4F16D8`).
- **Foreground Glyphs**: Multi-density PNGs in `mipmap-mdpi`, `mipmap-hdpi`, `mipmap-xhdpi`, `mipmap-xxhdpi`, `mipmap-xxxhdpi`.
  - **Centering Rule**: The flower glyph must be centered at `(canvas_size/2, canvas_size/2)` with equal padding on all four sides and sized to ~56% of canvas size to stay within the 66% adaptive circular safe zone.
- **Themed Icons**: `app/src/main/res/drawable/ic_launcher_monochrome.xml` (Android 13+ dynamic color mask).

> [!WARNING]
> **Never add static `ic_launcher.png` raster files** into `drawable-*` or `mipmap-*` density directories alongside adaptive icons. In Android resource resolution, density qualifiers take precedence over API level qualifiers (`-v26`), which will bypass the adaptive icon system and show a non-adaptive box.

---

## 5. Performance & UI Optimizations in `MainActivity.kt`

Key performance decisions made to eliminate tab switching lag and scroll stutter:

1. **`beyondViewportPageCount = 3` in `HorizontalPager`**:
   - With 4 tabs (0: Photos, 1: Albums, 2: Favorites, 3: Library), being on Tab 3 would otherwise destroy Tab 0 if `beyondViewportPageCount < 3`.
   - Keeping `beyondViewportPageCount = 3` ensures Tab 0 remains in memory, eliminating synchronous recomposition lag when switching between Library and other tabs.

2. **Cached `DateTimeFormatter` & `dayCache`**:
   - `timelinePatternSameYear` (`"EEEE, MMMM d"`) and `timelinePatternOtherYear` (`"EEEE, MMMM d, yyyy"`) are statically cached.
   - `dayCache` in `PhotoGrid` groups photos per day with `remember(photos)` so string formatting runs once per unique day instead of once per photo.

3. **Zero-Allocation Scroll Tracking**:
   - `visibleDate` computes the current timeline header using index scan loops instead of `.drop()` or `.take()` list allocations.

4. **Isolated `LazyGridState`**:
   - `libraryGridState` is separate from `favoriteGridState` to prevent scroll position conflicts when navigating inside Library subpages.

---

## 6. F-Droid & Release Checklist

To prepare a release for F-Droid or GitHub Releases:
1. Ensure `gradlew` wrapper and `gradle/wrapper/` exist in `iris-gallery` root (or copy from `../iris/gradlew`).
2. Verify `LICENSE` (Apache-2.0) is present in repository root.
3. Tag the release commit (e.g. `git tag v0.1.0`).
4. Build signed release:
   ```bash
   ANDROID_HOME="${PREFIX}/opt/android-sdk" ../iris/gradlew assembleRelease
   ```

---

## 7. Screen Orientation & Large Screen / Tablet Architecture

1. **Sensor-Aware Rotation in Video Player (`VideoPage.kt`)**:
   - The video rotation button requests `ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE` or `ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT` rather than fixed `SCREEN_ORIENTATION_LANDSCAPE`.
   - This allows the screen to automatically switch between normal landscape (90°) and reverse landscape (270°), or normal/reverse portrait, based on the physical position of the device.

2. **Automatic Orientation Reset on Viewer Dismissal (`PhotoViewer` in `MainActivity.kt`)**:
   - When `PhotoViewer` is closed (via back gesture/button, back arrow, item deletion, lock, or editing), a `DisposableEffect` resets `activity.requestedOrientation` to `ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED`.
   - On phones, this immediately restores the vertical portrait orientation (or respects the user's system auto-rotate lock).
   - On tablets and large screens (`sw600dp+`), `SCREEN_ORIENTATION_UNSPECIFIED` ensures the gallery smoothly returns to the tablet's natural/current orientation (e.g. landscape on a desk/stand) rather than forcing vertical phone portrait mode.

3. **Large Screen & Landscape UI Adaptations**:
   - Bottom floating action bars and video control rows use `Modifier.widthIn(max = 560.dp)` and `Modifier.widthIn(max = 720.dp)` to stay centered and ergonomic on large tablets / foldables.
   - Dialogs and bottom sheets (such as `PhotoDetailsSheet`) include `.verticalScroll()` to avoid clipping in landscape or compact-height window configurations.

