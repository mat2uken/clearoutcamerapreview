# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Clean project
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install debug APK on connected device
./gradlew installDebug

# Run all tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Run lint checks
./gradlew lint

# Run a single test class
./gradlew test --tests "app.mat2uken.android.app.clearoutcamerapreview.YourTestClass"

# Run with detailed output
./gradlew build --info
```

## App Overview

内蔵カメラの映像をプレビューするアプリです。
アプリを起動すると、カメラを初期化し、カメラからキャプチャした映像をアプリ画面にフルスクリーンでプレビューすることができます。

また、以下の機能を備えます。
- カメラ機能
  - ズーム率の変更
    - それぞれのハードウェアが備える範囲をハードウェアから拡大、縮小ができる最小値と最大値を取得し、それを変更できるスライダー
  - カメラの種別（背面カメラ、前面カメラなど）もしくはレンズの切り替えが可能になるプルダウンメニュー
- 映像出力機能
  - カメラ映像のプレビュー 
    - アプリ内にフルスクリーンでプレビュー
    - 外部ディスプレイが接続されている場合は、そちらに対してもフルスクリーンで表示
      - 外部ディスプレイが接続されプレビューを表示しているときも同様の内容をアプリ内の端末の画面にもフルスクリーンのプレビューは同時に表示される
- 外部ディスプレイの検出機能

## Architecture Overview

This is an Android application built with Kotlin and Jetpack Compose. The project follows a single-module structure with the package `app.mat2uken.android.app.clearoutcamerapreview`.

### Key Technology Stack
- **Language**: Kotlin 1.9.0
- **UI Framework**: Jetpack Compose (Material 3)
- **Camera Library**: CameraX 1.3.4
- **Permission Management**: Accompanist Permissions 0.32.0
- **Min SDK**: 31 (Android 12)
- **Target SDK**: 35 (Android 15)
- **Build System**: Gradle 8.7 with Kotlin DSL
- **Java Compatibility**: Java 11

### Project Structure
- `MainActivity.kt`: Main entry point, handles permissions and app initialization
- `CameraScreen.kt`: Main camera UI with preview, zoom slider, and camera selector
- `ui/theme/`: Contains Compose theming components (Color, Theme, Type)
- Dependencies are managed via version catalog in `gradle/libs.versions.toml`

### Key Components
1. **SimplifiedMultiDisplayCameraScreen**: Main composable that handles camera functionality with external display support
2. **SimpleCameraPresentation**: Android Presentation class for showing camera preview on external displays
3. **CameraSelectorDropdown**: UI for switching between front/back cameras
4. **ZoomSlider**: Controls for adjusting zoom level
5. **CameraPermissionScreen**: Manages camera permission requests
6. **ExternalDisplayManager**: Manages external display detection and state (not currently used in active implementation)

### Testing
- Unit tests are located in `app/src/test/`
- 31 unit tests covering core logic, validation, and edge cases
- Test files: `SimpleUnitTest.kt`, `CameraLogicTest.kt`, `ValidationTest.kt`
- Use MockK for mocking dependencies

### Development Notes
- The project uses Gradle wrapper (`./gradlew`) for consistent builds
- ProGuard is currently disabled for release builds
- Compose compiler extension version is 1.5.1
- CameraX requires camera permission handling before initialization
- Zoom ranges are device-specific and obtained from camera hardware

### External Display Support
- **Display Detection**: Uses DisplayManager to detect external display connections/disconnections
- **Dual Preview**: Shows camera preview simultaneously on both device screen and external display
- **Resolution Selection**: Prioritizes 1920x1080 resolution, falls back to closest 16:9 aspect ratio
- **Rotation Handling**: Applies appropriate rotation for portrait/landscape displays
  - CameraX `setTargetRotation(ROTATION_90)` + PreviewView 180° rotation = correct orientation
- **Aspect Ratio**: Maintains 16:9 aspect ratio with letterboxing as needed
- **Hot-plug Support**: Handles runtime display connection/disconnection without crashes

### Test Device
- Device IP: 192.168.0.238:5555
- ADB commands:
  ```bash
  ~/Library/Android/sdk/platform-tools/adb -s 192.168.0.238:5555 install -r app/build/outputs/apk/debug/app-debug.apk
  ~/Library/Android/sdk/platform-tools/adb -s 192.168.0.238:5555 shell am start -n app.mat2uken.android.app.clearoutcamerapreview/.MainActivity
  ```