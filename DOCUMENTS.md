# ClearOut Camera Preview - Technical Documentation

## 目次
1. [概要](#概要)
2. [アーキテクチャ](#アーキテクチャ)
3. [主要コンポーネント](#主要コンポーネント)
4. [設定の永続化](#設定の永続化)
5. [外部ディスプレイサポート](#外部ディスプレイサポート)
6. [オーディオシステム](#オーディオシステム)
7. [UI/UXデザイン](#uiuxデザイン)
8. [テスト戦略](#テスト戦略)
9. [ビルドとデプロイ](#ビルドとデプロイ)

## 概要

ClearOut Camera Previewは、Androidデバイスの内蔵カメラ映像をリアルタイムでプレビューし、外部ディスプレイへの同時出力をサポートするアプリケーションです。

### 主な機能
- カメラ映像のリアルタイムプレビュー（内蔵ディスプレイ・外部ディスプレイ同時）
- 前面/背面カメラの切り替え
- ズーム機能（カメラハードウェアの範囲内）
- 外部ディスプレイ映像の手動反転（上下・左右独立制御）
- 外部スピーカー接続時の自動オーディオパススルー
- 設定の永続化（Room database使用）
- 横向き固定表示

## アーキテクチャ

### 技術スタック
- **言語**: Kotlin 1.9.0
- **UI**: Jetpack Compose with Material 3
- **カメラ**: CameraX 1.3.4
- **オーディオ**: Android AudioRecord/AudioTrack APIs
- **データベース**: Room 2.6.1 with KSP
- **最小SDK**: 31 (Android 12)
- **ターゲットSDK**: 35 (Android 15)

### レイヤー構造

```
Presentation Layer (Compose UI)
    ↓
Domain Layer (State Management & Business Logic)
    ↓
Data Layer (Repository & Database)
    ↓
Infrastructure Layer (Camera, Audio, Display APIs)
```

## 主要コンポーネント

### 1. MainActivity
アプリケーションのエントリーポイント。横向き固定とパーミッション管理を担当。

```kotlin
requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
```

### 2. SimplifiedMultiDisplayCameraScreen
メインの画面コンポーザブル。リファクタリングにより、2075行から1289行に削減。

主な責務：
- カメラプレビューの管理
- 外部ディスプレイの検出と管理
- 設定の保存/復元
- UIイベントの処理

関連する抽出されたコンポーネント：
- **SimpleCameraPresentation** (presentation/SimpleCameraPresentation.kt) - 外部ディスプレイPresentation
- **CameraDialogs** (ui/CameraDialogs.kt) - すべてのダイアログコンポーネント
- **CameraSidebar** (ui/CameraSidebar.kt) - サイドバーUIコンポーネント

### 3. CameraState
イミュータブルなカメラ状態管理。

```kotlin
data class CameraState(
    val cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    val zoomRatio: Float = 1.0f,
    val minZoomRatio: Float = 1.0f,
    val maxZoomRatio: Float = 1.0f,
    val isExternalDisplayConnected: Boolean = false,
    val externalDisplayId: Int? = null
)
```

### 4. CameraRotationHelper
カメラの回転計算ロジックを分離したユーティリティ。

```kotlin
object CameraRotationHelper {
    fun isFrontCamera(cameraSelector: CameraSelector): Boolean
    fun getTargetRotation(deviceRotation: Int, isFrontCamera: Boolean): Int
    fun getRotationCompensation(deviceRotation: Int, isFrontCamera: Boolean): Int
}
```

## 設定の永続化

### Room Database構造

#### エンティティ

1. **DisplaySettings**
```kotlin
@Entity(tableName = "display_settings")
data class DisplaySettings(
    @PrimaryKey val displayId: String,
    val isVerticallyFlipped: Boolean = false,
    val isHorizontallyFlipped: Boolean = false,
    val displayName: String = ""
)
```

2. **CameraSettings**
```kotlin
@Entity(tableName = "camera_settings")
data class CameraSettings(
    @PrimaryKey val cameraId: String,
    val zoomRatio: Float = 1.0f,
    val minZoomRatio: Float = 1.0f,
    val maxZoomRatio: Float = 1.0f
)
```

3. **AppSettings**
```kotlin
@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val lastSelectedCamera: String = "back",
    val audioOutputDeviceId: Int? = null
)
```

### SettingsRepository
設定操作の中央管理リポジトリ。

主な機能：
- 初回起動時のデータベース初期化
- 設定の保存（suspend functions）
- 設定の読み込み（Flow対応）
- 自動保存トリガー

## 外部ディスプレイサポート

### DisplayUtils
外部ディスプレイの検出と情報取得ロジック。

```kotlin
data class DisplayInfo(
    val displayId: Int,
    val name: String,
    val width: Int,
    val height: Int,
    val rotation: Int,
    val densityDpi: Int,
    val isDefaultDisplay: Boolean
)

object DisplayUtils {
    fun findExternalDisplay(displays: Array<Display>): Display?
    fun findAllExternalDisplays(displays: Array<Display>): List<Display>
    fun getDisplayInfo(display: Display, context: Context?): DisplayInfo
    fun getAllDisplayInfo(displayManager: DisplayManager, context: Context?): List<DisplayInfo>
    fun isExternalDisplay(display: Display): Boolean
}
```

### マルチディスプレイ管理
- 複数の外部ディスプレイの検出
- ディスプレイ情報の詳細取得（名前、ID、解像度）
- リアルタイムディスプレイ切り替え
- ディスプレイ固有設定の保存

### ディスプレイ選択UI
**サイドバー → Display Status セクション**

表示される情報：
1. **External Display**: 接続状態
2. **Display Name**: ディスプレイ名
3. **Display ID**: ディスプレイID番号
4. **Resolution**: ディスプレイ解像度
5. **Select Display**: ディスプレイ切り替えUI（複数ディスプレイ時のみ表示）

### SimpleCameraPresentation
外部ディスプレイ用のPresentation実装。

機能：
- アスペクト比の自動調整
- 回転補正
- 反転変換サポート
- 16:9アスペクト比の維持
- 動的ディスプレイ切り替え対応

### 解像度選択ロジック
1. 1920x1080を優先
2. 16:9アスペクト比の解像度を選択
3. 最も近いアスペクト比にフォールバック

## オーディオシステム

### AudioCoordinator
オーディオシステム全体の調整役。

責務：
- 外部オーディオデバイスの検出
- オーディオキャプチャの自動開始/停止
- オーディオ状態の統合管理

### AudioDeviceMonitor
リアルタイムでオーディオデバイスを監視。

機能：
- 利用可能なデバイスのリスト管理
- デバイス接続/切断の検出
- デバイス名の取得

### AudioCaptureManager
マイクキャプチャとスピーカー再生を管理。

特徴：
- 低遅延オーディオパススルー
- 手動/自動ミュート制御
- preferredDevice APIによるデバイス選択
- テスト可能性のための依存性注入（AudioComponentFactory）

### Audio Interfaces（テスト可能性向上）
リファクタリングにより追加されたインターフェース：

```kotlin
interface AudioRecordWrapper {
    val state: Int
    val recordingState: Int
    fun startRecording()
    fun stop()
    fun release()
    fun read(audioData: ByteArray, offsetInBytes: Int, sizeInBytes: Int): Int
}

interface AudioTrackWrapper {
    val state: Int
    val playState: Int
    var preferredDevice: AudioDeviceInfo?
    fun play()
    fun stop()
    fun release()
    fun write(audioData: ByteArray, offsetInBytes: Int, sizeInBytes: Int): Int
}

interface AudioComponentFactory {
    fun createAudioRecord(...): AudioRecordWrapper
    fun createAudioTrack(...): AudioTrackWrapper
}
```

### AudioConfigurationHelper
最適なオーディオ設定を決定。

優先順位：
1. 48kHz
2. 44.1kHz
3. 最高利用可能サンプルレート

## UI/UXデザイン

### オーバーレイサイドバー
- 幅: 280dp
- 透明度: 95%（半透明効果）
- タップで表示/非表示切り替え

### モーダルダイアログ
すべてのコントロールはモーダルダイアログで実装：
- CameraSelectionDialog
- ZoomControlDialog
- FlipControlDialog
- AudioOutputSelectionDialog

### UI構成要素
- **SidebarSection**: セクションヘッダー
- **StatusRow**: 読み取り専用ステータス表示
- **ClickableRow**: クリック可能な設定行
- **MuteControlRow**: ミュートトグル

## リファクタリング成果

### コード品質の向上
1. **SimplifiedMultiDisplayCameraScreen**: 2075行 → 1289行（38%削減）
2. **コンポーネントの分離**:
   - SimpleCameraPresentation（外部ディスプレイロジック）
   - CameraDialogs（ダイアログコンポーネント）
   - CameraSidebar（サイドバーコンポーネント）
3. **不要なファイルの削除**: DisplayManagerHelper（未使用）
4. **テスト可能性の向上**: AudioインターフェースによるDI対応

## テスト戦略

### ユニットテスト
- 190のテスト
- 100%の成功率
- MockK、JUnit4、Robolectricを使用

### テストカテゴリ

1. **ユーティリティテスト** (67テスト)
   - CameraUtilsTest (23)
   - CameraRotationHelperTest (10)
   - DisplayUtilsTest (25)
   - FrameRateUtilsTest (9)

2. **状態管理テスト** (36テスト)
   - CameraStateTest (7)
   - SizeTest (8)
   - CameraFormatTest (13)
   - データベーステスト (8)

3. **オーディオテスト** (56テスト)
   - AudioDeviceMonitorTest (15)
   - AudioCaptureManagerIsolatedTest (9)
   - AudioCoordinatorTest (12)
   - AudioConfigurationHelperTest (15)
   - AudioDeviceSelectionTest (4)
   - AudioCaptureManagerPermissionTest (1)

4. **その他のテスト** (31テスト)
   - ValidationTest (10)
   - SimpleUnitTest (10)
   - CameraLogicTest (10)
   - ExampleUnitTest (1)

### MockK静的モッキングの課題
複数のテストファイルに分離して解決：
- AudioCaptureManagerIsolatedTest
- AudioCaptureManagerPermissionTest

## ビルドとデプロイ

### ビルドコマンド
```bash
# デバッグビルド
./gradlew assembleDebug

# リリースビルド
./gradlew assembleRelease

# テスト実行
./gradlew test
```

### デバイスへのインストール
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n app.mat2uken.android.app.clearoutcamerapreview/.MainActivity
```

### ProGuard設定
現在はリリースビルドでProGuardは無効化されています。

## 今後の拡張ポイント

1. **録画機能の追加**
   - MediaRecorder APIの統合
   - 外部ストレージへの保存

2. **ネットワークストリーミング**
   - RTMP/WebRTCサポート
   - リモートプレビュー機能

3. **画像処理フィルター**
   - リアルタイムエフェクト
   - カラー調整

4. **マルチカメラサポート**
   - 同時複数カメラプレビュー
   - ピクチャーインピクチャー

## トラブルシューティング

### よくある問題

1. **外部ディスプレイが検出されない**
   - DisplayManagerの権限確認
   - HDMIアダプターの互換性確認

2. **オーディオが再生されない**
   - RECORD_AUDIO権限の確認
   - オーディオデバイスの接続状態確認

3. **カメラプレビューが回転している**
   - デバイスの向きセンサーの確認
   - CameraRotationHelperのロジック確認

## ライセンスと著作権

このプロジェクトの詳細なライセンス情報については、プロジェクトルートのLICENSEファイルを参照してください。