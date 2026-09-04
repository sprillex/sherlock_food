# AGENTS.md - Android Build & Environment Standards

## Build Environment & Toolchains
- **JDK Target:** OpenJDK 17 (LTS) — Do not target or require JDK 21+ for Gradle execution.
- **Gradle Wrapper:** Use Gradle 8.7+ wrapper (`./gradlew`).
- **Android Gradle Plugin (AGP):** Use AGP `8.4.2` or later to ensure compatibility with modern SDK transforms.
- **Compile / Target SDK:** Android API 34 (Upside Down Cake) or API 35.
- **Min SDK:** Android API 26.

## Gradle & Kotlin Rules
- Always declare Java & Kotlin compilation targets using the Gradle JVM Toolchain:
  ```kotlin
  kotlin {
      jvmToolchain(17)
  }
