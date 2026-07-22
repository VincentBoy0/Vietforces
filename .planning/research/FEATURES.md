# Feature Landscape — VietForces

**Domain:** Gamified mobile language-learning app (Vietnamese vocabulary, Android)
**Researched:** 2026-07-22
**Confidence:** HIGH — patterns synthesized from Duolingo (500M users), Babbel, Memrise, Anki, and published retention research. Cross-checked against VietForces existing codebase and PROJECT.md requirements.

---

## Context

VietForces already ships 8 game modes, ELO ranking, and OpenAI AI features. This milestone adds the social/backend layer: Supabase auth, cloud sync, real-time leaderboard, daily challenges, streak system, social follow/compare, push notifications, onboarding, admin dashboard, and landing page.

Feature categorisation below distinguishes **what already exists**, **what must be added (table stakes)**, and **what can differentiate** in the market.

---

## Table Stakes

> Missing any of these and users perceive the product as incomplete or untrustworthy. These are the minimum bar for a release-quality language app.

### 1. Auth with Minimal Friction

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Google OAuth (one-tap) | Users abandon email+password signup at 40-60% — social login is the norm | Low | Supabase has built-in Google provider; Android one-tap SDK |
| Email + password fallback | Required for users without Google accounts | Low | Supabase Auth handles validation, hashing |
| "Try before signup" guest mode | Duolingo's most powerful acquisition pattern — first lesson before account wall | Medium | Store progress in SharedPreferences, migrate to Supabase on signup |
| Persistent login (remember me) | Session token persists across app restarts | Low | Supabase JWT auto-refresh handles this |
| Password reset via email | Trust baseline — without it users assume the app is low quality | Low | Supabase built-in |
| Clear auth error messages | Wrong password, email taken, network error — each needs a distinct message | Low | Map Supabase error codes to user-friendly strings |

**Verdict:** Use Google OAuth as primary CTA, email/password as secondary. Guest mode is the single highest-ROI pattern from Duolingo — do NOT gate gameplay behind registration.

---

### 2. Onboarding Flow (Goal-Setting + Level Placement)

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Welcome screen with mascot (Rooster) | Sets tone, introduces character, reduces abandonment | Low | Already have Rooster mascot |
| Goal selection (5/10/15/20 min/day) | Duolingo research: users who set goals have 2× retention at day 7 | Low | Store as `daily_goal_minutes` in user profile |
| Level placement (Beginner/Intermediate/Advanced) | Avoids boring experts and overwhelming beginners | Medium | 5-question quiz or self-report; maps to `VocabularyItem.difficulty` |
| Avatar / display name setup | Personalisation = commitment; named users return at higher rates | Low | Display name + optional avatar (Supabase Storage) |
| Feature tour (swipe-through, skippable) | Show streak, leaderboard, AI features — set expectation of value | Low | 3-4 slides max; always skippable |
| Push notification opt-in (in context) | Android 13+ requires explicit permission; ask after first positive moment | Low | Ask after first game completion, not at launch |

**Verdict:** Maximum 4 screens before the user can play. Every additional screen costs ~10% of users. The order is: Welcome → Goal → Level → Name → Play.

---

### 3. Streak System (Core Retention Loop)

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Daily streak counter visible on home | Duolingo's #1 retention lever; losing a streak is one of the most-cited motivators to return | Low | Extend existing `UserProgressManager` streak; sync to Supabase |
| Streak calendar / heatmap | Visual history of consistency; users feel accountability to fill gaps | Medium | Already have heatmap concept in existing code |
| Streak freeze (grace day) | Reduces churn after a missed day; Duolingo saw dramatic retention improvement adding this | Medium | 1 free freeze/week or earned via XP; stored in Supabase |
| "Streak in danger" push notification | Sent ~2h before midnight if no activity today; single highest-ROI notification | Low | Firebase Cloud Messaging triggered from Supabase Edge Function |
| Comeback bonus (after break) | Reduce shame/barrier to return after streak loss; "Welcome back — pick up where you left off" | Low | Show special animation + bonus XP if returning after 3+ day absence |
| Streak milestone celebrations | 7-day, 30-day, 100-day — in-app animation + achievement badge | Low | Tie to existing ELO achievement system |

**Verdict:** The streak is the single most important retention mechanic. It must be server-authoritative (Supabase), not local, so it persists across device reinstalls. Implement streak freeze — without it you lose users on the first missed day.

---

