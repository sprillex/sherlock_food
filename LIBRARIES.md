# `sherlock_food` Software Bill of Materials (SBOM) & Security Manifest

> **Source of Truth:** Parsed directly from `gradle/libs.versions.toml`, `build.gradle.kts`, `settings.gradle.kts`, and `app/build.gradle.kts`.
> **Note on BOM-Managed Compose Artifacts:** `androidx.compose.ui:*`, `androidx.compose.material3:material3`, and `androidx.compose.material:material-icons-extended` omit explicit versions in `gradle/libs.versions.toml` and are pinned by `androidx.compose:compose-bom` (`2024.02.02`).

<!-- BEGIN_DEPENDENCY_MANIFEST -->
```json
[
  {
    "name": "androidx.core:core-ktx",
    "version": "1.12.0",
    "scope": "runtime",
    "ecosystem": "Maven",
    "license": "Apache-2.0",
    "direct": true
  },
  {
    "name": "androidx.lifecycle:lifecycle-runtime-ktx",
    "version": "2.7.0",
    "scope": "runtime",
    "ecosystem": "Maven",
    "license": "Apache-2.0",
    "direct": true
  },
  {
    "name": "androidx.activity:activity-compose",
    "version": "1.8.2",
    "scope": "runtime",
    "ecosystem": "Maven",
    "license": "Apache-2.0",
    "direct": true
  },
  {
    "name": "androidx.compose:compose-bom",
    "version": "2024.02.02",
    "scope": "runtime",
    "ecosystem": "Maven",
    "license": "Apache-2.0",
    "direct": true
  },
  {
    "name": "androidx.compose.ui:ui",
    "version": "bom:2024.02.02",
    "scope": "runtime",
    "ecosystem": "Maven",
    "license": "Apache-2.0",
    "direct": true
  },
  {
    "name": "androidx.compose.ui:ui-graphics",
    "version": "bom:2024.02.02",
    "scope": "runtime",
    "ecosystem": "Maven",
    "license": "Apache-2.0",
    "direct": true
  },
  {
    "name": "androidx.compose.ui:ui-tooling",
    "version": "bom:2024.02.02",
    "scope": "runtime",
    "ecosystem": "Maven",
    "license": "Apache-2.0",
    "direct": true
  },
  {
    "name": "androidx.compose.ui:ui-tooling-preview",
    "version": "bom:2024.02.02",
    "scope": "runtime",
    "ecosystem": "Maven",
    "license": "Apache-2.0",
    "direct": true
  },
  {
    "name": "androidx.compose.material3:material3",
    "version": "bom:2024.02.02",
    "scope": "runtime",
    "ecosystem": "Maven",
    "license": "Apache-2.0",
    "direct": true
  },
  {
    "name": "androidx.compose.material:material-icons-extended",
    "version": "bom:2024.02.02",
    "scope": "runtime",
    "ecosystem": "Maven",
    "license": "Apache-2.0",
    "direct": true
  },
  {
    "name": "com.google.android.gms:play-services-location",
    "version": "21.1.0",
    "scope": "runtime",
    "ecosystem": "Maven",
    "license": "Apache-2.0",
    "direct": true
  },
  {
    "name": "androidx.room:room-runtime",
    "version": "2.6.1",
    "scope": "runtime",
    "ecosystem": "Maven",
    "license": "Apache-2.0",
    "direct": true
  },
  {
    "name": "androidx.room:room-ktx",
    "version": "2.6.1",
    "scope": "runtime",
    "ecosystem": "Maven",
    "license": "Apache-2.0",
    "direct": true
  },
  {
    "name": "androidx.room:room-compiler",
    "version": "2.6.1",
    "scope": "kapt",
    "ecosystem": "Maven",
    "license": "Apache-2.0",
    "direct": true
  },
  {
    "name": "org.jetbrains.kotlinx:kotlinx-serialization-json",
    "version": "1.6.3",
    "scope": "runtime",
    "ecosystem": "Maven",
    "license": "Apache-2.0",
    "direct": true
  },
  {
    "name": "junit:junit",
    "version": "4.13.2",
    "scope": "test",
    "ecosystem": "Maven",
    "license": "EPL-1.0",
    "direct": true
  },
  {
    "name": "androidx.test.ext:junit",
    "version": "1.1.5",
    "scope": "androidTest",
    "ecosystem": "Maven",
    "license": "Apache-2.0",
    "direct": true
  },
  {
    "name": "androidx.test.espresso:espresso-core",
    "version": "3.5.1",
    "scope": "androidTest",
    "ecosystem": "Maven",
    "license": "Apache-2.0",
    "direct": true
  },
  {
    "name": "com.android.tools.build:gradle",
    "version": "8.3.1",
    "scope": "build",
    "ecosystem": "Maven",
    "license": "Apache-2.0",
    "direct": true
  },
  {
    "name": "org.jetbrains.kotlin:kotlin-gradle-plugin",
    "version": "1.9.22",
    "scope": "build",
    "ecosystem": "Maven",
    "license": "Apache-2.0",
    "direct": true
  },
  {
    "name": "org.jetbrains.kotlin:kotlin-serialization",
    "version": "1.9.22",
    "scope": "build",
    "ecosystem": "Maven",
    "license": "Apache-2.0",
    "direct": true
  },
  {
    "name": "androidx.compose.compiler:compiler",
    "version": "1.5.8",
    "scope": "build",
    "ecosystem": "Maven",
    "license": "Apache-2.0",
    "direct": true
  }
]
```
<!-- END_DEPENDENCY_MANIFEST -->

---

## Security & Ecosystem Notes

- **Repositories (`settings.gradle.kts`):** `RepositoriesMode.FAIL_ON_PROJECT_REPOS` is enforced; only `google()` and `mavenCentral()` are enabled for dependency resolution (plus `gradlePluginPortal()` for `pluginManagement`).
- **Networking & Serialization:** No third-party HTTP client (OkHttp/Retrofit/Ktor) is bundled; `AiDiningClient` uses JDK `java.net.HttpURLConnection` directly over HTTPS (`generativelanguage.googleapis.com`) with `kotlinx-serialization-json:1.6.3`.
- **Gradle Wrapper:** `gradle/wrapper/gradle-wrapper.properties` pins `gradle-8.8-bin.zip` with `validateDistributionUrl=true`.
- **Companion Build Server (`android_tools/requirements.txt`, PyPI ecosystem — not bundled in APK):**
  - `fastapi>=0.100.0` (MIT)
  - `uvicorn>=0.22.0` (BSD-3-Clause)
  - `pydantic>=2.0.0` (MIT)
