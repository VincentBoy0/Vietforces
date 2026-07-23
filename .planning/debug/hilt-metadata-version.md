---
status: resolved
trigger: "hiltJavaCompileDebug FAILED: Provided Metadata instance has version 2.4.0, while maximum supported version is 2.1.0"
created: 2026-07-23
updated: 2026-07-23
---

## Symptoms
- Task `:app:hiltJavaCompileDebug` fails
- Error: `Provided Metadata instance has version 2.4.0, while maximum supported version is 2.1.0`
- Stack trace: `dagger.spi.internal.shaded.androidx.room.jarjarred.kotlinx.metadata.jvm.internal.JvmReadUtils.throwIfNotCompatible`

## Root Cause

**Hilt 2.51.1 bundles an old shaded `kotlinx-metadata-jvm`** that only supports Kotlin metadata up to version 2.1.0.

Some classes in the dependency tree (newer AndroidX/Compose libraries like `activityCompose = "1.12.2"`) were compiled with Kotlin 2.4.x and embed metadata version 2.4.0. When Hilt's annotation processor resolves `@Inject` bindings, it reads Kotlin metadata from those classes and crashes because its bundled reader maxes out at 2.1.0.

## Evidence
- `dagger.spi.internal.shaded.androidx.room.jarjarred.kotlinx.metadata.jvm.internal.JvmReadUtils.throwIfNotCompatible$kotlinx_metadata_jvm` — shaded library inside Hilt itself
- `LATEST_STABLE_SUPPORTED = KotlinClassMetadata.Version(2, 1, 0)` in Hilt 2.51.1's shaded copy
- `activityCompose = "1.12.2"` and other 2025/2026 AndroidX libs compiled with Kotlin 2.4.x

## Fix Applied

Upgraded `hilt = "2.51.1"` → `"2.56.2"` in `gradle/libs.versions.toml`.

Hilt 2.56.2 bundles a newer `kotlinx-metadata-jvm` that supports metadata version 2.4.0+.

## Resolution
- root_cause: Hilt 2.51.1 shaded kotlinx-metadata-jvm too old for metadata 2.4.0
- fix: Upgraded hilt to 2.56.2 in libs.versions.toml
- files_changed: gradle/libs.versions.toml
