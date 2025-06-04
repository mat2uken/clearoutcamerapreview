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
  - 映像キャプチャフォーマットの自動選択
    - 解像度
      - 1920x1080が選択可能であれば最優先で選択する
      - それ以外では、16:9にアスペクト比が最も近く、1920x1080以下の解像度のものを選択する
    - フレームレート
      - 選択された解像度の中で、60fpsが選べる場合は最優先でそれを選ぶ
      - それ以外では、60fps以下で最も高いフレームレートを選択する
      - 実際に使用されているフレームレートの検出と表示
        - CameraX で明示的にフレームレートが設定されていない場合も、実際のキャプチャフレームレートを検出
        - 1920x1080の解像度では60fps優先、その他の解像度では30fps優先で自動選択
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
- オーバーレイサイドバーUI
  - 専用の折り畳みボタンで表示/非表示切り替え
  - サイドバー右上の「>」ボタンで折り畳み
  - 非表示時は右上に「<」ボタンを表示して再展開可能
  - OSのステータスバーを考慮した位置調整
- オーディオ機能
  - 外部スピーカー接続時に自動的にマイク録音開始
  - マイクからスピーカーへのオーディオパススルー
  - リアルタイムオーディオデバイス監視
  - オーディオ設定
    - サンプルレート優先度: 48kHz > 44.1kHz > 最高利用可能
    - 16ビット深度 (PCM)
    - ステレオ対応（デバイスがサポートする場合、それ以外はモノラル）
    - 内部スピーカーのみの場合は自動ミュート
  - UI表示
    - マイクデバイス名とオーディオフォーマット表示
    - 出力デバイス情報表示
    - ミュート切り替えコントロール
    - 出力デバイス選択機能（クリックして選択可能）
- 設定の永続化
  - 下記の内容をローカルDBに対して設定変更時などで適切に保存して、次回起動時には同じ設定で起動できる
    - 外部ディスプレイとカメラの組み合わせごとの上下反転、左右反転の設定
      - 各外部ディスプレイと各カメラ（前面/背面）の組み合わせで独立した設定を保持
      - カメラ切り替え時に自動的に該当する設定を読み込み
    - 最後に選択されていたカメラの種別
    - カメラごとのズーム率

## Architecture Overview

This is an Android application built with Kotlin and Jetpack Compose. The project follows a single-module structure with the package `app.mat2uken.android.app.clearoutcamerapreview`.

### Key Technology Stack
- **Language**: Kotlin 1.9.0
- **UI Framework**: Jetpack Compose (Material 3)
- **Camera Library**: CameraX 1.3.4
- **Audio APIs**: AudioRecord, AudioTrack, AudioManager
- **Permission Management**: Accompanist Permissions 0.32.0
- **Min SDK**: 31 (Android 12)
- **Target SDK**: 35 (Android 15)
- **Build System**: Gradle 8.7 with Kotlin DSL
- **Java Compatibility**: Java 11
- **Database**: Room 2.6.1 with KSP (Kotlin Symbol Processing)
- **Annotation Processing**: KSP 1.9.0-1.0.13

