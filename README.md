# AMOLED Line Fixer

This Flutter application provides a drag-to-move overlay to cover AMOLED screen defects (like vertical lines). It includes support for Android 14+ Foreground Services to ensure the overlay remains active.

## Features

- **Drag-to-Move**: Position the overlay exactly where the defect is.
- **Thickness Control**: Adjust the thickness of the line.
- **Orientation Control**: Switch between vertical and horizontal lines.
- **Android 14+ Support**: Uses a foreground service to prevent the system from killing the overlay.

## Setup

This repository contains the core source files. To run this project, you need a Flutter environment.

1. **Create a Flutter Project** (if you haven't already):
   ```bash
   flutter create amoled_overlay_fix
   cd amoled_overlay_fix
   ```

2. **Replace Files**:
   Copy the files from this repository into your Flutter project, preserving the directory structure.
   - `lib/main.dart` -> `lib/main.dart`
   - `android/app/src/main/AndroidManifest.xml` -> `android/app/src/main/AndroidManifest.xml`
   - `android/app/src/main/kotlin/com/example/amoled_overlay_fix/OverlayService.kt` -> `android/app/src/main/kotlin/com/example/amoled_overlay_fix/OverlayService.kt`
   - `android/app/src/main/kotlin/com/example/amoled_overlay_fix/MainActivity.kt` -> `android/app/src/main/kotlin/com/example/amoled_overlay_fix/MainActivity.kt`

   *Note: Ensure your `android/app/build.gradle` has `applicationId "com.example.amoled_overlay_fix"` or update the package name in the Kotlin files and Manifest to match your project.*

3. **Run**:
   ```bash
   flutter run
   ```

4. **Permissions**:
   - The app will request "Display over other apps" permission. Grant it.
   - The app uses a Foreground Service.

## Troubleshooting

- **Missing Resources**: If you encounter errors about missing resources (like styles or icons), ensure you have standard Flutter Android resources in `android/app/src/main/res`.
- **Package Name**: If you changed the package name when creating the project, update `package` in `AndroidManifest.xml` and `package` declaration in Kotlin files.
