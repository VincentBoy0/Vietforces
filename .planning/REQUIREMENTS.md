# Requirements: VietForces

**Defined:** 2026-07-22
**Core Value:** Người dùng học tiếng Việt hiệu quả qua gameplay thú vị — mỗi phiên chơi phải cảm thấy có tiến bộ rõ ràng và muốn quay lại ngày hôm sau.

## v1 Requirements

### Pre-work & Foundation

- [ ] **PRE-01**: App dùng `Locale.ROOT` (thay vì `Locale.getDefault()`) cho tất cả date/time formatting để tránh streak bug trên device không phải tiếng Anh
- [ ] **PRE-02**: `applicationId` được đổi từ `com.example.vietforces` sang package name phù hợp trước khi tích hợp Firebase/FCM
- [ ] **FOUND-01**: Supabase project được khởi tạo với schema đầy đủ (6 tables: `users`, `user_progress`, `leaderboard`, `daily_challenges`, `friendships`, `fcm_tokens`) và RLS policies cho mọi table
- [ ] **FOUND-02**: Singleton `SupabaseClient` được khởi tạo qua Hilt trong `Application` class, inject được vào mọi Repository
- [ ] **FOUND-03**: OpenAI API key được chuyển khỏi APK — AI calls đi qua Supabase Edge Function proxy (key chỉ tồn tại trong Supabase env vars)
- [ ] **FOUND-04**: Hilt DI framework được tích hợp, thay thế manual instantiation trong Managers
- [ ] **FOUND-05**: Supabase free tier keepalive cron được cấu hình để project không bị pause sau 7 ngày inactive

### Authentication & Onboarding

- [ ] **AUTH-01**: User có thể đăng ký tài khoản bằng email và password
- [ ] **AUTH-02**: User có thể đăng nhập bằng Google OAuth (one-tap)
- [ ] **AUTH-03**: User có thể đăng nhập bằng email/password và session tồn tại xuyên suốt app restart
- [ ] **AUTH-04**: User có thể đăng xuất từ Settings screen
- [ ] **AUTH-05**: User có thể reset password qua email link
- [x] **ONBOARD-01**: User mới thấy onboarding flow 4 màn hình (Welcome → Chọn level → Chọn goal/day → Nhập tên/avatar) trước khi yêu cầu đăng ký
- [x] **ONBOARD-02**: User có thể chơi guest mode (ít nhất 1 game mode hoàn chỉnh) trước khi tạo tài khoản
- [ ] **ONBOARD-03**: Khi guest đăng ký, toàn bộ tiến độ local (ELO, streak, từ đã học) được migrate lên Supabase
- [ ] **SYNC-01**: Tiến độ học (ELO, streak, từ đã gặp, last_practiced) được sync lên Supabase sau mỗi game session kết thúc
- [ ] **SYNC-02**: App load tiến độ từ Supabase khi mở lần đầu sau khi đăng nhập (overwrite local nếu cloud mới hơn)

### Streak & Gamification

- [ ] **STREAK-01**: Streak counter được tính server-side (Supabase) dựa trên timezone của user, không phải device local time
- [ ] **STREAK-02**: User được cảnh báo "streak in danger" khi còn 2 tiếng trước nửa đêm mà chưa học hôm đó
- [ ] **STREAK-03**: User được tặng 1 "streak freeze" miễn phí mỗi tuần, tự động áp dụng khi bỏ 1 ngày (tối đa 1 freeze tồn đọng)
- [ ] **STREAK-04**: Streak calendar heatmap hiển thị trong Profile screen (7 ngày gần nhất tối thiểu)
- [ ] **ELO-01**: ELO score sau mỗi game được tính bởi Supabase PostgreSQL function (`SECURITY DEFINER`) — client chỉ gửi game metrics, không gửi ELO delta
- [ ] **ELO-02**: ELO ranking tiers (Bronze/Silver/Gold/Platinum/Diamond) hiển thị đúng với ELO server-side

### Leaderboard (Real-time)

- [ ] **LEAD-01**: Leaderboard screen hiển thị top 50 players (global) với ELO score real-time qua Supabase Realtime subscription
- [ ] **LEAD-02**: User thấy vị trí của bản thân trong leaderboard ngay cả khi không thuộc top 50
- [ ] **LEAD-03**: Leaderboard có tab "Tuần này" và "All-time" (filter server-side)
- [ ] **LEAD-04**: Leaderboard subscription được lifecycle-aware (cancel trong `onCleared()` của ViewModel)

### Daily Challenge

- [ ] **DAILY-01**: Mỗi ngày có 1 daily challenge mới (server-generated bởi pg_cron Edge Function lúc 00:00 UTC)
- [ ] **DAILY-02**: Daily challenge screen có countdown đến khi challenge kết thúc (midnight UTC)
- [ ] **DAILY-03**: Hoàn thành daily challenge tặng bonus ELO (+50) và credit 1 ngày streak dù chưa chơi game mode khác
- [ ] **DAILY-04**: User thấy history 7 ngày gần nhất của daily challenges (completed/missed/upcoming)

