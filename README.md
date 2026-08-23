<p align="center">
  <img src="fastlane/metadata/android/en-US/images/icon.png" width="128" height="128" alt="Iris Gallery Icon" style="border-radius: 28px;" />
</p>

<h1 align="center">Iris Gallery</h1>

<p align="center">
  <b>A modern, fluid, and 100% offline gallery app for Android built with Jetpack Compose & Material You 3.</b>
</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License" /></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-green.svg" alt="Android Version" />
  <img src="https://img.shields.io/badge/Privacy-100%25%20Offline-success.svg" alt="100% Offline" />
  <img src="https://img.shields.io/badge/Network%20Permissions-NONE-brightgreen.svg" alt="Zero Internet Permissions" />
  <img src="https://img.shields.io/badge/Kotlin-2.1.0-purple.svg" alt="Kotlin Version" />
</p>

---

## ✨ Features

### 🔒 100% Offline & Private by Design
- **Zero Internet Permissions**: `android.permission.INTERNET` is completely absent from `AndroidManifest.xml`.
- Your photos, videos, and metadata never leave your device. No cloud analytics, no telemetry, no tracking.

### 🎨 Material You 3 & Dynamic Theming
- Native **Dynamic Color** theming that adapts automatically to your system wallpaper.
- Fluid light and dark themes with edge-to-edge system bar integration and dynamic splash screen.

### 🔍 Crystal-Clear High-Resolution Viewer
- Native hardware canvas rendering directly rasterizes full-resolution source bitmap pixels without downsampling.
- Multi-level double-tap zoom (up to 7×) preserving crystal-clear clarity on screenshots, documents, and fine details.

### ✂️ Built-in Photo Editor
- **Transform**: Rotate 90° clockwise/counterclockwise, 180°, and flip horizontally ($\rightleftarrows$) or vertically ($\updownarrow$).
- **Crop**: Aspect ratio presets (Free, 1:1, 4:3, 16:9) with interactive touch handles.
- **Filters**: Instant color grading (B&W, Warm, Cool, Vivid, Vintage, Sepia).
- **Draw & Markup**: Smooth freehand brush sketches with adjustable stroke width and color palette.
- **Text**: Add customizable captions with live color styling.

### 📷 Rich EXIF Metadata & GPS Map Launcher
- Complete camera specifications: Camera Model, Aperture ($f/\text{stop}$), Shutter Speed, Focal Length, ISO, and Flash status.
- Interactive GPS Card displaying latitude/longitude with a direct **Map** button to launch your preferred navigation app.

### 🎬 Integrated Media3 Video Player
- Seamless ExoPlayer playback with hardware-accelerated sensor rotation.
- Volume, progress scrubbing, and play/pause controls.

### 🤏 Pinch-to-Resize Density
- Place two fingers on the photo grid to adjust density between **2 and 6 columns**.
- Fluid tile layout animations powered by Jetpack Compose `Modifier.animateItem()`.

### 🗂️ Format Categorization
- Smart filtering for **RAW** photographs, **GIFs**, **Motion Photos**, **Panoramas**, and **Screenshots**.

### 🧹 Duplicate & Redundant Media Finder
- On-device file scanner identifies duplicate or redundant photos to help free up storage space.

### 🔐 Biometric Locked Vault & Trash Bin
- **Locked Vault**: Protect sensitive photos behind biometric fingerprint/face unlock or device lock PIN.
- **Trash Bin**: Safe deletion with one-tap restoration and a safe **Empty Trash** purge button.

### 📱 Dynamic Home Screen Widget
- Pin photos directly to your Android launcher with customizable aspect ratios, corner rounding, and automatic background rotation.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.1
- **UI Toolkit**: Jetpack Compose (BOM 2025.08.01) with Material 3
- **Image Loading**: Coil 3 (`io.coil-kt.coil3`)
- **Video Playback**: Media3 ExoPlayer (`androidx.media3`)
- **Metadata**: AndroidX `ExifInterface`
- **Architecture**: MVI / MVVM with unidirectional data flow and Kotlin Coroutines

---

## 📦 Building from Source

### Prerequisites
- Android SDK 36 (Build Tools 36.0.0+)
- JDK 17+ (e.g. OpenJDK 17 or Zulu JDK 17)

### Build Commands

Clone the repository:
```bash
git clone https://github.com/MohamadOday/iris-gallery.git
cd iris-gallery
```

Build the release APK:
```bash
./gradlew assembleRelease
```

The compiled APK will be located at:
```
app/build/outputs/apk/release/app-release-unsigned.apk
```

---

## 📄 License

```
Copyright 2026 Mohamad Oday

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
