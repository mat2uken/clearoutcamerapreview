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
  - 外部ディスプレイ映像の手動反転機能
    - 上下反転、左右反転を独立して制御可能
- 外部ディスプレイの検出機能
- 横向き固定表示（landscape orientation）
- オーバーレイサイドバーUI（タップで表示/非表示切り替え）

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
```
app/src/main/java/.../clearoutcamerapreview/
├── MainActivity.kt                    # Entry point, landscape orientation lock
├── SimplifiedMultiDisplayCameraScreen.kt # Main camera screen with dual display
├── camera/
│   └── CameraState.kt                # Immutable camera state management
├── utils/
│   ├── CameraUtils.kt                # Camera utilities (resolution, zoom, etc.)
│   ├── DisplayUtils.kt               # External display detection
│   └── PresentationHelper.kt         # External display preview calculations
├── model/
│   └── Size.kt                       # Custom Size class for testing
└── ui/theme/                         # Compose theming components
```

### Key Components
1. **SimplifiedMultiDisplayCameraScreen**: Main composable that handles camera functionality with external display support
   - Overlay sidebar UI with tap-to-toggle
   - Modal dialogs for all controls
   - Dual display preview management
   
2. **SimpleCameraPresentation**: Android Presentation class for showing camera preview on external displays
   - Automatic aspect ratio adjustment
   - Rotation compensation
   - Flip transformation support
   
3. **CameraState**: Immutable data class for camera state management
   - Camera selector (front/back)
   - Zoom levels and bounds
   - External display connection status
   
4. **UI Components**:
   - **SidebarSection**: Table-like section headers in sidebar
   - **StatusRow**: Read-only status display rows
   - **ClickableRow**: Interactive rows that open dialogs
   - **CameraSelectionDialog**: Modal for camera switching
   - **ZoomControlDialog**: Modal for zoom adjustment
   - **FlipControlDialog**: Modal for flip controls

### Testing
- Unit tests are located in `app/src/test/`
- 75+ unit tests covering all utility classes and helpers
- Test coverage approaches 100% for non-UI components
- Test files:
  - `CameraUtilsTest.kt`: Camera utility functions
  - `DisplayUtilsTest.kt`: Display detection logic
  - `CameraStateTest.kt`: State management
  - `PresentationHelperTest.kt`: External display calculations
- Use JUnit 4 and MockK for mocking

### Development Notes
- The project uses Gradle wrapper (`./gradlew`) for consistent builds
- ProGuard is currently disabled for release builds
- Compose compiler extension version is 1.5.1
- CameraX requires camera permission handling before initialization
- Zoom ranges are device-specific and obtained from camera hardware
- App is locked to landscape orientation using `sensorLandscape`

### External Display Support
- **Display Detection**: Uses DisplayManager to detect external display connections/disconnections
- **Dual Preview**: Shows camera preview simultaneously on both device screen and external display
- **Resolution Selection**: Prioritizes 1920x1080 resolution, falls back to closest 16:9 aspect ratio
- **Rotation Handling**:
  - Front camera: Special rotation compensation with 270° adjustment
  - Back camera: Fixed 180° rotation for external display
  - Supports both landscape orientations (normal and reverse)
- **Aspect Ratio**: Maintains 16:9 aspect ratio with letterboxing as needed
- **Hot-plug Support**: Handles runtime display connection/disconnection without crashes
- **Flip Controls**: Manual vertical and horizontal flip for external display adjustments

### UI Design
- **Overlay Sidebar**: 280dp wide sidebar that overlays on camera preview
- **Semi-transparent**: 95% opacity for subtle see-through effect
- **Tap to Toggle**: Tap anywhere on camera preview to show/hide sidebar
- **Modal Dialogs**: All controls open in centered modal dialogs
- **Menu Icon**: Shows when sidebar is hidden to indicate toggle capability

### Recent Implementation Changes
1. **Unit Test Infrastructure**: Extracted logic into testable utility classes
2. **Landscape Lock**: App forced to landscape with `sensorLandscape`
3. **Rotation Fixes**: Separate handling for front/back camera rotations
4. **Flip Controls**: Added manual V/H flip for external display
5. **UI Overhaul**: Sidebar converted to overlay with modal dialogs
6. **State Management**: Immutable state pattern for camera configuration

### Test Device
- Device IP: 192.168.0.238:5555
- ADB commands:
  ```bash
  ~/Library/Android/sdk/platform-tools/adb -s 192.168.0.238:5555 install -r app/build/outputs/apk/debug/app-debug.apk
  ~/Library/Android/sdk/platform-tools/adb -s 192.168.0.238:5555 shell am start -n app.mat2uken.android.app.clearoutcamerapreview/.MainActivity
  ```