---
phase: "00-pre-work-fixes"
plan: "01"
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/example/vietforces/data/manager/UserProgressManager.kt
  - app/src/main/java/com/example/vietforces/ui/screens/PerformanceScreen.kt
  - app/src/main/java/com/example/vietforces/data/manager/NotificationManager.kt
autonomous: true
requirements:
  - PRE-01

must_haves:
  truths:
    - "Zero occurrences of Locale.getDefault() remain in date-formatting code across all three files"
    - "Streak date keys (yyyy-MM-dd) are always generated with Locale.ROOT, preventing corruption on non-English devices"
    - "App builds cleanly after the change"
  artifacts:
    - path: "app/src/main/java/com/example/vietforces/data/manager/UserProgressManager.kt"
      provides: "Locale.ROOT date formatter for streak storage and comparison (lines 84, 189)"
    - path: "app/src/main/java/com/example/vietforces/ui/screens/PerformanceScreen.kt"
      provides: "Locale.ROOT date formatter for heatmap calendar generation (line 610)"
    - path: "app/src/main/java/com/example/vietforces/data/manager/NotificationManager.kt"
      provides: "Locale.ROOT date formatter for display timestamps (line 196)"
  key_links:
    - from: "UserProgressManager.getTodayDateString()"
      to: "SharedPreferences streak keys"
      via: "SimpleDateFormat(\"yyyy-MM-dd\", Locale.ROOT)"
      pattern: "Locale\\.ROOT"
---

<objective>
Fix PRE-01: Replace all four occurrences of `Locale.getDefault()` in date-formatting code with `Locale.ROOT`.

Purpose: `SimpleDateFormat` with `Locale.getDefault()` produces locale-specific digit characters on some devices (Arabic, Persian, Hindi locales), causing streak date keys like `١٤٤٦-٠١-١٥` to be stored in SharedPreferences. These keys never match future lookups, silently resetting every user's streak. Switching to `Locale.ROOT` forces ASCII digits and invariant formatting, making keys deterministic regardless of device language.

Output: Three modified Kotlin files, zero `Locale.getDefault()` hits in date-related code, clean build.
</objective>

<execution_context>
@~/.copilot/gsd-core/workflows/execute-plan.md
@~/.copilot/gsd-core/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md

Key constraints from CONTEXT.md:
- Fix ONLY the four confirmed SimpleDateFormat call sites listed below
- Do NOT change Locale usage in UI strings, resource formatting, or any non-date context
- Do NOT migrate SimpleDateFormat to java.time (deferred)
- namespace = "com.example.vietforces" is untouched (separate plan handles applicationId)
</context>

<tasks>

<task type="auto">
  <name>Task 1: Replace Locale.getDefault() with Locale.ROOT in UserProgressManager.kt (lines 84 and 189)</name>
  <files>app/src/main/java/com/example/vietforces/data/manager/UserProgressManager.kt</files>
  <action>
    Make exactly two string replacements in this file. Do NOT change any other code.

    Replacement 1 — line 84 (inside getTodayDateString()):
      OLD: val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
      NEW: val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)

    Replacement 2 — line 189 (inside the streak update logic):
      OLD: val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
      NEW: val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)

    Both occurrences use the same pattern string "yyyy-MM-dd". The replacement is identical for both.
    `java.util.Locale` is already imported in this file — no import changes needed.
    Commit message: "fix(PRE-01): use Locale.ROOT in UserProgressManager date formatters"
  </action>
  <verify>
    <automated>grep -n "Locale.getDefault()" app/src/main/java/com/example/vietforces/data/manager/UserProgressManager.kt | grep -i "dateformat\|SimpleDateFormat\|sdf" ; echo "Exit: $?"</automated>
  </verify>
  <done>grep returns zero matching lines (no Locale.getDefault() remains in date formatter calls in this file). Both line 84 and line 189 now read Locale.ROOT.</done>
</task>

<task type="auto">
  <name>Task 2: Replace Locale.getDefault() with Locale.ROOT in PerformanceScreen.kt (line 610) and NotificationManager.kt (line 196)</name>
  <files>
    app/src/main/java/com/example/vietforces/ui/screens/PerformanceScreen.kt
    app/src/main/java/com/example/vietforces/data/manager/NotificationManager.kt
  </files>
  <action>
    Make one replacement in each file. Do NOT change any other code.

    FILE 1 — PerformanceScreen.kt, line 610 (inside generateWeeksData()):
      OLD: val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
      NEW: val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)

    FILE 2 — NotificationManager.kt, line 196 (inside the relative-time display formatter):
      OLD: val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
      NEW: val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT)

    Note: NotificationManager.kt line 196 is a UI display formatter, but per CONTEXT.md it is fixed for consistency anyway.
    `java.util.Locale` is already imported in both files — no import changes needed.
    Commit message: "fix(PRE-01): use Locale.ROOT in PerformanceScreen and NotificationManager date formatters"
  </action>
  <verify>
    <automated>grep -rn "Locale.getDefault()" \
      app/src/main/java/com/example/vietforces/ui/screens/PerformanceScreen.kt \
      app/src/main/java/com/example/vietforces/data/manager/NotificationManager.kt \
      app/src/main/java/com/example/vietforces/data/manager/UserProgressManager.kt ; echo "Hits above (expect 0 in date formatter lines)"</automated>
  </verify>
  <done>
    All four target occurrences now use Locale.ROOT:
    - UserProgressManager.kt:84 ✓
    - UserProgressManager.kt:189 ✓
    - PerformanceScreen.kt:610 ✓
    - NotificationManager.kt:196 ✓
    grep -rn "Locale.getDefault()" across all three files returns zero hits in SimpleDateFormat/sdf call sites.
  </done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| Device locale → date string | Locale.getDefault() was allowing device locale to corrupt deterministic date keys |

## STRIDE Threat Register

| Threat ID | Category | Component | Disposition | Mitigation Plan |
|-----------|----------|-----------|-------------|-----------------|
| T-00-01 | Tampering | SharedPreferences streak keys | mitigate | Use Locale.ROOT so keys are ASCII-only and cannot be corrupted by device locale |
| T-00-02 | Information Disclosure | Date formatting in display | accept | Display dates (NotificationManager.kt:196) are now also Locale.ROOT; no PII risk, low-value target |
</threat_model>

<verification>
After both tasks complete, run a final codebase-wide check:

```
grep -rn "Locale.getDefault()" app/src/main/java/com/example/vietforces/data/manager/ app/src/main/java/com/example/vietforces/ui/screens/PerformanceScreen.kt
```

Expected: zero lines output.

Then attempt a clean build to confirm no compilation errors:
```
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL
</verification>

<success_criteria>
1. Zero occurrences of `Locale.getDefault()` in date-formatting call sites across the three target files
2. `./gradlew assembleDebug` exits 0 (BUILD SUCCESSFUL)
3. Two atomic commits exist: one for UserProgressManager, one for PerformanceScreen + NotificationManager — each referencing PRE-01
</success_criteria>

<output>
Create `.planning/phases/00-pre-work-fixes/00-01-SUMMARY.md` when done with:
- Files changed (3 files, 4 substitutions)
- Verification command output confirming zero remaining hits
- Build result
- Commit SHAs
</output>
