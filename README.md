# MangaExplorer2

Android app for browsing and reading manga across multiple sources.

## Features

- Multi-source manga browsing (MangaDex, MangaTown)
- Search for manga by title
- Chapter navigation with pagination
- Page-by-page reading with gesture controls
- Favorites with reading progress tracking
- Update checking for new chapters

## Supported Sources

- **MangaDex** - Official API integration
- **MangaTown** - Web scraping integration

## Tech Stack

- Pure Kotlin
- AGP 9.4.1, Kotlin 2.2.10, JDK 21
- compileSdk 36, minSdk 24, targetSdk 33
- Retrofit (HTTP), jsoup (scraping), Glide (images)
- RecyclerView, ViewBinding, Material Components

## Build

```bash
./gradlew build          # Full build
./gradlew assembleDebug  # Debug APK
./gradlew test           # Unit tests
```

## Requirements

- Android SDK 36
- JDK 21 (auto-detected via Foojay resolver)
