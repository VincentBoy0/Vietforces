# External Integrations

**Analysis Date:** 2026-07-22

## APIs & External Services

**AI / Language Model:**
- **OpenAI Chat Completions API** — core AI backend for all intelligent features
  - Endpoint: `https://api.openai.com/v1/chat/completions`
  - SDK/Client: Raw `OkHttpClient` (no official SDK) — `app/src/main/java/com/example/vietforces/data/remote/OpenAiClient.kt`
  - Auth: `BuildConfig.OPENAI_API_KEY` (injected from `local.properties` at build time; env var name: `OPENAI_API_KEY`)
  - Default model: `gpt-4.1-mini` (overridable via `OPENAI_MODEL` in `local.properties`)
  - Timeouts: 15s connect / 45s read
  - Response format: `json_object` mode enforced for structured calls; free-form text for hints

## Data Storage

**Databases:**
- None — no SQLite, Room, or remote database used.
- Vocabulary data is hardcoded in `app/src/main/java/com/example/vietforces/data/repository/VocabularyRepository.kt` as an in-memory Kotlin `List<VocabularyItem>`.

**Local Persistence:**
- `android.content.SharedPreferences` (file: `vietforces_prefs`)
  - Client: `app/src/main/java/com/example/vietforces/data/storage/PreferencesManager.kt` (singleton object)
  - Data stored: ELO rating & history, streaks, game mode stats, learned word IDs, daily practice history, profile (name/phone/address), notifications, mascot position/size, AI feature toggles, roleplay conversation sessions, spaced-repetition item encounter data
  - All complex objects serialized/deserialized as JSON strings using `org.json` (built-in Android)

**File Storage:**
- Local filesystem only — vocabulary images are bundled as drawable resources (`app/src/main/res/drawable/`)
  - Categories: `animal_001–020`, `food_001–020`, `clothing_001–009`, `household_001–013`, `kitchen_001–012`, `school_001–012`, `place_001–015`, `body_001–002`, `vehicle_001–006`

**Caching:**
- None — no HTTP cache layer, no Coil/Glide image cache (all images are local drawables)

## Authentication & Identity

**Auth Provider:**
- None — no user authentication system
- "Profile" is a simple local data store (name, phone, address) persisted in SharedPreferences via `ProfileManager` (`app/src/main/java/com/example/vietforces/data/manager/ProfileManager.kt`)

## AI Feature Details

The OpenAI integration is the only external service. Four distinct usage patterns exist in `AiManager` (`app/src/main/java/com/example/vietforces/data/manager/AiManager.kt`):

| Method | OpenAI Call | Purpose |
|--------|-------------|---------|
| `gradeWriting(topic, text)` | `completeJson` | Grade short Vietnamese paragraphs (score 0–10, mistakes, corrected version) |
| `gradeOpenAnswer(q, expected, user)` | `completeJson` | Grade fill-blank / open answers semantically |
| `mascotReact(context)` | `completeJson` (temp=1.0) | Generate mascot (rooster) reaction messages |
| `buildLearningPlan(statsSummary)` | `completeJson` | Produce personalised learning path from user stats |
| `roleplayReply(scenario, history)` | `completeJsonChat` | Multi-turn NPC conversation with inline corrections & suggestions |
| `roleplayHint(scenario, history)` | `completeChat` (free-form) | Suggest a single Vietnamese sentence the learner could say next |

**Toggle controls** (persisted in SharedPreferences):
- `aiFeedbackEnabled` — master toggle for grading/feedback/learning-path AI (`KEY_AI_FEEDBACK_ENABLED`)
- `aiMascotEnabled` — toggle for mascot AI reactions (`KEY_AI_MASCOT_ENABLED`)
- Both configurable on the Settings screen (`app/src/main/java/com/example/vietforces/ui/screens/SettingsScreen.kt`)

**Error handling:**
- All AI calls return `AiCallResult<T>` (sealed class: `Success` / `Error`) — never throws to UI
- `OpenAiClient.NotConfiguredException` surfaced when `OPENAI_API_KEY` is blank
- `isConfigError = true` flag lets the UI display a configuration hint rather than a generic error

## Monitoring & Observability

**Error Tracking:**
- None — no crash reporting (no Firebase Crashlytics, Sentry, etc.)

**Logs:**
- `android.util.Log` used selectively in `AiManager` (e.g., `Log.w("AiManager", ...)` on blank AI reply, `Log.e("AiManager", ...)` on roleplay failures)
- No structured logging framework

## CI/CD & Deployment

**Hosting:**
- Not deployed — academic/coursework project; builds to a debug APK
- No Play Store listing or distribution pipeline configured

**CI Pipeline:**
- None detected — no GitHub Actions, CircleCI, or Fastlane configuration files present

## Environment Configuration

**Required build-time values (in `local.properties`, git-ignored):**
- `OPENAI_API_KEY` — OpenAI secret key; app runs without AI features if omitted
- `OPENAI_MODEL` — (optional) model override; defaults to `gpt-4.1-mini`

**Android permissions (declared in `app/src/main/AndroidManifest.xml`):**
- `android.permission.INTERNET` — required for all AI network calls

**No other secrets or environment variables are used.**

## Webhooks & Callbacks

**Incoming:**
- None — no webhook endpoints

**Outgoing:**
- None — only synchronous (coroutine-based) HTTP calls to OpenAI; no push/event callbacks

---

*Integration audit: 2026-07-22*
