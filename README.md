# Paprika Android Client

Modern, fast, and secure messenger client built with Kotlin and Jetpack Compose.

## Features
- **Jetpack Compose UI**: Modern, declarative UI with Material 3.
- **Real-time Messaging**: Full WebSocket integration for instant message delivery.
- **User Discovery**: Search users by username.
- **Stories**: View and create 24-hour stories (images/videos).
- **Profile Management**: Customizable bios and avatars with initial-based placeholders.
- **Offline First**: Room database caching for instant loading and offline viewing.
- **Dynamic Backend**: Change the API URL directly from the app for testing.

## Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Networking**: Retrofit + OkHttp
- **Local Database**: Room
- **Image Loading**: Coil
- **Real-time**: Custom WebSocket implementation

## Setup & Running

1. **Prerequisites**: Android Studio Ladybug or newer.
2. **Clone**: Clone the repository.
3. **Open**: Open the `PaprikaAndroid` folder as a project in Android Studio.
4. **Build**: Sync Gradle and build the project.
5. **Configuration**: 
   - By default, it connects to `http://10.0.2.2:8080` (Standard Android Emulator localhost).
   - You can change the backend URL in the login/chat screen by tapping the "Paprika Chats" title 3 times (debug feature).

## License
Attribution-NonCommercial-ShareAlike 4.0 International
