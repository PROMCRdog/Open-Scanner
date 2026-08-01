# ADR 0002: Android v0.1 toolchain

Date: 2026-07-31
Status: accepted

## Decision

Pin the first native build to Android Gradle Plugin 9.3.0, Gradle 9.5.0, JDK 17, Kotlin and Compose Compiler 2.3.21, and Compose BOM 2026.06.00. Compile against the installed stable Android 16 SDK 36.1 while targeting API 36 and supporting API 26 and newer.

Use AGP's built-in Kotlin support in Android modules. Pure JVM domain modules use the Kotlin JVM plugin. Dependency versions are centralized in `gradle/libs.versions.toml`; the Gradle distribution checksum is pinned in wrapper properties.

## Rationale

This combination follows the current stable Android documentation and matches the locally installed SDK/JDK. It avoids preview dependencies while preserving an API 37 compatibility path for a later release.

## Repository fallback

Google Maven, Maven Central, the Gradle plugin portal, and the canonical Gradle distribution are the authoritative sources. A China mirror may be used only after an authoritative source fails, and only when the downloaded artifact can be checked against an authoritative checksum.
