# MangaExplorer2

Android app for browsing manga across multiple sources (MangaDex API, MangaTown/MangaKakalot scraping).

## Stack

- Pure Kotlin (no Java source files)
- Gradle 9.8.0 wrapper, AGP 9.4.1, Kotlin 2.2.10, JDK 21 target
- compileSdk 36, minSdk 24, targetSdk 33
- kapt (Glide annotation processor), ViewBinding, Retrofit, jsoup
- Foojay resolver plugin auto-detects JDK from Gradle daemon

## Commands

- `./gradlew build` — full build
- `./gradlew assembleDebug` — debug APK
- `./gradlew test` — unit tests (single example test only)
- `./gradlew connectedAndroidTest` — instrumented tests (requires connected device)

## Architecture

- Package: `com.example.mangaexplorer2`
- Activities in `activities/`: `MainActivity` (entry), `SearchActivity`, `ChapterActivity`, `PageReaderActivity`
- Fragments in root: `WebViewFragment`, `MyAppGlideModule` (Glide configuration)
- Adapters in `adapters/`: list adapters for search, favorites, chapters, and image display
- Manga sources in `mangaSources/` — each implements `MangaSource`; registered via `SourceRegister`
- `Mangadex.kt` uses Retrofit; `MangaTown.kt` and `MangaKakalot.kt` use jsoup scraping
- Favorites stored in SQLite (`utility/FavoritenDB.kt`, `utility/DbHelper.kt`)
- Manifest declares `INTERNET` and `WRITE_EXTERNAL_STORAGE` permissions; uses cleartext traffic (`usesCleartextTraffic="true"`)
- `android:largeHeap="true"` enabled for image-heavy reading