### Project Structure
```
app/src/main/java/.../clearoutcamerapreview/
├── MainActivity.kt                    # Entry point, landscape orientation lock
├── SimplifiedMultiDisplayCameraScreen.kt # Main camera screen with dual display
├── camera/
│   └── CameraState.kt                # Immutable camera state management
├── utils/
│   ├── CameraUtils.kt                # Camera utilities (resolution, zoom, etc.)
│   ├── CameraRotationHelper.kt       # Camera rotation calculation logic
│   ├── DisplayUtils.kt               # External display detection
│   └── PresentationHelper.kt         # External display preview calculations
├── audio/
│   ├── AudioDeviceMonitor.kt         # Audio device detection and monitoring
│   ├── AudioCaptureManager.kt        # Microphone capture and playback
│   ├── AudioCoordinator.kt           # Coordinates audio based on device state
│   └── AudioConfigurationHelper.kt   # Optimal audio configuration detection
├── data/
│   ├── SettingsRepository.kt         # Settings management with Room database
│   └── database/
│       ├── AppDatabase.kt            # Room database instance
│       ├── SettingsDao.kt            # Data Access Object for settings
│       ├── DisplaySettings.kt        # Entity for display-specific settings
│       ├── CameraSettings.kt         # Entity for camera-specific settings
│       └── AppSettings.kt            # Entity for general app settings
├── model/
│   ├── Size.kt                       # Custom Size class for testing
│   └── CameraFormat.kt               # Camera format with resolution and frame rate
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
   
4. **Audio Components**:
   - **AudioDeviceMonitor**: Monitors audio device connections in real-time
     - Tracks available output devices
     - Detects external vs internal audio devices
     - Provides device display names
   - **AudioCaptureManager**: Handles microphone capture and speaker playback
     - Supports device selection via preferredDevice API
     - Manual and automatic mute control
     - Real-time audio routing
   - **AudioCoordinator**: Coordinates audio based on external speaker detection
     - Manages overall audio state
     - Exposes available devices for UI selection
   - **AudioConfigurationHelper**: Determines optimal audio settings
     - Sample rate priority: 48kHz > 44.1kHz > highest available
     - Channel configuration (stereo/mono) based on device support
     - 16-bit PCM encoding
   - Features:
     - Automatic start/stop based on external audio device presence
     - Dynamic sample rate selection based on hardware capabilities
     - Low-latency audio passthrough
     - Visual indicators in UI
     - Output device selection with real-time switching

5. **Settings Persistence**:
   - **SettingsRepository**: Central repository for all settings operations
     - Initializes database on first launch
     - Provides suspend functions for saving settings
     - Returns Flow for reactive updates
   - **Room Entities**:
     - DisplaySettings: Stores flip settings per display ID
     - CameraSettings: Stores zoom ratio per camera
     - AppSettings: Stores last selected camera and audio device
   - **Automatic Save/Restore**:
     - Camera selection saved on change
     - Zoom ratio saved per camera
     - Display flip settings saved per display
     - Audio output device preference saved
     - All settings restored on app launch

6. **UI Components**:
   - **SidebarSection**: Table-like section headers in sidebar
   - **StatusRow**: Read-only status display rows
   - **ClickableRow**: Interactive rows that open dialogs
   - **MuteControlRow**: Audio mute toggle with status
   - **CameraSelectionDialog**: Modal for camera switching
   - **ZoomControlDialog**: Modal for zoom adjustment
   - **FlipControlDialog**: Modal for flip controls
   - **AudioOutputSelectionDialog**: Modal for audio output device selection

### Testing
- Unit tests are located in `app/src/test/`
- 150+ unit tests covering all utility classes and helpers
- Test coverage approaches 100% for non-UI components
- Test files:
  - `CameraUtilsTest.kt`: Camera utility functions (23 tests)
  - `CameraRotationHelperTest.kt`: Camera rotation logic (10 tests)
  - `DisplayUtilsTest.kt`: Display detection logic (10 tests)
  - `CameraStateTest.kt`: State management (7 tests)
  - `PresentationHelperTest.kt`: External display calculations (5 tests)
  - `AudioDeviceMonitorTest.kt`: Audio device detection (15 tests)
  - `AudioCaptureManagerIsolatedTest.kt`: Audio capture logic (9 tests)
  - `AudioCaptureManagerPermissionTest.kt`: Permission-specific tests (1 test)
  - `AudioCoordinatorTest.kt`: Audio coordination (12 tests)
  - `AudioConfigurationHelperTest.kt`: Audio configuration selection (15 tests)
  - `AudioDeviceSelectionTest.kt`: Audio output device selection (4 tests)
  - `SizeTest.kt`: Custom Size class tests (8 tests)
  - Additional test files for validation and logic testing
- Use JUnit 4, MockK, and Robolectric for testing
- Special test setup for MockK static mocking conflicts (separated test classes)

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
- **Dedicated Toggle Buttons**: 
  - Collapse button (ChevronRight >) in sidebar header
  - Expand button (ChevronLeft <) as floating button when hidden
  - Status bar aware positioning to avoid OS UI overlap
- **Modal Dialogs**: All controls open in centered modal dialogs
- **No Tap Interference**: Camera preview area doesn't respond to taps

### Recent Implementation Changes
1. **Unit Test Infrastructure**: Extracted logic into testable utility classes
2. **Landscape Lock**: App forced to landscape with `sensorLandscape`
3. **Rotation Fixes**: Separate handling for front/back camera rotations
4. **Flip Controls**: Added manual V/H flip for external display
5. **UI Overhaul**: Sidebar converted to overlay with modal dialogs
6. **State Management**: Immutable state pattern for camera configuration
7. **Audio Passthrough**: Added automatic audio capture/playback for external speakers
8. **Audio Configuration**: Dynamic sample rate selection (48kHz > 44.1kHz > highest)
9. **Audio UI Controls**: Added mute toggle and device information display
10. **Audio Output Selection**: Added dialog for manual audio output device selection
11. **Settings Persistence**: Implemented Room database for saving user preferences
12. **Camera Rotation Helper**: Extracted rotation logic for better testability
13. **Test Coverage**: Expanded to 150+ tests with 100% success rate
14. **Frame Rate Detection**: Added automatic detection of actual camera capture frame rate when not explicitly set
15. **Camera Format Model**: Added CameraFormat data class to handle resolution and frame rate combinations
16. **Camera-Display Settings**: Flip settings now stored per camera-display combination with DB migration
17. **Sidebar Toggle UI**: Replaced tap-to-toggle with dedicated buttons and status bar aware positioning

### Test Device
- Device IP: 192.168.0.238:5555
- ADB commands:
  ```bash
  ~/Library/Android/sdk/platform-tools/adb -s 192.168.0.238:5555 install -r app/build/outputs/apk/debug/app-debug.apk
  ~/Library/Android/sdk/platform-tools/adb -s 192.168.0.238:5555 shell am start -n app.mat2uken.android.app.clearoutcamerapreview/.MainActivity
  ```