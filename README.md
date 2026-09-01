<p align="center">
  <img src="fastlane/metadata/android/en-US/images/icon.png" width="112" height="112" alt="Iris Gallery" style="border-radius: 24px;" />
</p>

<h1 align="center">Iris Gallery</h1>

<p align="center">
  A fast, private, 100% offline gallery app for Android built with Jetpack Compose and Material 3.
</p>

<p align="center">
  <a href="https://github.com/MohamadOday/iris-gallery/releases/latest"><img src="https://img.shields.io/github/v/release/MohamadOday/iris-gallery?label=GitHub%20Release&color=blue" alt="Latest Release" /></a>
  <a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/MohamadOday/iris-gallery"><img src="https://img.shields.io/badge/Obtainium-Add%20App-4080FF?logo=android&logoColor=white" alt="Add to Obtainium" /></a>
  <a href="https://f-droid.org/packages/com.iris.gallery/"><img src="https://img.shields.io/badge/F--Droid-Available-3DDC84?logo=f-droid&logoColor=white" alt="F-Droid" /></a>
  <a href="https://apt.izzysoft.de/fdroid/index/apk/com.iris.gallery"><img src="https://img.shields.io/badge/IzzyOnDroid-Repository-009688?logo=f-droid&logoColor=white" alt="IzzyOnDroid" /></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-green.svg" alt="Android 8.0+" />
  <img src="https://img.shields.io/badge/Network-None%20(100%25%20Offline)-brightgreen.svg" alt="100% Offline" />
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-orange.svg" alt="License" /></a>
</p>

---

> [!CAUTION]
> **Dev Notice**
> 
> We do not welcome contributions, collaboration, or support from individuals, companies, or organizations that justify, stand , defend or participate with/in Israeli war crimes, illegal settlement expansion, interventions , actions , or apartheid . 
> 
> Stand for justice, or do not stand with this project.

---

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1_photos_timeline.png" width="18%" />
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2_albums_grid.png" width="18%" />
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4_photo_viewer.png" width="18%" />
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/5_photo_editor.png" width="18%" />
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/8_app_lock.png" width="18%" />
</p>

Iris Gallery is designed to be a private, responsive, and customizable local media viewer. It has zero network permissions, opens instantly, and stays out of your way.

## Download & Installation

### 1. Obtainium (Recommended for Auto-Updates)
Track and install updates directly from GitHub Releases:
- **One-Click Add**: Click the badge below to add Iris Gallery directly into Obtainium:
  <p align="left">
    <a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/MohamadOday/iris-gallery"><img src="https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png" alt="Get it on Obtainium" height="52" /></a>
  </p>
- **Manual Setup**: Open **Obtainium** → **Add App** → Paste `https://github.com/MohamadOday/iris-gallery` → Click **Add**.

### 2. GitHub Releases
Download the signed APK directly from the [Releases](https://github.com/MohamadOday/iris-gallery/releases/latest) page.

### 3. F-Droid & IzzyOnDroid
- **F-Droid**: Available on [F-Droid](https://f-droid.org/packages/com.iris.gallery/).
- **IzzyOnDroid**: Available via the [IzzyOnDroid F-Droid repository](https://apt.izzysoft.de/fdroid/index/apk/com.iris.gallery).

---

## Highlights

- **100% Offline & Private**: Zero `INTERNET` permission in the manifest. Photos, videos, and metadata never leave your device.
- **Live Media Sync**: Automatic real-time timeline detection for new camera photos and downloads without needing app restarts.
- **Fluid Grid & Pinch Resize**: Smoothly resize the photos grid (2–6 columns) and albums grid (1–4 columns) with responsive pinch gestures.
- **Built-in Photo Editor**: Crop with aspect presets, rotate, flip, adjustments (Brightness, Contrast, Saturation, Warmth), freehand blur/pixelate brushes, resize, and external editor integration (Snapseed, Lightroom, ImageToolbox).
- **High-Res Viewer & EXIF/GPS Inspector**: Hardware canvas rendering with multi-level zoom (up to 7×), detailed camera metadata (ISO, aperture, shutter speed, focal length), and one-tap map location launcher.
- **Media3 Video Player**: Powered by ExoPlayer with sensor-aware hardware rotation, gesture seeking, auto-play, and mute controls.
- **App Lock & Private Vault**: Protect the entire app or media picker with a custom PIN and biometric fingerprint unlock, plus a dedicated encrypted vault for sensitive media.
- **Safe 30-Day Trash**: Move deleted items to the trash with easy restoration or one-tap permanent purge.
- **Album Management**: Create custom albums, move/copy media between folders, customize album covers, pin favorites, and sort by name, date, or count.
- **Duplicate & Similar Photo Finder**: Scan and clean up redundant media to reclaim device storage.
- **Smart Media Categorization**: Dedicated filters for RAW photos, animated GIFs, Motion Photos, Panoramas, and Screenshots.
- **Home Screen Photo Widget**: Customizable home screen widget with automatic memory rotation.
- **Multilingual Support**: Fully localized in 13+ languages with in-app language switcher.
- **Deep Customization**: Material You dynamic theming, pure AMOLED dark mode, adjustable corner rounding, startup tab selection, and thumbnail grid density sliders.

## Building

### Requirements
- Android SDK 35+ / Build-Tools 36.0.0+
- JDK 17+

### Build from source

```bash
git clone https://github.com/MohamadOday/iris-gallery.git
cd iris-gallery

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

The APK will be generated under `app/build/outputs/apk/`.

## Privacy

Iris Gallery does not include any analytics, crash reporters, telemetry, or network-enabled libraries. The app cannot make network requests at runtime because network permissions are completely omitted from `AndroidManifest.xml`.

## License

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
