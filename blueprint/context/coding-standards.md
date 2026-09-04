# Coding Standards

> Your conventions, tuned by `/onboard` to the real stack: Android, Kotlin,
> Jetpack Compose, Gradle Kotlin DSL. Edit freely. Build and verification
> commands live in `AGENTS.md`.

## Stack

- Android app, single `:app` module, `com.example.vehiclemaintenance`
- Kotlin with the `official` code style (`kotlin.code.style=official`)
- Jetpack Compose with Material 3, no XML layouts
- Compose BOM pins Compose library versions; add Compose deps without versions
- `minSdk 29`, `targetSdk`/`compileSdk 37`, Java 11 source/target compatibility
- Gradle Kotlin DSL with the version catalog at `gradle/libs.versions.toml`
- Gradle configuration cache is on; keep build logic configuration-cache safe

## Dependencies

- Declare every new dependency in `gradle/libs.versions.toml` first, then
  reference it as `libs.some.library` in `app/build.gradle.kts`
- Never hardcode a version string in a build file
- Prefer AndroidX and first-party Jetpack libraries over third-party wrappers

## File Organization

- Source lives under `app/src/main/java/com/example/vehiclemaintenance/`
- Theme code stays in `ui/theme/` (`Color.kt`, `Theme.kt`, `Type.kt`)
- Group by feature as the app grows (for example `maintenance/`, `vehicles/`),
  not by layer-wide `models/` or `utils/` buckets
- Unit tests mirror the main package under `app/src/test/`
- Instrumented and Compose UI tests mirror it under `app/src/androidTest/`
- User-visible strings go in `app/src/main/res/values/strings.xml`, not hardcoded
  in composables

## Naming

- Composables: `PascalCase`, named for what they render (`VehicleCard`)
- Classes, interfaces, objects: `PascalCase`
- Functions, properties, parameters: `camelCase`
- Constants: `UPPER_SNAKE_CASE`
- Resource ids and file names: `snake_case`
- One top-level composable screen per file; small private helpers may share it

## Compose

- Composables are stateless where practical; hoist state to the caller
- Every composable takes a `modifier: Modifier = Modifier` as its first optional
  parameter and applies it to its root element
- Keep side effects inside the proper effect APIs (`LaunchedEffect`,
  `DisposableEffect`), never in the composable body
- Screen state comes from a `ViewModel` exposing an immutable state type; do not
  read repositories or storage directly from a composable
- Add an `@Preview` for new screens and reusable components

## Styling

- Material 3 theming through `VehicleMaintenanceTheme`
- Light and dark mode must both work; verify new UI in both
- Colors, type, and shapes come from `MaterialTheme`, never hardcoded hex values
  in a composable
- Respect insets and edge-to-edge; use the `Scaffold` padding that is provided

## Data

- Store data locally on device as the source of truth
- Remote backup or cloud sync is optional and additive; the app must work fully
  offline
- Keep persistence behind a repository interface so storage can change without
  touching UI code
- Do no disk or network work on the main thread; use coroutines with the correct
  dispatcher

> TODO: pick the local persistence library (Room, DataStore, or files) during
> planning and record the choice here.

## Error Handling

- Show user-friendly messages; never surface a raw exception or stack trace
- Model expected failures as return values (`Result` or a sealed state type)
  rather than throwing across layers
- Never swallow an exception silently; log it or surface it

## Testing

- Pure logic (calculations, formatting, state reduction) goes in JVM unit tests
  under `app/src/test/`
- UI behavior that needs a device goes in `app/src/androidTest/` using Compose
  UI test
- Only the scaffold example tests exist today. Run `/tests` to grow real
  coverage before treating tests as a gate.

## Code Quality

- No commented-out code unless specified
- No unused imports or variables
- Keep functions under 50 lines when possible
- `./gradlew lintDebug` should not gain new warnings from your change

## Comments

Write code that explains itself; comment only what the code cannot say.
Over-commenting is a common AI tell, so resist it.

- Comment the **why**, not the **what**. Delete any comment that restates the code.
- No banner/header blocks, section dividers, or step-by-step narration of obvious
  code. A file does not need a comment announcing each region.
- A comment earns its place only when it captures something the code can't: a
  non-obvious decision, a gotcha or workaround, why a value is what it is, or a
  link to a spec or issue.
- Prefer self-documenting names and small functions over explanatory comments.
- Keep doc comments minimal: a one-line purpose on an exported type or function is
  plenty.
- When in doubt, leave the comment out.

## Writing

- No em dashes (U+2014) in generated content: docs, comments, commit messages,
  READMEs, specs. They read as AI-generated.
- Use a hyphen for `term - description` separators; rephrase prose with commas,
  parentheses, or a colon. Avoid en dashes and the ellipsis character too.
