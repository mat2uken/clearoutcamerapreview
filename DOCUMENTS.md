# カメラプレビューアプリ仕様書

## 概要
このアプリは、Android端末の内蔵カメラを使用してリアルタイムで映像をプレビューする機能を提供します。ユーザーはカメラの切り替えやズーム調整を行いながら、フルスクリーンでカメラ映像を確認できます。

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
- **CameraScreen**: メイン画面のComposable、カメラ制御のロジックを含む
- **CameraPreview**: カメラプレビューの表示を担当するComposable
- **CameraSelectorDropdown**: カメラ選択UIコンポーネント
- **ZoomSlider**: ズーム調整UIコンポーネント
- **CameraPermissionScreen**: カメラ権限の要求と状態管理

#### ファイル構成
```
app/src/main/java/app/mat2uken/android/app/clearoutcamerapreview/
├── MainActivity.kt              # メインアクティビティ
├── CameraScreen.kt             # カメラ画面の実装
└── ui/theme/                   # テーマ関連
    ├── Color.kt
    ├── Theme.kt
    └── Type.kt
```

## UI構成

### 画面レイアウト
1. **背景**: カメラプレビュー（フルスクリーン）
2. **オーバーレイUI**（画面下部）:
   - カメラ選択ドロップダウン
   - ズームスライダー（半透明カード内）

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

## 今後の拡張可能性
- 写真撮影機能
- 動画録画機能
- フラッシュ制御
- 各種カメラ設定（露出、ホワイトバランス等）
- 複数レンズ対応（広角、望遠等）
- フォーカス制御
- 画像処理フィルター
- QRコード/バーコード読み取り