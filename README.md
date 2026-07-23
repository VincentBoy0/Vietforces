# VietForces 🇻🇳

> Ứng dụng học tiếng Việt qua trò chơi — Đồ án tốt nghiệp HCMUS

**Android app** · **Supabase backend** · **Next.js admin dashboard** · **Landing page**

---

## Mục lục

1. [Yêu cầu hệ thống](#1-yêu-cầu-hệ-thống)
2. [Cài đặt Android app (Android Studio)](#2-cài-đặt-android-app-android-studio)
3. [Cài đặt Supabase backend](#3-cài-đặt-supabase-backend)
4. [Cài đặt Web Admin Dashboard](#4-cài-đặt-web-admin-dashboard)
5. [Cài đặt Landing Page](#5-cài-đặt-landing-page)
6. [Cấu trúc thư mục](#6-cấu-trúc-thư-mục)
7. [Biến môi trường](#7-biến-môi-trường)
8. [Tính năng chính](#8-tính-năng-chính)

---

## 1. Yêu cầu hệ thống

| Công cụ | Phiên bản tối thiểu | Ghi chú |
|---|---|---|
| **Android Studio** | Ladybug (2024.2.1) trở lên | Khuyến nghị Meerkat (2024.3.2) |
| **JDK** | 17 hoặc 21 | **KHÔNG dùng JDK 22+** (Gradle 8.x không tương thích) |
| **Android SDK** | API 24 (Android 7.0) | compileSdk = 36 |
| **Node.js** | 20+ | Cho web-admin và web-landing |
| **Supabase CLI** | 2.x | `npm install -g supabase` |
| **Git** | 2.x | — |

---

## 2. Cài đặt Android app (Android Studio)

### Bước 1 — Clone repository

```bash
git clone https://github.com/tranthebaobnnn-ai/vietforces.git
cd vietforces
```

### Bước 2 — Mở project trong Android Studio

1. Mở **Android Studio**
2. Chọn **File → Open** (hoặc **Open** ở màn hình chào)
3. Chọn thư mục `vietforces/` (thư mục gốc chứa `build.gradle.kts`)
4. Đợi Gradle sync hoàn tất (lần đầu mất 3–5 phút)

> ⚠️ **Lỗi JDK thường gặp:** Nếu thấy lỗi `Unsupported class file major version`, vào  
> **File → Settings → Build, Execution, Deployment → Build Tools → Gradle**  
> và đổi **Gradle JDK** thành **JDK 17** hoặc **JDK 21**.

### Bước 3 — Tạo file `local.properties`

File này **không được commit** (đã có trong `.gitignore`). Tạo thủ công tại thư mục gốc:

```properties
# local.properties — KHÔNG commit file này lên Git

# Đường dẫn Android SDK (Android Studio tự điền, chỉ cần kiểm tra)
sdk.dir=/path/to/your/Android/Sdk

# Supabase (lấy từ https://supabase.com/dashboard → Project Settings → API)
SUPABASE_URL=https://your-project-id.supabase.co
SUPABASE_ANON_KEY=your-anon-key-here

# OpenAI — tuỳ chọn, để trống nếu dùng Supabase Edge Function proxy
OPENAI_API_KEY=
```

> 💡 Nếu chưa có Supabase project, xem [Bước 3 — Cài đặt Supabase](#3-cài-đặt-supabase-backend) trước.

### Bước 4 — Chạy ứng dụng

1. Kết nối thiết bị Android (API 24+) hoặc khởi động **Android Emulator**
2. Trong Android Studio chọn thiết bị ở thanh toolbar
3. Nhấn nút **▶ Run** (Shift+F10) hoặc chọn **Run → Run 'app'**

### Bước 5 (tuỳ chọn) — Cấu hình Firebase cho Push Notifications

Nếu muốn dùng tính năng thông báo (NOTIF-01 đến NOTIF-05):

1. Tạo project trên [Firebase Console](https://console.firebase.google.com)
2. Thêm app Android với package name `com.vietforces.app`
3. Tải file `google-services.json` và đặt vào thư mục `app/`
4. Trong `app/build.gradle.kts`, bỏ comment dòng:
   ```kotlin
   // apply plugin: "com.google.gms.google-services"
   ```
5. Sync Gradle và chạy lại

---

## 3. Cài đặt Supabase backend

### Bước 1 — Cài Supabase CLI

```bash
npm install -g supabase
supabase --version  # kiểm tra: phải >= 2.x
```

### Bước 2 — Tạo Supabase project

1. Đăng ký tài khoản tại [supabase.com](https://supabase.com)
2. Tạo **New Project** (chọn region gần nhất — Singapore)
3. Lưu lại:
   - **Project URL**: `https://xxxxxxxxxxxx.supabase.co`
   - **Anon public key** (ở Project Settings → API)
   - **Service role key** (dùng cho admin web — **giữ bí mật**)

### Bước 3 — Áp dụng migrations

```bash
# Đăng nhập Supabase CLI
supabase login

# Liên kết với project
supabase link --project-ref your-project-id

# Chạy tất cả 10 migrations
supabase db push
```

Migrations bao gồm:
- `001` — Schema chính (6 bảng: users, user_progress, leaderboard, daily_challenges, friendships, fcm_tokens)
- `002–006` — Functions: `calculate_elo()`, `update_streak()`, `award_daily_bonus()`
- `007–010` — Activity feed, admin schema, security fixes, notification preferences

### Bước 4 — Deploy Edge Functions

```bash
# Deploy tất cả Edge Functions
supabase functions deploy openai-proxy
supabase functions deploy generate-daily-challenge
supabase functions deploy send-streak-reminder
supabase functions deploy refresh-streak-freeze
```

### Bước 5 — Cài đặt secrets cho Edge Functions

```bash
# OpenAI API key (dùng cho openai-proxy)
supabase secrets set OPENAI_API_KEY=sk-...

# Bảo vệ cron webhook
supabase secrets set CRON_SECRET=$(openssl rand -hex 32)

# Firebase (dùng cho send-streak-reminder)
supabase secrets set FIREBASE_PROJECT_ID=your-firebase-project-id
supabase secrets set FIREBASE_SERVICE_ACCOUNT_JSON="$(cat path/to/serviceAccount.json)"
```

### Bước 6 — Tạo tài khoản admin

Sau khi tạo tài khoản bằng app hoặc Supabase Auth:

```sql
-- Chạy trong Supabase Dashboard → SQL Editor
UPDATE public.users
SET is_admin = true
WHERE id = (SELECT id FROM auth.users WHERE email = 'your-admin@email.com');
```

---

## 4. Cài đặt Web Admin Dashboard

### Yêu cầu: Node.js 20+

```bash
# Vào thư mục web-admin
cd web-admin

# Cài dependencies
npm install

# Tạo file .env.local
cp .env.example .env.local  # hoặc tạo thủ công (xem bên dưới)
```

Tạo file `web-admin/.env.local`:

```env
# web-admin/.env.local — KHÔNG commit file này

NEXT_PUBLIC_SUPABASE_URL=https://your-project-id.supabase.co
NEXT_PUBLIC_SUPABASE_ANON_KEY=your-anon-key

# Service role key — chỉ dùng server-side, KHÔNG dùng NEXT_PUBLIC_
SUPABASE_SERVICE_ROLE_KEY=your-service-role-key
```

### Chạy development server

```bash
npm run dev
# Mở http://localhost:3000
```

### Build production

```bash
npm run build
npm start
```

### Deploy lên Vercel

1. Push code lên GitHub
2. Vào [vercel.com](https://vercel.com) → **Add New Project** → chọn repo
3. Đặt **Root Directory** là `web-admin`
4. Thêm các biến môi trường (từ `.env.local`) trong Vercel dashboard
5. Deploy

---

## 5. Cài đặt Landing Page

```bash
cd web-landing

npm install
npm run dev
# Mở http://localhost:3001
```

### Deploy lên Vercel (static export)

```bash
npm run build
# Thư mục out/ chứa HTML tĩnh, upload lên bất kỳ hosting nào
```

Hoặc deploy tự động qua Vercel (đặt Root Directory là `web-landing`).

---

## 6. Cấu trúc thư mục

```
vietforces/
├── app/                          # Android app (Kotlin + Jetpack Compose)
│   └── src/main/java/com/example/vietforces/
│       ├── data/
│       │   ├── manager/          # UserProgressManager, NotificationManager, FCMTokenManager
│       │   ├── model/            # Data classes, DTOs
│       │   ├── remote/           # Supabase data sources
│       │   ├── repository/       # Auth, ELO, Streak, Leaderboard, Social, Daily...
│       │   ├── service/          # MigrationService
│       │   ├── storage/          # PreferencesManager (SharedPreferences)
│       │   └── worker/           # StreakDangerWorker (WorkManager)
│       ├── di/                   # Hilt modules
│       ├── navigation/           # Screen routes
│       ├── ui/
│       │   ├── components/       # UiComponents, BottomNavigation, StreakHeatmap
│       │   ├── screens/          # Tất cả màn hình
│       │   ├── theme/            # Color, Theme (VietRed, VietYellow)
│       │   └── viewmodel/        # ViewModels
│       ├── MainActivity.kt
│       └── VietForcesApplication.kt
│
├── supabase/
│   ├── migrations/               # 10 file SQL migration
│   ├── functions/                # 4 Deno Edge Functions
│   ├── config.toml
│   └── README.md
│
├── web-admin/                    # Next.js 15 Admin Dashboard
│   └── src/
│       ├── app/admin/            # vocabulary, users, analytics, daily-challenges
│       ├── lib/
│       │   ├── actions/          # Server Actions
│       │   └── supabase/         # client, server, admin clients
│       └── middleware.ts         # Auth guard
│
├── web-landing/                  # Next.js Landing Page (static export)
│   └── src/app/
│       └── page.tsx              # Toàn bộ landing page
│
├── gradle/
│   └── libs.versions.toml        # Version catalog
├── build.gradle.kts              # Root Gradle config
├── app/build.gradle.kts          # App module config
└── local.properties              # 🔒 Secrets (git-ignored)
```

---

## 7. Biến môi trường

### Android (`local.properties` — git-ignored)

| Biến | Bắt buộc | Mô tả |
|---|---|---|
| `sdk.dir` | ✅ | Đường dẫn Android SDK (Android Studio tự điền) |
| `SUPABASE_URL` | ✅ | URL Supabase project |
| `SUPABASE_ANON_KEY` | ✅ | Anon/public key |
| `OPENAI_API_KEY` | ❌ | Để trống — AI calls đi qua Edge Function proxy |

### Web Admin (`web-admin/.env.local` — git-ignored)

| Biến | Mô tả |
|---|---|
| `NEXT_PUBLIC_SUPABASE_URL` | URL Supabase project |
| `NEXT_PUBLIC_SUPABASE_ANON_KEY` | Anon key |
| `SUPABASE_SERVICE_ROLE_KEY` | Service role key (server-only) |

### Supabase Edge Function Secrets

| Secret | Dùng cho |
|---|---|
| `OPENAI_API_KEY` | `openai-proxy` function |
| `CRON_SECRET` | Bảo vệ cron webhook |
| `FIREBASE_PROJECT_ID` | `send-streak-reminder` function |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | FCM HTTP v1 API auth |

---

## 8. Tính năng chính

| Tính năng | Mô tả |
|---|---|
| 🎮 **5 Game Modes** | Hình ảnh → Từ, Từ → Nghĩa, Điền từ, Sắp xếp câu, Word Chain |
| 🤖 **AI Hints** | GPT-4.1-mini qua Supabase Edge Function proxy (key không trong APK) |
| 🔐 **Auth** | Email/password + Google OAuth (Supabase GoTrue) |
| 👤 **Onboarding** | 4-bước HorizontalPager, guest mode, tạo tài khoản sau |
| ☁️ **Cloud Sync** | Tiến độ học sync sau mỗi game session, last-write-wins |
| ⚡ **ELO System** | Server-side SECURITY DEFINER RPC, dynamic K-factor |
| 🔥 **Streak** | Server-authoritative, auto-freeze 1/tuần, 28-day heatmap |
| 🏆 **Leaderboard** | Real-time qua Supabase Realtime WebSocket, All-time + Tuần này + Bạn bè |
| 📅 **Daily Challenge** | Server-generated lúc 00:00 UTC, countdown, +50 ELO bonus |
| 🔔 **Notifications** | FCM push (streak danger, daily challenge), Android 13+ permission |
| 👥 **Social** | Follow/unfollow, tìm bạn bè, public profiles, activity feed |
| 🌐 **Web Admin** | Vocabulary CRUD, user management, analytics, daily challenge scheduler |
| 📄 **Landing Page** | Mobile-first marketing page, deploy Vercel |

---

## Lỗi thường gặp

### `Unsupported class file major version`
→ JDK quá mới. Đổi sang JDK 17 hoặc 21 trong Android Studio.

### `SUPABASE_URL not found`
→ Chưa tạo `local.properties` hoặc thiếu key. Xem [Bước 3 phần 2](#bước-3--tạo-file-localproperties).

### `Hilt component not found`
→ `VietForcesApplication` chưa được đăng ký trong `AndroidManifest.xml`. Kiểm tra `android:name=".VietForcesApplication"`.

### Supabase `relation "user_progress" does not exist`
→ Chưa chạy migrations. Thực hiện `supabase db push`.

### Web admin trắng sau login
→ User chưa có `is_admin = true`. Chạy câu SQL ở [Bước 6](#bước-6--tạo-tài-khoản-admin).

---

## Kiến trúc

```
Android App  ──────────────────▶  Supabase (PostgreSQL + RLS)
     │                                    │
     │  Supabase-kt 3.7.0                 │  Edge Functions (Deno)
     │  Hilt 2.51.1                       │  ├── openai-proxy
     │  Jetpack Compose                   │  ├── generate-daily-challenge
     │  WorkManager                       │  ├── send-streak-reminder
     │                                    │  └── refresh-streak-freeze
     │
     └── FCM (Firebase Cloud Messaging)

Web Admin  ────────────────────▶  Supabase (Service Role)
Next.js 15 App Router
Tailwind v4 · @supabase/ssr

Landing Page  ─────────────────▶  Vercel (Static)
Next.js 15 Static Export
```

---

*Đồ án tốt nghiệp — Trường Đại học Khoa học Tự nhiên TP.HCM (HCMUS)*  
*© 2025 VietForces*