### Push Notifications

- [ ] **NOTIF-01**: App đăng ký FCM token và lưu vào `users.fcm_token` trong Supabase khi user đăng nhập
- [ ] **NOTIF-02**: Supabase Edge Function gửi FCM push "Streak in danger!" (~19:00 UTC hoặc dựa theo timezone user) nếu user chưa học hôm đó
- [ ] **NOTIF-03**: Notification đưa user thẳng vào Daily Challenge screen khi tap
- [ ] **NOTIF-04**: User có thể bật/tắt từng loại notification trong Settings screen
- [ ] **NOTIF-05**: App xử lý đúng Android 13+ notification permission request (POST_NOTIFICATIONS)

### Social / Friends

- [ ] **SOCIAL-01**: User có thể search và follow bạn bè theo username
- [ ] **SOCIAL-02**: Friends tab trong Leaderboard chỉ hiển thị ranking của những người user đang follow (asymmetric follow)
- [ ] **SOCIAL-03**: Profile screen của user khác có thể được xem với streak, ELO, và game stats (read-only)
- [ ] **SOCIAL-04**: Activity feed đơn giản: xem bạn bè vừa hoàn thành daily challenge hoặc đạt ELO milestone

### UX Polish (Android)

- [ ] **UX-01**: Mọi màn hình có empty state có thiết kế (illustration + message + CTA) thay vì blank screen
- [ ] **UX-02**: Mọi network request hiển thị skeleton loading hoặc shimmer thay vì spinner trắng
- [ ] **UX-03**: Loading state cho game modes khi fetch vocabulary từ remote
- [ ] **UX-04**: Error states với retry button cho network failures
- [ ] **UX-05**: Dark mode support (theme đã có sẵn, cần wire với system setting)

### Web Admin Dashboard

- [ ] **ADMIN-01**: Admin dashboard được deploy trên Vercel với Next.js 15 App Router, bảo vệ bởi Supabase Auth (service-role key chỉ dùng server-side)
- [ ] **ADMIN-02**: Admin có thể xem danh sách vocabulary items với filter theo category
- [ ] **ADMIN-03**: Admin có thể thêm vocabulary item mới (word, classifier, category, image upload lên Supabase Storage, distractors)
- [ ] **ADMIN-04**: Admin có thể sửa và xóa vocabulary items
- [ ] **ADMIN-05**: Admin có thể xem danh sách users với ELO, streak, last_active
- [ ] **ADMIN-06**: Admin có thể soft-ban user (set `is_banned=true` → app show banned message khi login)
- [ ] **ADMIN-07**: Analytics dashboard hiển thị: DAU 30 ngày, top game modes, average session length (từ Supabase queries)
- [ ] **ADMIN-08**: Admin có thể tạo và lên lịch daily challenge thủ công (override pg_cron auto-generation)

### Landing Page

- [ ] **LAND-01**: Landing page được deploy trên Vercel, responsive mobile-first
- [ ] **LAND-02**: Hero section với app name, tagline ("Học tiếng Việt qua trò chơi"), và app screenshot mockup
- [ ] **LAND-03**: Features section giới thiệu 4-6 tính năng nổi bật (game modes, AI, leaderboard, daily challenge)
- [ ] **LAND-04**: Download CTA (Google Play badge hoặc APK download link)
- [ ] **LAND-05**: Screenshots/video demo section

## v2 Requirements

Deferred to future release.

### Nâng cao

- **V2-01**: Head-to-head real-time challenge (chơi cùng bạn bè đồng thời)
- **V2-02**: iOS version
- **V2-03**: Offline-first với conflict resolution phức tạp (hơn last-write-wins)
- **V2-04**: In-app purchases / monetization
- **V2-05**: Spaced repetition algorithm server-side (SRS thay vì local weights)
- **V2-06**: Community vocabulary contributions (user-submitted words)
- **V2-07**: Teacher/classroom mode
- **V2-08**: 2FA authentication
- **V2-09**: Magic link login
- **V2-10**: Full cohort analytics và A/B testing

## Out of Scope

| Feature | Reason |
|---------|--------|
| iOS version | Ngoài phạm vi đồ án |
| Monetization / in-app purchases | Đồ án học thuật |
| Complex offline sync conflict resolution | last-write-wins đủ dùng theo PROJECT.md |
| Self-hosted backend (non-Supabase) | Quyết định đã chốt: Supabase |
| Full ProGuard/R8 obfuscation | Không cần thiết cho demo |
| Play Store publication | Không bắt buộc cho đồ án (APK sideload đủ) |

## Traceability

*Updated by roadmapper agent: 2026-07-22*