### 4. Cloud Sync (Progress Preservation)

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| ELO sync to Supabase | Users expect progress to survive reinstall/new device | Medium | Last-write-wins strategy noted in PROJECT.md |
| Streak sync | Same as above — losing streak on reinstall = guaranteed uninstall | Low | Supabase `user_progress` table |
| Vocabulary encountered sync | Spaced repetition weights must persist | Medium | `EncounteredItemsManager` data → Supabase `vocabulary_progress` table |
| Offline-capable gameplay | Must play without internet; sync on reconnect | Medium | Keep SharedPreferences as write-through cache; sync on app resume |
| Sync conflict resolution | Last-write-wins for ELO; max-value for streak (don't punish user) | Low | Simple `updated_at` timestamp comparison |

---

### 5. Leaderboard (Global + Friends)

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Global ELO leaderboard | Competitive hook — users play more to rank up | Medium | Supabase `users` table ordered by `elo_rating`; paginated |
| Top 100 display | Full global list is noise; top 100 is aspirational | Low | Paginated query; show user's own rank regardless |
| User's own rank + percentile | "You are #847 — top 12%" is motivating even off the leaderboard | Low | Subquery: `COUNT(*) WHERE elo > user_elo` |
| Weekly reset / seasons | Duolingo's league reset creates urgency each week | Medium | Separate `weekly_xp` column; reset via Supabase cron; archive top 3 |
| Real-time updates | Watching your rank change live during active sessions is deeply engaging | Medium | Supabase Realtime channel on `leaderboard_view` |
| Friends leaderboard sub-tab | More motivating than global — you know these people | Medium | Filter leaderboard by friendship table |

---

### 6. Daily Challenge

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| One challenge per day (server-generated) | Creates a reason to open the app daily; "habit anchor" | Medium | Supabase Edge Function generates challenge at midnight per timezone |
| Countdown timer to next challenge | "23:14:32 until new challenge" — FOMO mechanic | Low | Client-side countdown from challenge expiry timestamp |
| Bonus XP/ELO reward for completion | Must feel worth doing vs. regular games | Low | 2× multiplier on ELO gain for daily challenge |
| Completion indicator | Green checkmark on home screen = daily satisfaction signal | Low | `daily_challenge_completions` table |
| Challenge variety | Rotate game modes (ImageToWord, WordSearch, etc.) — not always the same game | Low | Random selection from `GameMode` enum server-side |
| Streak credit | Completing daily challenge counts toward streak | Low | Tie into streak update logic |

---

### 7. Push Notifications (Smart, Not Spammy)

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Daily reminder (user-chosen time) | Users who enable notifications show 2-4× better D30 retention | Low | FCM scheduled message; user sets time in settings |
| Streak danger warning | Highest-urgency, highest-ROI notification | Low | Trigger: no activity by 21:00 local time |
| Friend activity ("Sarah just passed your ELO") | Social pressure is highly effective for young adult learners | Medium | Triggered from Supabase DB webhook → FCM |
| Daily challenge available | "New challenge is ready" at midnight | Low | Scheduled via Supabase cron |
| Achievement unlocked | Celebrate milestones immediately | Low | Triggered in-app + background notification |
| Weekly summary | "This week: 7 days streak, 150 new words" — positive reinforcement | Low | Sunday evening push; generated from Supabase |
| Notification frequency cap | Max 2 push/day to avoid uninstall | Low | Track send count in Supabase; gate all notification sends |

---

### 8. Empty States & Skeleton Screens

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Leaderboard empty state (new user) | Without it, empty list looks broken | Low | "Play your first game to earn ELO and appear here" |
| Friends list empty state | "Invite your first friend" CTA with share button | Low | — |
| No daily challenge loaded (offline) | Graceful degradation message | Low | — |
| Skeleton screens for all data loads | Perceived performance; prevents jarring content pop-in | Medium | Shimmer effect on leaderboard, challenge card |

---

## Differentiators

> Features that set VietForces apart from generic language apps. These are not expected, but create competitive advantage and word-of-mouth.

### 1. ELO Ranking (Already Built — Must Be Showcased)

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Codeforces-style ELO with Vietnamese rank names | "Newbie → Huyền Thoại" — culturally resonant for Vietnamese learners | Already built | Must be surfaced on profile, leaderboard, and onboarding |
| Rank badge on profile card | Visual identity — users share their rank on social media | Low | Render rank badge in profile composable |
| Rank-up animation | Emotional peak moment — "You reached Chuyên Gia!" | Low | Full-screen confetti + mascot reaction |
| Rank history graph | Show ELO progression over time | Medium | Line chart from `elo_history` table in Supabase |

---

### 2. AI-Powered Personalisation (Already Built — Must Be Connected to Social)

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| AI writing grading publicly visible | Show friends your writing score on profile | Low | Opt-in: share writing score in activity feed |
| AI roleplay session replay | Save and replay best AI conversations to profile | Medium | Store `ai_conversation` in Supabase with opt-in sharing |
| AI-generated personalised challenge | Daily challenge generated based on *your* weak vocabulary areas | High | Query `vocabulary_progress` for low-success items; pass to challenge generator |
| Mascot Rooster reactions to friend activity | "Rooster says: Lan just scored higher than you!" | Low | Push notification content uses mascot personality |

---

### 3. Social Features (Friends + Compare)

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Follow system (asymmetric like Twitter) | Lower barrier than mutual friendship — follow anyone on leaderboard | Medium | `follows(follower_id, following_id)` table + Supabase RLS |
| Activity feed (following's milestones) | "Nam achieved 30-day streak", "Linh ranked up to Chuyên Gia" | Medium | Supabase Realtime feed or polling; event-driven from progress updates |
| Head-to-head ELO comparison | Profile screen showing your ELO vs. followed user's ELO on same chart | Medium | Side-by-side stat cards |
| Invite friends via share link | Deep-link to app download + auto-follow the inviter | Medium | Firebase Dynamic Links or App Links; Supabase referral tracking |
| Friend-only leaderboard tab | "Among your 12 friends, you rank #3" — more motivating than global rank | Medium | Filter leaderboard by follow graph |

**Key insight:** Language learners use social features differently than gamers. The primary motivation is *social accountability* ("my friend will see I missed today") not competition. Build accountability features (shared streaks, activity visibility) before competitive features (challenge a friend).

---

### 4. Streak Freeze + Streak Repair (Retention Safety Net)

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Streak freeze (1/week free) | Duolingo's most impactful retention feature post-2016 | Medium | `streak_freezes` count in user profile; auto-apply on missed day if available |
| Streak repair for 24h after miss | Pay XP/ELO to restore a broken streak within 24h | Medium | Supabase function checks `last_activity` and `streak_broken_at` |
| "Teammate streak" (shared streak) | Two friends maintain a shared streak — both must play daily | High | Defer to later milestone; complex edge cases |

---

### 5. Achievement / Badge System

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Category mastery badges (Animals, Food, etc.) | Sense of completion per vocabulary category | Low | Check 100% completion rate for category in `vocabulary_progress` |
| Game mode mastery badges | "WordSearch Expert" for 50 wins in a mode | Low | Counter per game mode in Supabase |
| Social badges ("First Friend", "Invited 5 users") | Drive social growth | Low | Event-driven from social actions |
| Badge display on public profile | Shareable identity | Low | Render up to 6 featured badges on profile card |
| Badge unlock push notification | Immediate celebration moment | Low | — |

---

## Admin Dashboard Features

> For vocabulary-based language apps, the admin surface needs to be minimal but powerful. Over-built admin dashboards waste development time — focus on what actually changes at operating scale.

### Vocabulary CRUD (Highest Priority)

| Feature | Why Needed | Complexity | Notes |
|---------|------------|------------|-------|
| List all vocabulary with search/filter | Finding the word to edit quickly | Low | Table with search by `word`, filter by `category` |
| Add new vocabulary item | Expand content library | Medium | Form: Vietnamese word, English translation, category, difficulty, image upload, syllables, distractors |
| Edit vocabulary item | Fix errors, add missing data | Low | Same form as Add |
| Delete / soft-delete | Remove inappropriate content | Low | Soft delete with `deleted_at` timestamp |
| Bulk CSV import | Fastest way to add 100+ words at once | Medium | Parse CSV → validate → bulk insert; preview before confirm |
| Image upload to Supabase Storage | Vocabulary images are core to VietForces | Medium | Drag-and-drop upload; auto-resize to standard dimensions |
| Preview vocabulary in game context | See how word will appear in ImageToWord / WordSearch before publishing | Medium | Inline mock-up renders |

### User Management

| Feature | Why Needed | Complexity | Notes |
|---------|------------|------------|-------|
| User list with search | Find a specific user by name/email | Low | — |
| View user profile (ELO, streak, stats) | Support and moderation context | Low | Read-only view |
| Ban / suspend user | Handle abuse | Low | Set `is_banned` flag; Supabase RLS blocks banned users |
| Reset ELO (admin action) | Fix data issues, reset test accounts | Low | Logged action with reason |
| Impersonate / view as user | Debug experience issues | High | **Defer** — complex security surface, not needed for demo |

### Analytics Dashboard

| Feature | Why Needed | Complexity | Notes |
|---------|------------|------------|-------|
| DAU / MAU chart | Health indicator | Medium | Aggregate from `user_sessions` table; Chart.js or Recharts |
| D1/D7/D30 retention cohorts | Shows if new features improve long-term retention | High | SQL cohort query from `user_created_at` vs `last_active` — **use simple table view first** |
| Game mode usage breakdown | Which modes are most/least played | Low | Count from `game_sessions` grouped by `game_mode` |
| Top 10 users by ELO | At-a-glance leaderboard status | Low | Simple query |
| New registrations per day | Growth signal | Low | Count from `users.created_at` |
| AI feature usage (roleplay/writing calls) | Monitor OpenAI cost | Low | Count from `ai_calls` log table |
| Streak distribution | How many users have 1/7/30/100+ day streaks | Low | Histogram from `user_progress.current_streak` |

### Daily Challenge Management

| Feature | Why Needed | Complexity | Notes |
|---------|------------|------------|-------|
| View upcoming challenges | See what users will see | Low | List of pre-generated challenges |
| Create manual challenge | Override auto-generated challenge for a specific date | Medium | Form: date, game mode, vocabulary set, bonus XP |
| Challenge completion stats | How many users completed today's challenge | Low | Count from `daily_challenge_completions` |

### Content Moderation

| Feature | Why Needed | Complexity | Notes |
|---------|------------|------------|-------|
| AI conversation log viewer | Review roleplay/writing sessions for inappropriate content | Medium | Paginated list with filter by flag; show conversation transcript |
| Flag / review queue | Prioritise flagged content for review | Medium | Users can flag AI responses; admin sees queue |
| **Full conversation search** | **Defer** — privacy risk, complex | — | Not needed for academic demo |

---

## Landing Page Features

> Language learning landing pages follow a well-established conversion formula. The goal is to drive Play Store installs, not sign-ups on the web.

### Must-Have Sections

| Section | Purpose | Conversion Impact | Notes |
|---------|---------|-------------------|-------|
| Hero: App name + tagline + screenshot | First impression; communicate value in 3 seconds | Very High | Tagline focus: "Learn Vietnamese through play" — action-oriented |
| App screenshots / short demo video | Show game modes in action; reduce "what is this?" uncertainty | Very High | 3-4 screenshots of best game modes (ImageToWord, WordSearch + daily challenge) |
| Feature highlights (3-4 cards) | Answer "why this app over others?" | High | Game modes, ELO ranking, AI features, daily challenges |
| Social proof | Build trust | High | "Designed for learners" + GitHub stars if open source, or "part of academic showcase" |
| Download CTA (Play Store badge) | Primary conversion action | Very High | Sticky CTA in header + hero + footer |
| FAQ (3-5 questions) | Reduce remaining uncertainty before download | Medium | "Is it free?", "Do I need internet?", "What level is it for?" |
| Footer (links, about, contact) | Professionalism signal | Low | Link to GitHub repo for academic context |

### Nice-to-Have Sections

| Section | Complexity | Notes |
|---------|------------|-------|
| Feature comparison table vs. Duolingo | Medium | Shows differentiation; effective for informed users |
| Leaderboard teaser (live top-5 from Supabase) | Medium | Shows live data; impressive for academic demo |
| About section (project context) | Low | "Built as a graduation project at HCMUS" — academic authenticity |
| Language toggle (EN/VI) | Low | Landing page should have both; users may be Vietnamese speakers checking for a family member |

### Anti-Patterns for Landing Pages

- **Email sign-up form on landing page**: Users are going to Play Store, not subscribing to a newsletter. Don't add friction.
- **Auto-play video with sound**: Instant back button.
- **Long feature list (10+ items)**: Pick 3-4 best and go deep on those.
- **No mobile-optimised landing**: Most visitors arrive from a phone — if landing page looks bad on mobile, they never tap the Play Store link.

---

## Anti-Features

> Things to **deliberately not build** for this milestone. Doing them would waste time or actively harm the product.

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| Paid subscription / streak shields for purchase | Academic project; adds no learning value; legal complexity for store submission | Offer streak freeze as free weekly reward |
| Social sharing of scores to external platforms | Complex OAuth integrations, low usage in language apps | Simple in-app activity feed is sufficient |
| Chat / messaging between users | Moderation nightmare; not a language learning differentiator | Activity feed + leaderboard comparisons cover the social need |
| Custom user-created vocabulary sets | High complexity; content quality control issues; out of scope | Admin-managed vocabulary library is the right model for this stage |
| Audio / pronunciation features | Would require recording studio or TTS API budget; out of PROJECT.md scope | Text-based games + AI writing covers core learning |
| Comprehensive offline sync conflict resolution | PROJECT.md explicitly descoped this | Last-write-wins with max-value for streak |
| iOS version | Out of scope per PROJECT.md | — |
| In-app purchases / monetisation | Academic project | — |
| Admin user impersonation | Security surface too large for demo; Supabase RLS complexity | View user profile (read-only) is sufficient |
| D1/D7/D30 full cohort retention analytics | Complex SQL for demo; won't have enough users to make it meaningful | Simple DAU/MAU chart + streak distribution histogram |
| Real-time collaborative game modes | Very high complexity; no clear language-learning benefit over solo play | Solo games with social leaderboard is the proven model |
| Notifications more frequent than 2/day | Causes uninstalls; violates user trust | Hard cap: 2 push notifications per day max |

---

## Feature Dependencies

```
Guest mode / "Try first" ──────────────────────────────────────────────────────┐
                                                                                │
Supabase Auth (email + Google OAuth) ─────────────────────────────────────────┤
                           │                                                    │
                           ▼                                                    ▼
             Cloud sync (ELO, streak, vocab_progress) ◄──── Onboarding flow (goal + level)
                           │
                           ├─────────────────────────────────────────────────────┐
                           ▼                                                     ▼
              Streak system (server-authoritative) ──► Streak freeze         Real-time leaderboard
                           │                                                     │
                           ▼                                                     ▼
              Daily challenge (server-generated) ──────────────────► Friends leaderboard tab
                           │                                                     │
                           ▼                                                     ▼
              Push notifications ◄──────────────────────────── Social follow system
                                                                                 │
                                                                                 ▼
                                                                      Activity feed
```

**Critical path:** Supabase Auth → Cloud Sync → Streak → Daily Challenge → Notifications → Social

**Can be built in parallel:**
- Admin Dashboard (independent web app; needs Supabase tables but not auth flow)
- Landing Page (purely static/SSG; only needs Play Store link)
- Onboarding UI (local-first, integrates with auth at the end)

---

## MVP Recommendation

> Minimum feature set that makes VietForces feel complete and demo-worthy as a graduation project.

**Prioritise (must demo):**
1. **Google OAuth + email login** — trust baseline, takes 1 day with Supabase
2. **Onboarding flow** (4 screens: Welcome → Goal → Level → Name) — first impression
3. **Streak system** (counter + calendar + streak-danger push notification) — single highest-retention feature
4. **Daily challenge** (server-generated, 1/day, countdown timer, bonus ELO) — reason to return daily
5. **Real-time leaderboard** (global top 100 + user's own rank) — showable in demo
6. **Cloud sync** (ELO + streak survive reinstall) — de-risk demo day
7. **Admin vocabulary CRUD** — shows full-stack; usable by demo evaluators
8. **Landing page** (hero + screenshots + Play Store link) — professional presentation

**Defer (ship if time permits):**
- Follow/friends system and activity feed — implement after core retention loop is solid
- Streak freeze — good to have but not demo-critical
- Admin analytics beyond DAU — complex SQL for small user base
- Achievement badge system — nice but tertiary to core loop
- AI-personalised daily challenge — high value but complex; use random rotation first

---

## Sources

| Source | Confidence | Notes |
|--------|-----------|-------|
| Duolingo public blog + retention research case studies | HIGH | Published by Duolingo Research team; streaks, notifications, leaderboards |
| Babbel / Memrise feature audits (public product) | HIGH | Direct product analysis |
| Google Play Store language-learning category analysis | HIGH | Feature set common across top-10 apps |
| Supabase Auth documentation patterns | HIGH | Official provider; auth patterns well-documented |
| PROJECT.md + ARCHITECTURE.md (VietForces codebase) | HIGH | Authoritative source for existing features and constraints |
| Android push notification (FCM) best practices | HIGH | Google developer documentation |
| Conversion rate optimisation research for app landing pages | MEDIUM | Multiple sources; patterns consistent across studies |
