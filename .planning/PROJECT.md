# VietForces

## What This Is

VietForces là ứng dụng Android học từ vựng tiếng Việt dạng gamified, nhắm đến người học nước ngoài và trẻ em. App dùng hình ảnh + nhiều game modes (ImageToWord, WordToImage, SyllableMatch, WordSearch, WordChain, FillBlank, SentenceOrder) kết hợp AI (OpenAI) cho roleplay và writing practice. Dự án đang được hoàn thiện thành sản phẩm đầy đủ với backend Supabase, web admin dashboard, landing page và đầy đủ social features.

## Core Value

Người dùng học tiếng Việt hiệu quả qua gameplay thú vị — mỗi phiên chơi phải cảm thấy có tiến bộ rõ ràng và muốn quay lại ngày hôm sau.

## Requirements

### Validated

- ✓ 8+ game modes hoạt động (ImageToWord, WordToImage, SyllableMatch, WordSearch, FillBlank, WordChain, SentenceOrder) — existing
- ✓ Hệ thống ELO ranking cục bộ — existing
- ✓ AI Roleplay & Writing Practice qua OpenAI API — existing
- ✓ Vocabulary repository với 100+ từ phân loại (animals, food, clothing, household, school, kitchen, body, places) — existing
- ✓ Custom Jetpack Compose theme + Rooster mascot — existing
- ✓ Local progress tracking (SharedPreferences) — existing
- ✓ Settings & Profile screens — existing
- ✓ Bottom navigation 5 tabs — existing

### Active

**Backend & Auth**
- [ ] Supabase Auth (email/password + Google OAuth)
- [ ] Cloud sync tiến độ học (ELO, từ đã học, streak)
- [ ] Real-time leaderboard (ELO toàn cầu)
- [ ] Daily challenge system (server-generated, đổi mỗi ngày)
- [ ] Push notifications thông minh (nhắc học hàng ngày, streak warning)
- [ ] Social: follow bạn bè, so sánh tiến độ
- [ ] Supabase database schema (users, progress, challenges, leaderboard, friendships)

**Android Frontend**
- [ ] Onboarding flow (welcome → chọn level → tạo profile)
- [ ] Empty states cho mọi màn hình (chưa có dữ liệu)
- [ ] Loading states & skeleton screens
- [ ] Online leaderboard screen (real-time)
- [ ] Daily challenge UI (countdown, reward animation)
- [ ] Streak system UI (calendar heatmap, streak counter)
- [ ] Friends/Social screen (follow, compare, invite)
- [ ] Smart notification settings UI
- [ ] Login/Register screens (Supabase Auth)
- [ ] Profile sync với Supabase

**Web Admin Dashboard**
- [ ] Vocabulary CRUD (thêm/sửa/xóa từ vựng + hình ảnh)
- [ ] User management (xem danh sách, ban, reset ELO)
- [ ] Analytics dashboard (DAU, retention, game mode usage)
- [ ] Daily challenge management (tạo/lên lịch)
- [ ] Content moderation (AI conversation logs review)

**Landing Page**
- [ ] Hero section với app showcase
- [ ] Features overview (game modes, AI, gamification)
- [ ] Download links (Google Play / APK)
- [ ] Screenshots/video demo

### Out of Scope

- iOS version — ngoài phạm vi đồ án, chỉ tập trung Android
- Monetization / in-app purchases — đồ án học thuật
- Offline-first với complex sync conflict resolution — dùng last-write-wins đơn giản
- Multi-language UI (chỉ Vietnamese learning, UI tiếng Anh/Việt)

## Context

- **Công nghệ Android hiện tại**: Kotlin + Jetpack Compose, Gradle Kotlin DSL, minSdk 24, compileSdk 36
- **AI**: OpenAI API (gpt-4.1-mini mặc định), key trong local.properties
- **State management**: Hiện dùng SharedPreferences + in-memory managers, cần migrate sang ViewModel + StateFlow
- **Codebase**: 41 Kotlin files, cấu trúc sạch (data/ui/navigation layers), đã có các màn hình placeholder chờ implement
- **Backend mới**: Supabase (PostgreSQL + Auth + Realtime + Storage)
- **Web stack**: React (Next.js) + Tailwind cho Admin Dashboard & Landing page
- **Đối tượng**: Đồ án tốt nghiệp — cần demo hoàn chỉnh, architecture rõ ràng

## Constraints

- **Platform**: Android only (minSdk 24+) cho app chính
- **Backend**: Supabase — không tự host server
- **AI**: OpenAI API — giữ nguyên, không thay thế
- **Timeline**: Đồ án tốt nghiệp — cần hoàn thiện để bảo vệ
- **Codebase**: Giữ nguyên package name `com.example.vietforces`, không refactor căn bản
- **UI**: Giữ Jetpack Compose + theme hiện tại, không redesign toàn bộ

## Key Decisions

| Decision | Rationale | Outcome |
|---|---|---|
| Supabase làm backend | PostgreSQL + Auth + Realtime trong một, free tier đủ cho demo, không cần tự build REST API | — Pending |
| Next.js cho Web Admin + Landing | React ecosystem quen thuộc, SSR tốt cho landing page SEO, deploy Vercel dễ | — Pending |
| Giữ SharedPreferences song song Supabase | Migration dần, offline fallback nếu không có mạng | — Pending |
| ELO ranking online = server-authoritative | Tránh cheating, sync real-time qua Supabase Realtime | — Pending |
| Onboarding trước login | User trải nghiệm app trước khi yêu cầu tạo tài khoản (reduce friction) | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-07-22 after initialization*
