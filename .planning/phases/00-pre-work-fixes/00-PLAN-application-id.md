---
phase: "00-pre-work-fixes"
plan: "02"
type: execute
wave: 1
depends_on: []
files_modified:
  - app/build.gradle.kts
autonomous: true
requirements:
  - PRE-02

must_haves:
  truths:
    - "applicationId in app/build.gradle.kts is com.vietforces.app"
    - "namespace in app/build.gradle.kts remains com.example.vietforces (unchanged)"
    - "App builds cleanly — no manifest merge conflicts from the applicationId change"
  artifacts:
    - path: "app/build.gradle.kts"
      provides: "Updated applicationId suitable for FCM and Play Store registration"
      contains: "applicationId = \"com.vietforces.app\""
  key_links:
    - from: "app/build.gradle.kts defaultConfig"
      to: "FCM / Play Store registration"
      via: "applicationId = \"com.vietforces.app\""
      pattern: "applicationId.*com\\.vietforces\\.app"
---

<objective>
Fix PRE-02: Rename `applicationId` from `com.example.vietforces` to `com.vietforces.app` in `app/build.gradle.kts`.

Purpose: The current `applicationId = "com.example.vietforces"` is a placeholder that blocks FCM token registration and Play Store listing (Google rejects `com.example.*` package names). Changing it to `com.vietforces.app` unblocks Phase 4 (FCM notifications) and any future Play Store submission. The Kotlin `namespace` (`com.example.vietforces`) is deliberately left unchanged — namespace controls generated R/BuildConfig class packages and would require a full package rename if changed (deferred, out of scope).

Output: One line changed in `app/build.gradle.kts`, clean build confirming no manifest merge conflicts.
</objective>

<execution_context>
@~/.copilot/gsd-core/workflows/execute-plan.md
@~/.copilot/gsd-core/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md

Key constraints from CONTEXT.md:
- Change ONLY the `applicationId` line — one line, one file
- `namespace = "com.example.vietforces"` MUST remain unchanged
- Do NOT rename Kotlin packages or imports (full package rename is deferred)
- Do NOT modify AndroidManifest.xml (Gradle handles applicationId override automatically)
- New applicationId: com.vietforces.app (per D-PRE-02 decision in CONTEXT.md)
</context>

<tasks>

<task type="auto">
  <name>Task 1: Change applicationId in app/build.gradle.kts</name>
  <files>app/build.gradle.kts</files>
  <action>
    Make exactly ONE string replacement in `app/build.gradle.kts`.

    The file's `defaultConfig` block currently contains these two adjacent lines:
      namespace = "com.example.vietforces"      ← DO NOT TOUCH THIS LINE
      ...
      applicationId = "com.example.vietforces"  ← CHANGE THIS LINE ONLY

    Replacement (inside `defaultConfig { ... }`):
      OLD: applicationId = "com.example.vietforces"
      NEW: applicationId = "com.vietforces.app"

    IMPORTANT: There are two occurrences of "com.example.vietforces" in this file —
      1. `namespace = "com.example.vietforces"` — DO NOT change
      2. `applicationId = "com.example.vietforces"` — change this one

    Use a precise match on the full `applicationId = "com.example.vietforces"` string
    so that the namespace line is not accidentally modified.

    After making the change, verify with grep that:
      - `applicationId = "com.vietforces.app"` is present (exactly 1 hit)
      - `namespace = "com.example.vietforces"` is still present (exactly 1 hit)
      - `applicationId = "com.example.vietforces"` is absent (0 hits)

    Commit message: "fix(PRE-02): rename applicationId to com.vietforces.app"
  </action>
  <verify>
    <automated>
grep -n 'applicationId' app/build.gradle.kts
grep -n 'namespace' app/build.gradle.kts
    </automated>
  </verify>
  <done>
    - `grep 'applicationId' app/build.gradle.kts` shows exactly: `applicationId = "com.vietforces.app"`
    - `grep 'namespace' app/build.gradle.kts` still shows: `namespace = "com.example.vietforces"`
    - No line contains `applicationId = "com.example.vietforces"` any more
  </done>
</task>

<task type="auto">
  <name>Task 2: Clean build to confirm no manifest merge conflicts</name>
  <files>app/build.gradle.kts</files>
  <action>
    Run a clean debug build to confirm the applicationId change does not introduce manifest
    merge conflicts or other build errors.

    Command:
      ./gradlew clean assembleDebug

    Expected outcome: BUILD SUCCESSFUL with no manifest merge error mentioning
    "com.example.vietforces" or "package" attribute conflicts.

    If the build fails:
    - Check the error for any AndroidManifest.xml `package` attribute conflict.
      Gradle 7+ manages applicationId entirely via build.gradle.kts; there should be no
      `package` attribute in AndroidManifest.xml to conflict with. If one exists, remove it.
    - Do NOT change the namespace line.
    - Do NOT rename any Kotlin source packages.

    Commit message (only needed if a manifest fix was required):
      "fix(PRE-02): remove stale package attribute from AndroidManifest.xml"
  </action>
  <verify>
    <automated>./gradlew assembleDebug 2>&1 | tail -5</automated>
  </verify>
  <done>
    `./gradlew assembleDebug` exits 0 and the last lines contain "BUILD SUCCESSFUL".
    The generated APK at `app/build/outputs/apk/debug/app-debug.apk` exists and has
    applicationId `com.vietforces.app` (confirmable via `aapt dump badging` if needed).
  </done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| Build config → APK manifest | applicationId propagates into the installed APK's package; incorrect value would block FCM registration |

## STRIDE Threat Register

| Threat ID | Category | Component | Disposition | Mitigation Plan |
|-----------|----------|-----------|-------------|-----------------|
| T-00-03 | Tampering | app/build.gradle.kts applicationId | mitigate | Replace only the applicationId line; grep gate confirms namespace is untouched |
| T-00-04 | Denial of Service | Manifest merge conflict from old package attribute | mitigate | Clean build in Task 2 surfaces any conflict immediately; fix path documented in action |
</threat_model>

<verification>
Final checks after both tasks:

```bash
# 1. Confirm applicationId value
grep 'applicationId' app/build.gradle.kts
# Expected: applicationId = "com.vietforces.app"

# 2. Confirm namespace is unchanged
grep 'namespace' app/build.gradle.kts
# Expected: namespace = "com.example.vietforces"

# 3. Confirm no old applicationId remains
grep 'applicationId.*com.example' app/build.gradle.kts
# Expected: (no output)

# 4. Build passes
./gradlew assembleDebug
# Expected: BUILD SUCCESSFUL
```
</verification>

<success_criteria>
1. `app/build.gradle.kts` contains `applicationId = "com.vietforces.app"` (exactly 1 hit)
2. `app/build.gradle.kts` still contains `namespace = "com.example.vietforces"` (unchanged)
3. `./gradlew assembleDebug` exits 0 — BUILD SUCCESSFUL
4. One atomic commit exists referencing PRE-02 with message "fix(PRE-02): rename applicationId to com.vietforces.app"
</success_criteria>

<output>
Create `.planning/phases/00-pre-work-fixes/00-02-SUMMARY.md` when done with:
- The single line changed (old vs new)
- grep output confirming applicationId and namespace values
- Build result (exit code + BUILD SUCCESSFUL confirmation)
- Commit SHA
</output>
