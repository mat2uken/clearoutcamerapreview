# Clear Out Camera Preview - Technical Documentation

## 概要 / Overview
このアプリは、Android端末の内蔵カメラを使用してリアルタイムで映像をプレビューする機能を提供します。ユーザーはカメラの切り替えやズーム調整を行いながら、フルスクリーンでカメラ映像を確認できます。さらに、外部ディスプレイが接続されている場合は、端末画面と外部ディスプレイの両方に同時にカメラ映像を表示することができます。

This application provides real-time camera preview functionality using Android device's built-in camera. Users can view camera feed in fullscreen while switching cameras and adjusting zoom. When an external display is connected, the camera preview is shown simultaneously on both device screen and external display.

## 動作環境 / System Requirements
- **最小SDK / Min SDK**: Android 12 (API Level 31)
- **ターゲットSDK / Target SDK**: Android 15 (API Level 35)
- **開発言語 / Language**: Kotlin 1.9.0
- **UIフレームワーク / UI Framework**: Jetpack Compose (Material 3)
- **Javaバージョン / Java Version**: Java 11
- **ビルドシステム / Build System**: Gradle 8.7 (Kotlin DSL)
- **画面向き / Orientation**: 横向き固定 (Landscape only)

## 主要機能 / Key Features

### 1. カメラプレビュー / Camera Preview
- アプリ起動時にカメラを自動的に初期化
- カメラからキャプチャした映像をリアルタイムでフルスクリーン表示
- CameraXライブラリを使用した高性能なカメラ制御
- 横向き固定表示（両方向のlandscapeに対応）

### 2. ズーム機能 / Zoom Control
- 各デバイスのハードウェアがサポートする最小・最大ズーム率を自動取得
- モーダルダイアログによる直感的なズーム調整
- 現在のズーム倍率をリアルタイム表示（例: 1.0x, 2.5x）
- ズーム範囲はデバイスのカメラ性能に依存

### 3. カメラ切り替え / Camera Switching
- モーダルダイアログによるカメラ選択機能
- 対応カメラ:
  - 背面カメラ（Back Camera）
  - 前面カメラ（Front Camera）
- カメラ切り替え時は自動的にプレビューを再初期化
- 前面/背面カメラで異なる回転補正を適用

### 4. 外部ディスプレイ対応 / External Display Support
- 外部ディスプレイの自動検出
- 接続時に端末画面と外部ディスプレイの両方にカメラ映像を同時表示
- 外部ディスプレイでもフルスクリーン表示
- ディスプレイの接続/切断を自動検知して表示を更新
- ステータス表示で外部ディスプレイの接続状態を確認可能
- 映像の手動反転機能（上下反転・左右反転を独立制御）

### 5. オーバーレイサイドバーUI / Overlay Sidebar UI
- カメラプレビューの上に重ねて表示される半透明サイドバー
- タップで表示/非表示の切り替え
- すべての設定をモーダルダイアログで操作
- メニューアイコンでサイドバーの存在を示唆

### 6. オーディオパススルー機能 / Audio Passthrough
- 外部スピーカー接続時に自動的にマイク録音を開始
- マイクからの音声を外部スピーカーへリアルタイム出力
- オーディオ設定:
  - サンプルレート優先度: 48kHz > 44.1kHz > 最高利用可能
  - 16ビット深度 (PCM)
  - ステレオ対応（デバイスがサポートする場合、それ以外はモノラル）
  - 内部スピーカーのみの場合は自動ミュート
- UI表示機能:
  - マイクデバイス名とオーディオフォーマット表示
  - 出力デバイス情報表示
  - ミュート切り替えコントロール（手動/自動）
  - 出力デバイス選択ダイアログ（利用可能なデバイスから選択）

## 技術仕様 / Technical Specifications

### 依存ライブラリ / Dependencies
- **CameraX**: 1.3.4
  - camera-core
  - camera-camera2
  - camera-lifecycle
  - camera-view
- **Accompanist Permissions**: 0.32.0（権限管理用）
- **Jetpack Compose BOM**: 2024.04.01
- **AndroidX Core KTX**: 1.16.0
- **AndroidX Lifecycle Runtime KTX**: 2.9.0
- **AndroidX Activity Compose**: 1.10.1

### テスト用依存ライブラリ / Test Dependencies
- **JUnit**: 4.13.2
- **MockK**: 1.13.8
- **Kotlinx Coroutines Test**: 1.7.3
- **AndroidX Arch Core Testing**: 2.2.0
- **Robolectric**: 4.11.1

