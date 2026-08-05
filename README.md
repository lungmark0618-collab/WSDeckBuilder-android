# WS 牌組管理器 — Android

[iOS 版](https://github.com/lungmark0618-collab/WSDeckBuilder)的 Android 移植，
Kotlin + Jetpack Compose。個人自用工具，不上架。

> **非官方粉絲專案**，與 Bushiroad 及各原作品權利方無任何關聯，非商業用途。

## 與 iOS 版共用什麼

兩邊不共用程式碼，共用的是**兩個資料格式**——這已經是事實上的 API：

| 介面 | 效果 |
|---|---|
| 卡表 JSON（`*_cards.json`） | 同一份資料、同一套中文譯文，由同一條 Python 管線產出 |
| `manifest.json` | 線上更新機制通用，出新彈兩邊同時拿到 |
| QR 載荷 `WSD1\|牌組名\|卡號:張數` | **iOS 匯出的牌組圖，Android 掃得回來，反之亦然** |

## 卡片資料

**這個 repo 不含卡片資料**，理由與 iOS 版相同（日文卡面著作權屬 Bushiroad，
中文譯文是其衍生著作）。建置前先取得：

```bash
python3 ../WSDeckBuilder/tools/fetch_published_cards.py --out app/src/main/assets
```

## 建置

```bash
./gradlew :app:assembleDebug
```

需要 Android SDK 36、JDK 17+（Android Studio 內建的 JBR 可用）。

## 進度

- [x] 卡表載入、作品選單、卡片網格、搜尋、卡片詳情
- [x] 卡圖快取（記憶體 + 磁碟，放 `filesDir` 不放 `cacheDir`，避免被系統清除）
- [ ] 篩選（等級／顏色／種類／判定標誌／特徵）
- [ ] 牌組管理（Room）、規則驗證、統計
- [ ] 牌組出圖 + QR、掃圖匯入
- [ ] 卡表線上更新

## 踩過的坑

**卡圖伺服器沒有瀏覽器 User-Agent 就回 404**。實測 `curl` 無 UA → 404、有 UA → 200。
iOS 的 `ImageCache` 與 `fetch_cards.py` 都有設，Android 這邊靠 OkHttp 攔截器加上。

**Coil 3 不會自動註冊網路載入器**。少了 `OkHttpNetworkFetcherFactory`，卡圖就是
一片空白而且**不會有任何錯誤訊息**，很難查。

**AGP 8.13 不支援 compileSdk 37**——SDK 37 只有 `android-37.0` 這種帶小版號的
platform，AGP 8.x 找的是 `android-37`。用 36。而 AGP 9.x 內建 Kotlin 支援，
會跟 `org.jetbrains.kotlin.android` 撞名。

## 著作權

卡片文字與圖片的著作權屬 **Bushiroad** 及各原作品權利方。卡圖不隨程式打包也不散布，
一律由 App 從官方網址載入後快取於裝置本機。若權利方認為有不妥之處，請開 issue。
