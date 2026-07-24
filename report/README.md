# Báo cáo Đồ án VietForces — LaTeX

## Cấu trúc

```
report/
├── main.tex          # Báo cáo chính (tất cả mục theo yêu cầu thầy)
├── brochure.tex      # Tờ rơi A4 bắt buộc nộp (1 trang)
├── images/
│   ├── README.md     # Danh sách screenshot cần chụp
│   └── *.png / *.jpg # Đặt ảnh chụp màn hình vào đây
└── README.md         # File này
```

## Nội dung `main.tex`

| Mục | Nội dung |
|-----|---------|
| §1  | Problem Statement (bài toán, phạm vi, đối tượng) |
| §2  | Use Case Model (sơ đồ TikZ + bảng mô tả + 8 game modes) |
| §3  | Glossary (ELO, Streak, RLS, Edge Function, v.v.) |
| §4  | Supplementary Specification (yêu cầu phi chức năng) |
| §5  | Class Diagram (TikZ domain model + MVVM layer diagram) |
| §6  | Software Architecture (hệ thống tổng thể + Supabase schema) |
| §7  | AI Integration ⭐ (5 tính năng AI, kiến trúc, bảo mật) |
| §8  | UI Screenshots (placeholder — thêm ảnh thực vào images/) |
| §9  | Video Demo (kịch bản demo) |
| §10 | Tính năng nổi bật (Advanced Features) |

## Nội dung `brochure.tex`

Tờ rơi A4 1 trang thiết kế màu sắc:
- Mục tiêu đề tài
- Tech Stack
- 8 Game Modes
- AI Features (5 tính năng)
- Gamification (ELO, Streak, Daily Challenge, Leaderboard)
- Security & Quality
- Social Features
- Web Admin Dashboard
- Kiến trúc mini-diagram
- Footer

## Cách dùng trên Overleaf

1. Vào [overleaf.com](https://www.overleaf.com) → New Project → Upload
2. Upload toàn bộ thư mục `report/`
3. Menu → Compiler → **XeLaTeX** (quan trọng cho tiếng Việt đẹp nhất)
4. Compile `main.tex` cho báo cáo
5. Compile `brochure.tex` cho tờ rơi

## Thêm screenshot thực tế

1. Chụp màn hình app (xem `images/README.md` để biết tên file cần đặt)
2. Đặt file ảnh vào `report/images/`
3. Trong `main.tex` mục §8, bỏ comment các dòng `% \includegraphics{...}`
4. Xoá hoặc comment các dòng `\fbox{...}` placeholder tương ứng

## Compile trên máy local (nếu có TeX)

```bash
cd report
xelatex main.tex && xelatex main.tex  # chạy 2 lần cho TOC
xelatex brochure.tex
```

Hoặc dùng latexmk:
```bash
latexmk -xelatex main.tex
latexmk -xelatex brochure.tex
```
