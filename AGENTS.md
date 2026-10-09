# MangaExplorer2

Android app for browsing manga across multiple sources (MangaDex API, MangaTown/MangaKakalot scraping).

## Stack

- Pure Kotlin (no Java source files)
- Gradle 9.8.0 wrapper, AGP 9.4.1, Kotlin 2.2.10
- compileSdk 36, minSdk 24, targetSdk 33
- kapt (Glide annotation processor), ViewBinding, Retrofit, jsoup

## Commands

- `./gradlew build` — full build
- `./gradlew assembleDebug` — debug APK
- `./gradlew test` — unit tests
- `./gradlew connectedAndroidTest` — instrumented tests (requires connected device)

## Architecture

- Package: `com.example.mangaexplorer2`
- Activities: `MainActivity`, `SearchActivity`, `ChapterActivity`, `PageReaderActivity`
- Manga sources in `mangaSources/` — each implements `MangaSource`; registered via `SourceRegister`
- Favorites stored in SQLite (`utility/FavoritenDB.kt`)
- Manifest declares `INTERNET` permission; uses cleartext traffic
