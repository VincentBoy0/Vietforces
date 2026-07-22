# Phase 7 Context: Landing Page

## Goal
Build a mobile-first Vietnamese landing page for VietForces deployed on Vercel. Single Next.js app (can share the web-admin repo or be a separate `web-landing/` directory — use separate for clean separation).

## Requirements
LAND-01, LAND-02, LAND-03, LAND-04, LAND-05

## Tech Stack
- Next.js 15.3.9 (App Router, static export or SSG)
- Tailwind CSS v4 (same as web-admin)
- No auth needed — fully public
- Deploy to Vercel (separate project from web-admin)

## Project Location
`/web-landing/` at repo root

## Page Structure

### Hero Section (LAND-02)
- Full-height section with gradient background (VietRed → dark)
- App icon + "VietForces" headline (bold, large)
- Tagline: "Học tiếng Việt qua trò chơi 🇻🇳"
- Subheadline: "Gamified Vietnamese vocabulary learning with AI, leaderboards, and daily challenges"
- Phone mockup: CSS-styled mockup frame containing app screenshot placeholder (gradient card)
- Primary CTA: "Tải về ngay" button → Google Play link (placeholder #)
- Secondary CTA: "Xem tính năng" anchor → #features

### Features Section (LAND-03)
- Section id="features"
- Grid of 6 feature cards (icon emoji + title + description):
  1. 🎮 "5 Game Modes" — Hình ảnh → Từ, Từ → Nghĩa, Phát âm, Flashcard, Thách đấu hàng ngày
  2. 🤖 "AI-Powered Hints" — GPT-4o tạo gợi ý thông minh, giải thích từ trong ngữ cảnh
  3. 🏆 "Real-time Leaderboard" — Xếp hạng ELO thực thời, cạnh tranh với bạn bè toàn cầu
  4. 🔥 "Streak System" — Duy trì chuỗi ngày học, tự động freeze khi bỏ lỡ
  5. 👥 "Social Features" — Follow bạn bè, xem hoạt động, cùng nhau tiến bộ
  6. 📅 "Daily Challenges" — Thách đấu mới mỗi ngày, +50 bonus ELO khi hoàn thành

### Screenshots/Demo Section (LAND-05)
- Horizontal scroll row of 3-4 phone frame mockups showing different screens
- Pure CSS phone frames (no external library)
- Placeholder gradient fills with screen names (Main Screen, Leaderboard, Daily Challenge, Profile)

### Download CTA Section (LAND-04)
- Centered section with: "Sẵn sàng học tiếng Việt?"
- Google Play badge (SVG/PNG placeholder with link to #)
- APK direct download link (placeholder)
- "Miễn phí — Không cần thẻ tín dụng" subtext

### Footer
- App name + tagline
- Links: Privacy Policy, Terms (placeholder hrefs)
- © 2025 VietForces

## Single Plan (Phase 7 is a single page build)
- 07-01-PLAN.md — entire landing page (all 5 requirements, 1 plan)

## No waves needed — single sequential build
