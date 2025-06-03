# カメラプレビューアプリ仕様書

## 概要
このアプリは、Android端末の内蔵カメラを使用してリアルタイムで映像をプレビューする機能を提供します。ユーザーはカメラの切り替えやズーム調整を行いながら、フルスクリーンでカメラ映像を確認できます。さらに、外部ディスプレイが接続されている場合は、端末画面と外部ディスプレイの両方に同時にカメラ映像を表示することができます。

## 動作環境
- **最小SDK**: Android 12 (API レベル 31)
- **ターゲットSDK**: Android 15 (API レベル 35)
- **開発言語**: Kotlin 1.9.0
- **UIフレームワーク**: Jetpack Compose (Material 3)
- **Javaバージョン**: Java 11
- **ビルドシステム**: Gradle 8.7 (Kotlin DSL)

## 主要機能

### 1. カメラプレビュー
- アプリ起動時にカメラを自動的に初期化
- カメラからキャプチャした映像をリアルタイムでフルスクリーン表示
- CameraXライブラリを使用した高性能なカメラ制御

### 2. ズーム機能
- 各デバイスのハードウェアがサポートする最小・最大ズーム率を自動取得
- スライダーUIによる直感的なズーム調整
- 現在のズーム倍率をリアルタイム表示（例: 1.0x, 2.5x）
- ズーム範囲はデバイスのカメラ性能に依存

### 3. カメラ切り替え
- ドロップダウンメニューによるカメラ選択機能
- 対応カメラ:
  - 背面カメラ（Back Camera）
  - 前面カメラ（Front Camera）
- カメラ切り替え時は自動的にプレビューを再初期化

### 4. 外部ディスプレイ対応
- 外部ディスプレイの自動検出
- 接続時に端末画面と外部ディスプレイの両方にカメラ映像を同時表示
- 外部ディスプレイでもフルスクリーン表示
- ディスプレイの接続/切断を自動検知して表示を更新
- ステータス表示で外部ディスプレイの接続状態を確認可能

## 技術仕様

### 依存ライブラリ
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

### テスト用依存ライブラリ
- **JUnit**: 4.13.2
- **MockK**: 1.13.8
- **Kotlinx Coroutines Test**: 1.7.3
- **AndroidX Arch Core Testing**: 2.2.0
- **Robolectric**: 4.11.1

### 必要な権限
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="true" />
<uses-feature android:name="android.hardware.camera.autofocus" />
```

### アーキテクチャ

#### 主要コンポーネント
- **MainActivity**: アプリのエントリーポイント、権限管理を担当
- **SimplifiedMultiDisplayCameraScreen**: 外部ディスプレイ対応のメインカメラ画面（現在使用中）
- **SimpleCameraPresentation**: 外部ディスプレイ用のPresentation実装（回転制御付き）
- **CameraScreen**: 基本的なカメラ画面の実装
- **CameraPreview**: カメラプレビューの表示を担当するComposable
- **CameraSelectorDropdown**: カメラ選択UIコンポーネント
- **ZoomSlider**: ズーム調整UIコンポーネント
- **CameraPermissionScreen**: カメラ権限の要求と状態管理
- **ExternalDisplayManager**: 外部ディスプレイの検出と管理（参照実装）

#### ファイル構成
```
app/src/main/java/app/mat2uken/android/app/clearoutcamerapreview/
├── MainActivity.kt                          # メインアクティビティ
├── SimplifiedMultiDisplayCameraScreen.kt    # 外部ディスプレイ対応カメラ画面（現在使用中）
├── CameraScreen.kt                         # 基本的なカメラ画面の実装
├── MultiDisplayCameraScreen.kt             # 外部ディスプレイ対応（旧バージョン）
├── RobustMultiDisplayCameraScreen.kt       # エラー処理強化版（旧バージョン）
├── CameraScreenWithExternalDisplay.kt      # 外部ディスプレイ対応（初期バージョン）
├── ExternalDisplayManager.kt               # 外部ディスプレイ管理
└── ui/theme/                               # テーマ関連
    ├── Color.kt
    ├── Theme.kt
    └── Type.kt
```

## UI構成

### 画面レイアウト
1. **背景**: カメラプレビュー（フルスクリーン）
2. **オーバーレイUI**（画面下部）:
   - 外部ディスプレイ接続ステータス表示
   - カメラ選択ドロップダウン
   - ズームスライダー（半透明カード内）

### 外部ディスプレイ表示
- 外部ディスプレイ接続時は自動的にフルスクリーン表示
- 端末画面と同じカメラ映像を同時表示
- ズームやカメラ切り替えは端末側で操作し、外部ディスプレイに反映

### 権限処理
- 初回起動時にカメラ権限をリクエスト
- 権限が拒否された場合は説明メッセージを表示
- 権限が許可されるまでカメラプレビューは表示されない

## 実装の特徴
- Jetpack Composeによる宣言的UI
- CameraXによる最新のカメラAPI使用
- リアクティブな状態管理
- エッジツーエッジ表示対応
- Material 3デザインシステム採用

## ビルド・実行方法

### ビルド
```bash
# プロジェクトのクリーン
./gradlew clean

