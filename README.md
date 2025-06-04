# ClearOut Camera Preview

内蔵カメラの映像をフルスクリーンでプレビュー表示するAndroidアプリケーションです。外部ディスプレイへの同時出力、オーディオパススルー機能、詳細な設定管理機能を備えています。

## 📱 プロジェクトの概要

このアプリは、Androidデバイスのカメラを使用して高品質な映像プレビューを提供します。主な特徴：

- **1920x1080 解像度優先**: 利用可能な場合は常にフルHD解像度を選択
- **デュアルディスプレイ対応**: 内蔵画面と外部ディスプレイへの同時出力
- **オーディオパススルー**: 外部スピーカー接続時にマイクから音声を自動中継
- **横向き固定表示**: 安定したランドスケープモードでの動作
- **設定の永続化**: カメラやディスプレイの設定を自動保存・復元

## 🎯 アプリの利用方法・操作方法

### 基本操作

1. **アプリの起動**
   - アプリを起動すると自動的にカメラプレビューが開始されます
   - 画面は横向き（ランドスケープ）に固定されます

2. **サイドバーの表示/非表示**
   - 画面右端の「<」ボタンをタップしてサイドバーを表示
   - サイドバー右上の「>」ボタンでサイドバーを非表示

3. **カメラの切り替え**
   - サイドバーの「Camera」行をタップ
   - ダイアログから前面/背面カメラを選択

4. **ズーム調整**
   - サイドバーの「Zoom」行をタップ
   - スライダーでズーム倍率を調整（デバイスの最小〜最大倍率）

5. **外部ディスプレイ設定**（外部ディスプレイ接続時のみ）
   - サイドバーの「Flip Controls」をタップ
   - 垂直反転・水平反転を個別に設定可能

6. **オーディオ設定**（外部スピーカー接続時のみ）
   - 出力デバイスの選択: 「Device」行をタップして選択
   - ミュート切り替え: 「Output」行をタップ

### 表示情報

サイドバーには以下の情報が表示されます：

- **Display Status**: 外部ディスプレイの接続状態
- **Audio Status**: 外部オーディオデバイスの接続状態
- **Camera Information**: 解像度、アスペクト比、フレームレート
- **Microphone**: 使用中のマイクデバイスとオーディオフォーマット
- **Audio Output**: 出力先デバイスとミュート状態

## 🛠 使用している主な技術

### 言語・フレームワーク
- **Kotlin** 1.9.0
- **Jetpack Compose** (Material 3)
- **Android SDK** (Min: 31, Target: 35)

### 主要ライブラリ
- **CameraX** 1.3.4 - カメラ機能の実装
- **Room** 2.6.1 - 設定データの永続化
- **Accompanist Permissions** 0.32.0 - パーミッション管理
- **KSP** 1.9.0-1.0.13 - アノテーション処理

### ビルドシステム
- **Gradle** 8.7 (Kotlin DSL)
- **Java** 11

## 📋 必要な環境変数・コマンド一覧

### ビルドコマンド

```bash
# プロジェクトのクリーン
./gradlew clean

# デバッグAPKのビルド
./gradlew assembleDebug

# リリースAPKのビルド
./gradlew assembleRelease

# デバイスへのインストール
./gradlew installDebug

# 全テストの実行
./gradlew test

# Lintチェック
./gradlew lint
```

### ADBコマンド（デバッグ用）

```bash
# APKのインストール
~/Library/Android/sdk/platform-tools/adb -s 192.168.0.137:5555 install -r app/build/outputs/apk/debug/app-debug.apk

# アプリの起動
~/Library/Android/sdk/platform-tools/adb -s 192.168.0.137:5555 shell am start -n app.mat2uken.android.app.clearoutcamerapreview/.MainActivity

# ログの確認
~/Library/Android/sdk/platform-tools/adb logcat | grep SimplifiedMultiDisplay
```

## 📁 ディレクトリ構成