| Requirement | Phase | Status |
|-------------|-------|--------|
| PRE-01 | Phase 0 — Pre-work Fixes | Pending |
| PRE-02 | Phase 0 — Pre-work Fixes | Pending |
| FOUND-01 | Phase 1 — Supabase Foundation | Pending |
| FOUND-02 | Phase 1 — Supabase Foundation | Pending |
| FOUND-03 | Phase 1 — Supabase Foundation | Pending |
| FOUND-04 | Phase 1 — Supabase Foundation | Pending |
| FOUND-05 | Phase 1 — Supabase Foundation | Pending |
| AUTH-01 | Phase 2 — Auth + Onboarding + Sync + UX | Pending |
| AUTH-02 | Phase 2 — Auth + Onboarding + Sync + UX | Pending |
| AUTH-03 | Phase 2 — Auth + Onboarding + Sync + UX | Pending |
| AUTH-04 | Phase 2 — Auth + Onboarding + Sync + UX | Pending |
| AUTH-05 | Phase 2 — Auth + Onboarding + Sync + UX | Pending |
| ONBOARD-01 | Phase 2 — Auth + Onboarding + Sync + UX | Complete |
| ONBOARD-02 | Phase 2 — Auth + Onboarding + Sync + UX | Complete |
| ONBOARD-03 | Phase 2 — Auth + Onboarding + Sync + UX | Pending |
| SYNC-01 | Phase 2 — Auth + Onboarding + Sync + UX | Pending |
| SYNC-02 | Phase 2 — Auth + Onboarding + Sync + UX | Pending |
| UX-01 | Phase 2 — Auth + Onboarding + Sync + UX | Pending |
| UX-02 | Phase 2 — Auth + Onboarding + Sync + UX | Pending |
| UX-03 | Phase 2 — Auth + Onboarding + Sync + UX | Pending |
| UX-04 | Phase 2 — Auth + Onboarding + Sync + UX | Pending |
| UX-05 | Phase 2 — Auth + Onboarding + Sync + UX | Pending |
| STREAK-01 | Phase 3 — Streak + Leaderboard | Pending |
| STREAK-02 | Phase 3 — Streak + Leaderboard | Pending |
| STREAK-03 | Phase 3 — Streak + Leaderboard | Pending |
| STREAK-04 | Phase 3 — Streak + Leaderboard | Pending |
| ELO-01 | Phase 3 — Streak + Leaderboard | Pending |
| ELO-02 | Phase 3 — Streak + Leaderboard | Pending |
| LEAD-01 | Phase 3 — Streak + Leaderboard | Pending |
| LEAD-02 | Phase 3 — Streak + Leaderboard | Pending |
| LEAD-03 | Phase 3 — Streak + Leaderboard | Pending |
| LEAD-04 | Phase 3 — Streak + Leaderboard | Pending |
| DAILY-01 | Phase 4 — Daily Challenge + Notifications | Pending |
| DAILY-02 | Phase 4 — Daily Challenge + Notifications | Pending |
| DAILY-03 | Phase 4 — Daily Challenge + Notifications | Pending |
| DAILY-04 | Phase 4 — Daily Challenge + Notifications | Pending |
| NOTIF-01 | Phase 4 — Daily Challenge + Notifications | Pending |
| NOTIF-02 | Phase 4 — Daily Challenge + Notifications | Pending |
| NOTIF-03 | Phase 4 — Daily Challenge + Notifications | Pending |
| NOTIF-04 | Phase 4 — Daily Challenge + Notifications | Pending |
| NOTIF-05 | Phase 4 — Daily Challenge + Notifications | Pending |
| SOCIAL-01 | Phase 5 — Social / Friends | Pending |
| SOCIAL-02 | Phase 5 — Social / Friends | Pending |
| SOCIAL-03 | Phase 5 — Social / Friends | Pending |
| SOCIAL-04 | Phase 5 — Social / Friends | Pending |
| ADMIN-01 | Phase 6 — Web Admin Dashboard | Pending |
| ADMIN-02 | Phase 6 — Web Admin Dashboard | Pending |
| ADMIN-03 | Phase 6 — Web Admin Dashboard | Pending |
| ADMIN-04 | Phase 6 — Web Admin Dashboard | Pending |
| ADMIN-05 | Phase 6 — Web Admin Dashboard | Pending |
| ADMIN-06 | Phase 6 — Web Admin Dashboard | Pending |
| ADMIN-07 | Phase 6 — Web Admin Dashboard | Pending |
| ADMIN-08 | Phase 6 — Web Admin Dashboard | Pending |
| LAND-01 | Phase 7 — Landing Page | Pending |
| LAND-02 | Phase 7 — Landing Page | Pending |
| LAND-03 | Phase 7 — Landing Page | Pending |
| LAND-04 | Phase 7 — Landing Page | Pending |
| LAND-05 | Phase 7 — Landing Page | Pending |

**Coverage:**

- v1 requirements: 58 enumerated IDs
- Mapped to phases: 58
- Unmapped: 0 ✓

*(Note: original doc header stated 56; actual enumerated IDs count to 58 — all are mapped above.)*

---
*Requirements defined: 2026-07-22*
*Last updated: 2026-07-22 after initialization*