### 必要な権限 / Required Permissions
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-feature android:name="android.hardware.camera" android:required="true" />
<uses-feature android:name="android.hardware.camera.autofocus" />
```

### アーキテクチャ / Architecture

#### 主要コンポーネント / Key Components
- **MainActivity**: アプリのエントリーポイント、権限管理、横向き設定
- **SimplifiedMultiDisplayCameraScreen**: メインカメラ画面（外部ディスプレイ対応）
- **SimpleCameraPresentation**: 外部ディスプレイ用のPresentation実装
- **CameraState**: イミュータブルなカメラ状態管理
- **Utility Classes**:
  - CameraUtils: カメラ関連の計算処理
  - DisplayUtils: 外部ディスプレイ検出
  - PresentationHelper: 外部ディスプレイ表示計算
- **UI Components**:
  - SidebarSection: サイドバーのセクションヘッダー
  - StatusRow: 読み取り専用の状態表示
  - ClickableRow: クリック可能な設定行
  - Modal Dialogs: カメラ選択、ズーム調整、反転制御

#### ファイル構成 / File Structure
```
app/src/main/java/app/mat2uken/android/app/clearoutcamerapreview/
├── MainActivity.kt                          # メインアクティビティ
├── SimplifiedMultiDisplayCameraScreen.kt    # メインカメラ画面
├── camera/
│   └── CameraState.kt                      # カメラ状態管理
├── utils/
│   ├── CameraUtils.kt                      # カメラユーティリティ
│   ├── DisplayUtils.kt                     # ディスプレイ検出
│   └── PresentationHelper.kt               # 外部表示計算
├── audio/
│   ├── AudioDeviceMonitor.kt               # オーディオデバイス監視
│   ├── AudioCaptureManager.kt              # 録音・再生管理
│   ├── AudioCoordinator.kt                 # オーディオ統合制御
│   └── AudioConfigurationHelper.kt         # 最適オーディオ設定検出
├── model/
│   └── Size.kt                             # テスト用Sizeクラス
└── ui/theme/                               # テーマ関連
    ├── Color.kt
    ├── Theme.kt
    └── Type.kt
```

## UI構成 / UI Design

### 画面レイアウト / Screen Layout
1. **背景**: カメラプレビュー（フルスクリーン）
2. **オーバーレイサイドバー**（右側、280dp幅）:
   - 半透明背景（95%不透明度）
   - スクロール可能なコンテンツ
   - セクション構成:
     - Display Status（接続状態）
     - Audio Status（外部オーディオ接続、録音状態）
     - Microphone（デバイス名、フォーマット）
     - Audio Output（デバイス選択、ミュート制御）
     - Camera Information（解像度、アスペクト比）
     - Camera Controls（カメラ選択、ズーム）
     - External Display（反転制御）

### モーダルダイアログ / Modal Dialogs
- **カメラ選択**: ラジオボタンで前面/背面を選択
- **ズーム調整**: スライダーでズーム倍率を調整
- **反転制御**: スイッチで上下/左右反転を切り替え
- **オーディオ出力選択**: 利用可能なデバイスから選択（ラジオボタン）

### 権限処理 / Permission Handling
- 初回起動時にカメラとマイク権限をリクエスト
- 権限が拒否された場合は説明メッセージを表示
- 権限が許可されるまでカメラプレビューは表示されない
- オーディオ機能はマイク権限が必要

## 実装の詳細 / Implementation Details

### 解像度選択アルゴリズム / Resolution Selection
```kotlin
fun selectOptimalResolution(availableSizes: List<Size>): Size? {
    // 1. 1920x1080を優先
    val fullHd = availableSizes.find { 
        it.width == 1920 && it.height == 1080 
    }
    if (fullHd != null) return fullHd
    
    // 2. 16:9に最も近いアスペクト比を選択
    return availableSizes
        .filter { it.width > 0 && it.height > 0 }
        .minByOrNull { 
            abs(it.width.toFloat() / it.height - 16f / 9f) 
        }
}
```

### 回転制御 / Rotation Handling
前面カメラと背面カメラで異なる回転ロジック:
- **前面カメラ**: 270度の補正を適用
- **背面カメラ**: 外部ディスプレイでは固定180度回転

### 反転機能 / Flip Transformation
```kotlin
private fun applyFlipTransformation() {
    val scaleX = if (isHorizontallyFlipped) -1f else 1f
    val scaleY = if (isVerticallyFlipped) -1f else 1f
    
    previewView.scaleX = scaleX
    previewView.scaleY = scaleY
}
```

### オーディオ実装 / Audio Implementation

#### デバイス検出 / Device Detection
外部オーディオデバイスの接続を自動検出:
```kotlin
private fun isExternalAudioDevice(device: AudioDeviceInfo): Boolean {
    return when (device.type) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> false
        
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_HDMI,
        AudioDeviceInfo.TYPE_USB_DEVICE -> true
        
        else -> true // Unknown devices treated as external
    }
}
```

#### オーディオキャプチャ設定 / Audio Capture Configuration
- サンプルレート優先順位:
  1. 48kHz（推奨）
  2. 44.1kHz（標準）
  3. デバイスがサポートする最高サンプルレート
- オーディオフォーマット: 16-bit PCM
- チャンネル: ステレオ（サポートされている場合）、それ以外はモノラル
- バッファサイズ: 自動計算（最小100ms）
- 出力デバイス選択: Android API 23以上でpreferredDevice APIを使用

#### リアルタイム処理 / Real-time Processing
```kotlin
// メインループ
while (isActive && state.isCapturing) {
    val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
    if (bytesRead > 0) {
        audioTrack?.write(buffer, 0, bytesRead)
    }
    yield()
}
```

## テスト / Testing

### ユニットテスト構成 / Unit Test Structure
- **CameraUtilsTest**: 14テスト（解像度選択、ズーム計算、アスペクト比）
- **DisplayUtilsTest**: 15テスト（ディスプレイ検出ロジック）
- **CameraStateTest**: 18テスト（状態管理、イミュータブル更新）
- **PresentationHelperTest**: 28テスト（外部ディスプレイ計算）
- **AudioDeviceMonitorTest**: 10テスト（オーディオデバイス検出）
- **AudioCaptureManagerTest**: 6テスト（録音・再生管理）
- **AudioCoordinatorTest**: 4テスト（オーディオ統合制御）
- **AudioConfigurationHelperTest**: 3テスト（オーディオ設定選択）
- **AudioDeviceSelectionTest**: 4テスト（出力デバイス選択）

合計100以上のユニットテストでビジネスロジックをカバー

### テスト実行 / Running Tests
```bash
# 全てのユニットテストを実行
./gradlew test

