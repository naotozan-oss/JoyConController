# Joy-Con Controller for Android

右Joy-ConでAndroid端末を完全操作できるアプリです。  
AccessibilityService + Bluetooth HID を利用し、**タップ・スワイプ・ナビゲーション**をカーソル操作に変換します。

---

## 動作要件

| 項目 | 要件 |
|------|------|
| Android | 10 (API 29) 以上 |
| 言語 | Kotlin |
| IDE | Android Studio Hedgehog 以降 |
| 接続 | Bluetooth (Joy-Con は標準HIDとして認識) |

---

## セットアップ手順

### 1. ビルド

```bash
# Android Studio でプロジェクトを開く
File → Open → JoyConController フォルダを選択

# または CLI
./gradlew assembleDebug
```

### 2. インストール

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 3. Joy-Con をペアリング

1. Android の **設定 → Bluetooth** を開く
2. Joy-Con の **シンクボタン**（細いボタン）を長押し
3. ランプが点滅したら Android 側で「Joy-Con (R)」を選択してペアリング

### 4. アクセシビリティサービスを有効化

1. **設定 → ユーザー補助（アクセシビリティ）** を開く
2. **インストール済みのアプリ** → **Joy-Con Controller** を選択
3. サービスを **ON** にする
4. 「許可」をタップ

> ⚠️ このステップはアプリが動作するために必須です。  
> AccessibilityService はタップ・スワイプをシステムに注入するために必要です。

### 5. アプリを起動して確認

- メイン画面で「✅ Accessibility Service: Active」と表示されればOK
- Joy-Con のボタンを押してテスト

---

## ボタンマッピング（右Joy-Con）

| ボタン | 動作 |
|--------|------|
| 右スティック | カーソル移動 |
| **A** | タップ |
| **B** | 戻る |
| **X** | 長押し |
| **Y** | 未割り当て（v2で設定可能） |
| **ZR** | ダブルタップ |
| **R** | 速度ブースト（押しながら） |
| **HOME** | ホーム画面 |
| **+（Plus）** | 最近のアプリ |
| **−（Minus）** | 通知パネル |
| **RSクリック** | タップ（サブ） |

### スクロール方式

設定画面で以下から選択：

| モード | 操作 |
|--------|------|
| A案（デフォルト） | 左スティックでスクロール |
| B案 | R ボタン押しながら右スティック |

---

## プロジェクト構成

```
JoyConController/
├── app/src/main/
│   ├── java/com/joyconcontroller/
│   │   ├── input/
│   │   │   ├── JoyConConstants.kt      # ボタンコード・軸定数
│   │   │   ├── JoyConState.kt          # 入力スナップショット
│   │   │   ├── JoyConInputHandler.kt   # HIDイベント解析
│   │   │   └── GestureDispatcher.kt    # ジェスチャー変換・実行
│   │   ├── service/
│   │   │   ├── JoyConAccessibilityService.kt  # メインサービス
│   │   │   └── CursorOverlayService.kt        # カーソル表示
│   │   ├── ui/
│   │   │   ├── MainActivity.kt
│   │   │   └── CursorView.kt           # カーソル描画View
│   │   ├── settings/
│   │   │   └── SettingsActivity.kt
│   │   └── utils/
│   │       ├── AppPreferences.kt       # 設定値の一元管理
│   │       ├── Extensions.kt           # deadzone/acceleration計算
│   │       └── BootReceiver.kt
│   ├── res/
│   │   ├── xml/
│   │   │   ├── accessibility_service_config.xml
│   │   │   └── preferences.xml
│   │   └── layout/
│   │       ├── activity_main.xml
│   │       └── activity_settings.xml
│   └── AndroidManifest.xml
```

---

## 設定項目

| 設定 | 範囲 | デフォルト | 説明 |
|------|------|-----------|------|
| カーソル速度 | 1–50 | 12 | ピクセル/フレーム |
| デッドゾーン | 0.01–0.30 | 0.08 | スティック無反応帯域 |
| 加速 | ON/OFF | ON | 低速=精密 / 高速=加速 |
| スクロール方式 | A/B案 | A案 | スクロール入力方式 |
| スクロール速度 | 1–30 | 8 | スクロール量 |
| ブーストモード | ホールド/トグル | ホールド | R ボタンの挙動 |

---

## アーキテクチャ概要

```
Joy-Con (BT HID)
     │ KeyEvent / MotionEvent
     ▼
JoyConInputHandler
  ├── applyDeadZone()
  └── StateFlow<JoyConState>
          │
     ┌────┴────────────────┐
     ▼                     ▼
GestureDispatcher    CursorOverlayService
  ├── moveCursor()         └── CursorView (overlay window)
  ├── performTap()
  ├── performScroll()
  └── AccessibilityService.dispatchGesture()
```

入力遅延目標: **50ms 以下**  
UIフレームレート: 60fps（16ms tick）

---

## トラブルシューティング

### Joy-Con が認識されない
- Android の Bluetooth 設定で「接続済み」になっているか確認
- Joy-Con の電源を入れ直してから再ペアリング
- 一部の Android ROM では HID GamePad として認識されないことがある（LineageOS 等は概ね動作）

### カーソルが動かない
- アクセシビリティサービスが有効か確認
- アプリを強制停止 → 再起動

### ボタンのキーコードが違う
- `JoyConConstants.kt` のコードは標準的なマッピングだが、  
  ファームウェアや Android バージョンにより異なる場合がある
- `MainActivity.tvLastInput` でボタン押下時のキーコードを確認し、定数を修正

---

## v2 予定機能

- [ ] キーコンビネーション（L+A = コピー など）
- [ ] 音声入力ショートカット
- [ ] アプリ別プロファイル
- [ ] 左 Joy-Con サポート
- [ ] 設定画面でのボタン再割り当てUI

---

## ライセンス

MIT License