```
app/src/main/java/.../clearoutcamerapreview/
├── MainActivity.kt                    # エントリーポイント
├── SimplifiedMultiDisplayCameraScreen.kt # メイン画面の実装
├── camera/
│   └── CameraState.kt                # カメラ状態管理
├── utils/
│   ├── CameraUtils.kt                # カメラユーティリティ
│   ├── CameraRotationHelper.kt       # 回転計算ヘルパー
│   ├── DisplayUtils.kt               # ディスプレイ検出
│   └── FrameRateUtils.kt             # フレームレート検出
├── audio/
│   ├── AudioDeviceMonitor.kt         # オーディオデバイス監視
│   ├── AudioCaptureManager.kt        # 録音・再生管理
│   ├── AudioCoordinator.kt           # オーディオ統合管理
│   └── AudioConfigurationHelper.kt   # オーディオ設定最適化
├── data/
│   ├── SettingsRepository.kt         # 設定管理リポジトリ
│   └── database/
│       ├── AppDatabase.kt            # Roomデータベース
│       ├── SettingsDao.kt            # データアクセスオブジェクト
│       ├── DisplaySettings.kt        # ディスプレイ設定エンティティ
│       ├── CameraSettings.kt         # カメラ設定エンティティ
│       └── AppSettings.kt            # アプリ設定エンティティ
├── model/
│   ├── Size.kt                       # カスタムSizeクラス
│   └── CameraFormat.kt               # カメラフォーマット情報
└── ui/theme/                         # Composeテーマ設定
```

## 🚀 開発環境の構築方法

### 必要な環境

1. **Android Studio** (最新版推奨)
2. **JDK 11** 以上
3. **Android SDK** (API Level 31以上)
4. **実機またはエミュレータ** (Android 12以上)

### セットアップ手順

1. **リポジトリのクローン**
   ```bash
   git clone [repository-url]
   cd clearoutcamerapreview
   ```

2. **Android Studioで開く**
   - Android Studioを起動
   - 「Open」を選択してプロジェクトディレクトリを指定

3. **Gradleの同期**
   - Android Studioが自動的にGradle同期を開始
   - 必要な依存関係がダウンロードされます

4. **実機の準備**（実機を使用する場合）
   - 開発者オプションを有効化
   - USBデバッグを有効化
   - PCに接続

5. **ビルドと実行**
   ```bash
   ./gradlew installDebug
   ```

### パーミッション設定

アプリは以下のパーミッションを必要とします（AndroidManifest.xmlで定義済み）：

- `CAMERA` - カメラアクセス
- `RECORD_AUDIO` - マイクアクセス
- `MODIFY_AUDIO_SETTINGS` - オーディオ設定の変更

## 🔧 トラブルシューティング

### よくある問題と解決方法

#### 1. カメラプレビューが表示されない
- **原因**: カメラパーミッションが許可されていない
- **解決**: 設定 > アプリ > ClearOut Camera Preview > 権限 からカメラを許可

#### 2. 音声が出力されない
- **原因**: 録音パーミッションが許可されていない、または外部スピーカーが接続されていない
- **解決**: 
  - 録音パーミッションを許可
  - 外部スピーカー/ヘッドホンを接続

#### 3. 解像度が1920x1080にならない
- **原因**: デバイスが1920x1080をサポートしていない
- **解決**: サイドバーのCamera Informationで利用可能な解像度を確認

#### 4. 外部ディスプレイに表示されない
- **原因**: ディスプレイが正しく認識されていない
- **解決**: 
  - ケーブルの接続を確認
  - デバイスを再起動
  - 別のケーブル/アダプタを試す

#### 5. アプリがクラッシュする
- **原因**: メモリ不足または互換性の問題
- **解決**: 
  - 他のアプリを終了してメモリを確保
  - デバイスを再起動
  - アプリのキャッシュをクリア

### デバッグ情報の取得

問題が解決しない場合は、以下のコマンドでログを取得：

```bash
# 詳細ログの取得
adb logcat -v time | grep -E "SimplifiedMultiDisplay|CameraUtils|AudioCaptureManager" > debug.log
```

## 📝 ライセンス

このプロジェクトのライセンスについては、LICENSEファイルを参照してください。

## 🤝 貢献

バグ報告や機能提案は、GitHubのIssuesページから行ってください。

プルリクエストを送る前に：
1. コードスタイルガイドラインに従ってください
2. すべてのテストが通ることを確認してください
3. 新機能の場合はテストを追加してください