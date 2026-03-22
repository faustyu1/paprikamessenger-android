# Paprika Messenger — Android

Kotlin/Jetpack Compose мессенджер. Архитектура: MVVM, single-activity, Jetpack Navigation.

## Stack

- **UI**: Jetpack Compose + Material 3
- **Network**: Retrofit 2 + OkHttp 4 + Gson
- **Local DB**: Room (SQLite)
- **Images**: Coil
- **Real-time**: Custom WebSocket через OkHttp
- **Min SDK**: 29 (Android 10), Target SDK: 34

## Структура

```
app/src/main/java/ru/faustyu/paprika/
├── MainActivity.kt          # Single activity, NavHost, self-update
├── data/
│   ├── PrefsManager.kt      # SharedPreferences (token, backendUrl)
│   ├── network/
│   │   ├── NetworkModule.kt # Retrofit singleton, auth interceptor
│   │   ├── ApiService.kt    # REST endpoints + data models
│   │   └── Imports.kt       # WebSocket models
│   └── db/                  # Room entities, DAOs, AppDatabase
├── ui/
│   ├── auth/                # AuthScreen + AuthViewModel
│   ├── chat/                # ChatListScreen, ChatScreen, ViewModels
│   ├── groups/              # CreateGroupScreen
│   ├── profile/             # ProfileScreen, UserProfileScreen
│   ├── search/              # SearchScreen
│   ├── stories/             # StoriesScreen, StoriesViewModel
│   └── theme/               # Color, Type, Theme
└── util/
    ├── CryptoManager.kt     # AES + Diffie-Hellman E2E encryption
    └── UpdateChecker.kt     # GitHub Releases self-update
```

## Сборка

```bash
./gradlew assembleDebug      # debug APK
./gradlew assembleRelease    # release APK (minified)
./gradlew installDebug       # установить на устройство
```

## Конфигурация backend

По умолчанию `http://localhost:8080/`. В debug-режиме: тапнуть 3 раза на "Paprika Chats" в заголовке → появится диалог смены URL. URL сохраняется в SharedPreferences.

## Self-update

Проверяет GitHub Releases при каждом запуске:
- `UpdateChecker.kt` → `https://api.github.com/repos/faustyu1/paprikamessenger-android/releases/latest`
- Сравнивает `tag_name` с `BuildConfig.VERSION_NAME` (semver)
- Если новее — показывает AlertDialog
- "Update" → DownloadManager скачивает APK → BroadcastReceiver запускает установку
- Для выпуска обновления: создай GitHub Release с тегом `v1.x` и приложи APK

## NetworkModule

`NetworkModule` — object (singleton). Меняет URL динамически через `setCustomUrl()`. `authToken` — глобальный, устанавливается после логина.

## WebSocket

Подключение управляется в `ChatViewModel`. Поддерживаемые события: `new_message`, `message_status`, `user_typing`, `call:offer`, `call:answer`, `call:ice_candidate`.

## Звонки (статус)

Backend полностью готов (API + WebSocket signaling + FCM). В Android — заглушка:
- Кнопка звонка в `ChatScreen.kt:191` — `onClick = { /* Call */ }`
- WebRTC **не подключён** (нет зависимости `org.webrtc:google-webrtc`)
- Нет UI экранов звонка, нет разрешений на микрофон/камеру
- Для реализации: добавить WebRTC dep, PeerConnectionFactory, экраны звонка, обработку `call:*` событий WebSocket

## Ключевые правила

- Не мокать Room в тестах — использовать реальную in-memory БД
- `NetworkModule._api` инвалидируется при смене URL — не кэшировать `api` локально
- Версию приложения менять в `build.gradle.kts` (versionCode + versionName), теги GitHub Releases = `v{versionName}`