# 特定のテストクラスを実行
./gradlew test --tests "*CameraUtilsTest"

# カバレッジレポート生成
./gradlew testDebugUnitTest jacocoTestReport
```

## ビルド・実行方法 / Build & Run

### ビルド / Build
```bash
# プロジェクトのクリーン
./gradlew clean

# デバッグビルド
./gradlew assembleDebug

# リリースビルド
./gradlew assembleRelease
```

### インストール・実行 / Install & Run
```bash
# デバイスへのインストール
./gradlew installDebug

# アプリの起動
adb shell am start -n app.mat2uken.android.app.clearoutcamerapreview/.MainActivity
```

## トラブルシューティング / Troubleshooting

### よくある問題 / Common Issues

1. **外部ディスプレイが検出されない**
   - HDMI接続を確認
   - DisplayのPresentationフラグをチェック
   - logcatでDisplayManagerイベントを監視

2. **カメラの回転問題**
   - 前面カメラ: 270度補正を使用
   - 背面カメラ: 外部ディスプレイでは180度固定
   - デバイス回転: sensorLandscapeで処理

3. **プレビューのアスペクト比**
   - 16:9ディスプレイを想定
   - 異なる比率にはレターボックスを適用
   - 1920x1080を優先的に選択

4. **サイドバーが表示されない**
   - カメラプレビューをタップ
   - 右上のメニューアイコンを確認
   - タッチイベントの伝播を確認

5. **オーディオが機能しない**
   - マイク権限を確認
   - 外部スピーカーの接続を確認
   - AudioManagerイベントをlogcatで監視
   - オーディオデバイスの種類を確認

### デバッグ用ログタグ / Debug Log Tags
- `SimplifiedMultiDisplay`: メインカメラ操作
- `DisplayUtils`: 外部ディスプレイ検出
- `CameraUtils`: 解像度選択
- `Presentation`: 外部ディスプレイレンダリング
- `AudioDeviceMonitor`: オーディオデバイス検出
- `AudioCaptureManager`: 録音・再生管理
- `AudioCoordinator`: オーディオ統合制御
- `AudioConfigurationHelper`: オーディオ設定選択

## 今後の拡張可能性 / Future Enhancements
- 写真撮影機能
- 動画録画機能
- ジェスチャーによるズーム操作
- 設定の永続化
- カスタムアスペクト比サポート
- 複数の外部ディスプレイ対応
- オーディオエフェクト（エコーキャンセル、ノイズ抑制）
- オーディオ録音機能
- Bluetooth LEオーディオ対応