# デバッグビルド
./gradlew assembleDebug

# リリースビルド
./gradlew assembleRelease

# すべてをビルド
./gradlew build
```

### テスト実行
```bash
# 全てのユニットテストを実行
./gradlew test

# 特定のテストクラスを実行
./gradlew test --tests "app.mat2uken.android.app.clearoutcamerapreview.SimpleUnitTest"

# テストレポートの確認
# app/build/reports/tests/testDebugUnitTest/index.html
```

### インストール・実行
```bash
# デバイスへのインストール
./gradlew installDebug

# アプリの起動
adb shell am start -n app.mat2uken.android.app.clearoutcamerapreview/.MainActivity

# ADBパスの例（macOS）
~/Library/Android/sdk/platform-tools/adb devices
```

## テスト

### ユニットテスト
プロジェクトには31個のユニットテストが含まれており、以下の領域をカバーしています：

1. **SimpleUnitTest.kt** (10テスト)
   - 基本的なロジックのテスト
   - カメラセレクターの検証
   - ズーム比率のフォーマット
   - 権限定数の確認

2. **CameraLogicTest.kt** (10テスト)
   - カメラ関連のビジネスロジック
   - 状態管理のテスト
   - エラーハンドリング
   - カメラ切り替えロジック

3. **ValidationTest.kt** (11テスト)
   - 境界値のテスト
   - 例外シナリオ
   - スレッドセーフティ
   - メモリリーク防止パターン

## 実装済み機能の詳細

### カメラプレビュー
- CameraXを使用した効率的なカメラアクセス
- リアルタイムプレビューの表示
- カメラのライフサイクル管理
- エラーハンドリング（カメラが使用中の場合など）

### ズーム機能
- ハードウェアから最小・最大ズーム率を自動取得
- スライダーによる直感的な操作
- リアルタイムのズーム値表示（例: "Zoom: 2.5x"）
- ズーム値の範囲制限とバリデーション

### カメラ切り替え
- 前面・背面カメラの切り替え
- ドロップダウンメニューによる選択
- カメラ切り替え時の自動再初期化
- 利用可能なカメラの検出

### 権限管理
- Accompanist Permissionsを使用した権限リクエスト
- 権限が拒否された場合の適切なメッセージ表示
- 権限状態の監視と自動更新

### 外部ディスプレイ機能
- DisplayManagerを使用した外部ディスプレイの検出
- Presentationクラスによる外部ディスプレイへの表示
- 自動的な接続/切断の検知（ホットプラグ対応）
- 端末と外部ディスプレイへの同時プレビュー表示
- 外部ディスプレイでのフルスクリーン表示
- 接続状態のリアルタイム表示（接続時は"LIVE"バッジ表示）

#### 解像度選択機能
- カメラハードウェアから利用可能な解像度を取得
- 1920x1080（Full HD）を優先的に選択
- Full HDが利用できない場合は16:9に最も近いアスペクト比を選択
- 選択された解像度と実際のプレビュー解像度をUIに表示

#### 回転制御とアスペクト比維持
- 外部ディスプレイの向き（縦/横）を自動検出
- カメラプレビューの回転処理:
  - CameraXレベル: `setTargetRotation(Surface.ROTATION_90)`
  - PreviewViewレベル: 180度回転
  - 合計270度の回転補正で正しい向きを実現
- 16:9のアスペクト比を常に維持
- 必要に応じてレターボックス（黒帯）を追加
- 映像のクロップや歪みを防止

## 実装の進化と課題解決

### 開発の反復
1. **初期実装** (`CameraScreenWithExternalDisplay.kt`)
   - 基本的な外部ディスプレイサポート
   - 切断時のクラッシュ問題あり

2. **堅牢版** (`RobustMultiDisplayCameraScreen.kt`)
   - エラーハンドリングの追加
   - プレビュー表示の問題が発生

3. **簡略版** (`SimplifiedMultiDisplayCameraScreen.kt`)
   - PreviewViewの直接管理
   - 適切なライフサイクル処理
   - 回転とアスペクト比の問題を解決

### 解決した主な課題
- カメラプレビューがどちらのディスプレイにも表示されない問題
- 外部ディスプレイでのクロップ/ズーム表示の問題
- 270度回転して表示される問題
- アスペクト比の歪み（縦長/横長に変形）
- ディスプレイ切断時のクラッシュ
- ホットプラグ対応（実行時の接続/切断）

## 今後の拡張可能性
- 写真撮影機能
- 動画録画機能
- フラッシュ制御
- 各種カメラ設定（露出、ホワイトバランス等）
- 複数レンズ対応（広角、望遠等）
- フォーカス制御
- 画像処理フィルター
- QRコード/バーコード読み取り
- 複数の外部ディスプレイ対応
- ディスプレイごとの異なるズーム設定