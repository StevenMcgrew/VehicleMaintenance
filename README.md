# Vehicle Maintenance

An Android app for tracking vehicle maintenance.

> The full problem statement, users, and feature scope are still being planned in
> `blueprint/project-plan.md` and `blueprint/build-plan.md`.

## Requirements

- Android Studio (or a JDK 11+ toolchain) and an Android SDK with API 37
- A device or emulator running Android 10 (API 29) or newer

## Commands

Run from the project root.

- Build debug APK: `./gradlew assembleDebug`
- Install on a running device or emulator: `./gradlew installDebug`
- Unit tests: `./gradlew testDebugUnitTest`
- Instrumented tests: `./gradlew connectedDebugAndroidTest`
- Android Lint: `./gradlew lintDebug`
- Full build plus checks: `./gradlew build`

## Stack

Kotlin, Jetpack Compose with Material 3, Gradle Kotlin DSL with a version
catalog at `gradle/libs.versions.toml`.

Agent instructions and project conventions live in `AGENTS.md`.
