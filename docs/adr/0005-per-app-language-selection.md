# ADR 0005: Per-app language selection

Date: 2026-08-02

Status: accepted for the v0.2.0 candidate

## Context

Open Scanner ships complete English and Simplified Chinese resources, but relying only on the device locale gives users no way to recover from an unwanted language or keep this app in a different language from the rest of Android. Android 13 introduced system-managed per-app locales; Android 8–12 require a compatibility implementation for the same in-app picker behavior.

## Decision

- Settings offers exactly **System default**, **English**, and **Simplified Chinese**.
- **System default** is the clean-install and reset value. It is represented by an empty application-locale list, so the app follows later device-language changes instead of copying the current locale once.
- `AppCompatDelegate.setApplicationLocales` is the single setter on API 26–36. `MainActivity` extends `AppCompatActivity`, allowing the same path to recreate and relocalize the Compose activity on older Android versions while synchronizing with Android's system App language setting on Android 13 and newer.
- Android Gradle Plugin generates the packaged locale configuration from the app resources. Locale filters constrain it to `en` and `zh-CN`, with `en` declared as the unqualified fallback in `resources.properties`.
- AppCompat's disabled, non-exported locale metadata holder stores the selection on API 32 and lower. Android owns storage and synchronization on API 33 and newer; the app does not maintain a competing DataStore locale value.
- Reset settings clears the application-locale override in addition to restoring the existing privacy, report-redaction, and refresh defaults.
- The transitive EmojiCompat startup initializer is removed. Open Scanner's UI is Compose-only and does not need AppCompat View emoji processing, so it should not create that background font-loading work or query a system downloadable-font provider.

## Consequences

- Selecting a language applies immediately through an activity configuration change and persists across process restarts.
- A clean install or reset continues tracking the device language in both directions.
- The app adds the pinned stable AppCompat dependency and explicitly declares the already-resolved AndroidX Startup runtime used by the manifest merge. Dependency verification records hashes for the new graph.
- The package adds no permission, `INTERNET` access, exported component, enabled background service, scan loop, or telemetry path. The locale metadata holder service is disabled and non-exported.
- Locale behavior is covered by pure tag-policy tests, English/Chinese resource-parity tests, Compose selector tests, and an API 36 emulator flow. API 26–32 physical compatibility remains part of the broader device matrix.

## References

- [Android per-app language preferences](https://developer.android.com/guide/topics/resources/app-languages)
- [AppCompatDelegate application locales](https://developer.android.com/reference/androidx/appcompat/app/AppCompatDelegate#setApplicationLocales(androidx.core.os.LocaleListCompat))
- [EmojiCompat initializer behavior](https://developer.android.com/reference/androidx/emoji2/text/EmojiCompatInitializer)
