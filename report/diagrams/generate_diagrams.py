#!/usr/bin/env python3
"""
VietForces — Diagram Generator
Generates PNG diagrams for the graduation project report.

Usage:  python3 generate_diagrams.py
Output: usecase.png, class_diagram.png, architecture.png,
        ai_architecture.png, mvvm_layers.png  (all in same folder)
"""

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.patches import FancyBboxPatch, Ellipse, FancyArrow
from matplotlib.lines import Line2D
import numpy as np
import os

OUT = os.path.dirname(os.path.abspath(__file__))
DPI = 150

# ── Colour palette ──────────────────────────────────────────────────
RED    = '#C62828'; LRED    = '#FFEBEE'; MRED    = '#EF9A9A'
BLUE   = '#1565C0'; LBLUE   = '#E3F2FD'; MBLUE   = '#90CAF9'
GREEN  = '#2E7D32'; LGREEN  = '#E8F5E9'; MGREEN  = '#A5D6A7'
ORANGE = '#E65100'; LORANGE = '#FFF3E0'; MORANGE = '#FFCC80'
GRAY   = '#424242'; LGRAY   = '#F5F5F5'; MGRAY   = '#BDBDBD'
WHITE  = '#FFFFFF'

# ── Helpers ─────────────────────────────────────────────────────────

def save(fig, name):
    path = os.path.join(OUT, name)
    fig.savefig(path, dpi=DPI, bbox_inches='tight', facecolor=WHITE)
    plt.close(fig)
    print(f"  ✓  {name}")


def rbox(ax, x, y, w, h, label, ec, fc, fontsize=9, bold=False,
         lw=2, radius=0.06, sublabel=None, sublabel_fs=7.5):
    """Draw a rounded rectangle with centred label."""
    r = FancyBboxPatch((x - w/2, y - h/2), w, h,
                       boxstyle=f'round,pad={radius}',
                       edgecolor=ec, facecolor=fc, linewidth=lw, zorder=3)
    ax.add_patch(r)
    weight = 'bold' if bold else 'normal'
    yo = y + (0.10 if sublabel else 0)
    ax.text(x, yo, label, ha='center', va='center',
            fontsize=fontsize, fontweight=weight, color=GRAY, zorder=4,
            multialignment='center')
    if sublabel:
        ax.text(x, y - 0.13, sublabel, ha='center', va='center',
                fontsize=sublabel_fs, color=GRAY, style='italic', zorder=4,
                multialignment='center')


def arrow(ax, x1, y1, x2, y2, color=GRAY, lw=1.5, style='->', dashed=False):
    ls = (0, (5, 4)) if dashed else 'solid'
    ax.annotate('', xy=(x2, y2), xytext=(x1, y1),
                arrowprops=dict(arrowstyle=style, color=color,
                                lw=lw, linestyle=ls),
                zorder=5)


def actor(ax, x, y, label, color=BLUE, fontsize=9):
    """Stick-figure actor."""
    # head
    circle = plt.Circle((x, y + 0.38), 0.12, color=color, fill=True, zorder=4)
    ax.add_patch(circle)
    # body
    ax.plot([x, x], [y + 0.26, y - 0.02], color=color, lw=2, zorder=4)
    # arms
    ax.plot([x - 0.18, x + 0.18], [y + 0.10, y + 0.10], color=color, lw=2, zorder=4)
    # legs
    ax.plot([x, x - 0.14], [y - 0.02, y - 0.30], color=color, lw=2, zorder=4)
    ax.plot([x, x + 0.14], [y - 0.02, y - 0.30], color=color, lw=2, zorder=4)
    # label
    ax.text(x, y - 0.45, label, ha='center', va='top',
            fontsize=fontsize, color=color, fontweight='bold',
            multialignment='center', zorder=4)


def uc_ellipse(ax, x, y, w, h, label, ec=ORANGE, fc=LORANGE, fontsize=8.5):
    """Draw a use-case ellipse."""
    e = Ellipse((x, y), w, h, edgecolor=ec, facecolor=fc, linewidth=1.5, zorder=3)
    ax.add_patch(e)
    # wrap long labels
    words = label.split('\n')
    ax.text(x, y, '\n'.join(words), ha='center', va='center',
            fontsize=fontsize, color=GRAY, multialignment='center', zorder=4,
            linespacing=1.3)


def class_box(ax, x, y, name, attrs, w=2.8, row_h=0.28, hdr_h=0.38,
              ec=BLUE, fc_hdr=LBLUE):
    """Draw a UML-style class box."""
    n = len(attrs)
    total_h = hdr_h + n * row_h
    # header
    hdr = FancyBboxPatch((x, y - hdr_h), w, hdr_h,
                         boxstyle='round,pad=0.02',
                         edgecolor=ec, facecolor=fc_hdr, linewidth=1.8, zorder=3)
    ax.add_patch(hdr)
    ax.text(x + w/2, y - hdr_h/2, name,
            ha='center', va='center', fontsize=9.5, fontweight='bold',
            color=GRAY, zorder=4)
    # body
    body = FancyBboxPatch((x, y - total_h), w, n * row_h,
                          boxstyle='round,pad=0.02',
                          edgecolor=ec, facecolor=LGRAY, linewidth=1.5, zorder=3)
    ax.add_patch(body)
    for i, attr in enumerate(attrs):
        ay = y - hdr_h - (i + 0.5) * row_h
        ax.text(x + 0.12, ay, attr, ha='left', va='center',
                fontsize=7.8, color=GRAY,
                fontfamily='monospace', zorder=4)
    return total_h


# ════════════════════════════════════════════════════════════════════
# 1. SYSTEM ARCHITECTURE DIAGRAM
# ════════════════════════════════════════════════════════════════════
def draw_architecture():
    fig, ax = plt.subplots(figsize=(11, 6.5))
    ax.set_xlim(0, 11); ax.set_ylim(0, 6.5)
    ax.axis('off')
    fig.patch.set_facecolor(WHITE)

    # Title
    ax.text(5.5, 6.15, 'Kiến trúc hệ thống VietForces', ha='center',
            fontsize=15, fontweight='bold', color=RED)
    ax.text(5.5, 5.8, 'Đa nền tảng  ·  Supabase Backend  ·  OpenAI AI  ·  Firebase FCM',
            ha='center', fontsize=9, color=GRAY, style='italic')

    # ── Client layer label ──────────────────────────────────────────
    ax.text(0.25, 4.9, 'Client\nLayer', ha='left', va='center',
            fontsize=8, color=MGRAY, style='italic')
    ax.plot([0.8, 0.8], [3.9, 5.4], color=MGRAY, lw=0.8, ls='--')

    # ── Client boxes ───────────────────────────────────────────────
    clients = [
        (2.2,  4.7, 'Android App', 'Kotlin 2.0 + Jetpack Compose\nHilt DI + Supabase SDK', RED, LRED),
        (5.5,  4.7, 'Web Admin', 'Next.js 15 App Router\nTypeScript + Tailwind', BLUE, LBLUE),
        (8.8,  4.7, 'Landing Page', 'Next.js 15\nTailwind CSS', GREEN, LGREEN),
    ]
    for cx, cy, title, sub, ec, fc in clients:
        rbox(ax, cx, cy, 2.6, 0.85, title, ec, fc, fontsize=10, bold=True,
             sublabel=sub, sublabel_fs=7.5, lw=2.2)

    # ── Supabase backend ───────────────────────────────────────────
    ax.text(0.25, 3.2, 'Backend\nLayer', ha='left', va='center',
            fontsize=8, color=MGRAY, style='italic')
    sb = FancyBboxPatch((1.0, 2.35), 9.0, 1.5,
                        boxstyle='round,pad=0.08',
                        edgecolor=ORANGE, facecolor=LORANGE, linewidth=2.5, zorder=2)
    ax.add_patch(sb)
    ax.text(5.5, 3.5, 'Supabase Backend', ha='center',
            fontsize=12, fontweight='bold', color=ORANGE, zorder=3)
    ax.text(5.5, 3.10,
            'PostgreSQL + RLS Policies  ·  Auth (Email + Google OAuth)  ·  Realtime WebSocket',
            ha='center', fontsize=8.8, color=GRAY, zorder=3)
    ax.text(5.5, 2.72,
            '10 tables  ·  3 SECURITY DEFINER functions  ·  4 Edge Functions (Deno)  ·  pg_cron',
            ha='center', fontsize=8.2, color=GRAY, style='italic', zorder=3)

    # ── External services ──────────────────────────────────────────
    ax.text(0.25, 1.3, 'External\nServices', ha='left', va='center',
            fontsize=8, color=MGRAY, style='italic')
    ext = [
        (2.5,  1.15, 'OpenAI API\ngpt-4o-mini',        RED,    LRED),
        (5.5,  1.15, 'Firebase Cloud Messaging\n(FCM)', BLUE,   LBLUE),
        (8.5,  1.15, 'Vercel\n(Web Deploy)',            GREEN,  LGREEN),
    ]
    for ex, ey, lbl, ec, fc in ext:
        rbox(ax, ex, ey, 2.8, 0.75, lbl, ec, fc, fontsize=9, lw=1.8)

    # ── Arrows: clients → Supabase ─────────────────────────────────
    for cx in [2.2, 5.5, 8.8]:
        arrow(ax, cx, 4.28, cx, 3.85, color=MGRAY, lw=1.8)

    # ── Arrows: Supabase → external ────────────────────────────────
    for ex in [2.5, 5.5, 8.5]:
        arrow(ax, ex, 2.35, ex, 1.55, color=MGRAY, lw=1.5)

    # ── Legend ─────────────────────────────────────────────────────
    leg_items = [
        mpatches.Patch(facecolor=LRED,    edgecolor=RED,    label='Android (Kotlin)'),
        mpatches.Patch(facecolor=LBLUE,   edgecolor=BLUE,   label='Web (Next.js)'),
        mpatches.Patch(facecolor=LORANGE, edgecolor=ORANGE, label='Supabase Backend'),
    ]
    ax.legend(handles=leg_items, loc='lower right', fontsize=8,
              framealpha=0.9, ncol=3)

    save(fig, 'architecture.png')


# ════════════════════════════════════════════════════════════════════
# 2. USE CASE DIAGRAM
# ════════════════════════════════════════════════════════════════════
def draw_usecase():
    fig, ax = plt.subplots(figsize=(14, 18))
    ax.set_xlim(0, 14); ax.set_ylim(0, 18)
    ax.axis('off')
    fig.patch.set_facecolor(WHITE)

    # ── Title ───────────────────────────────────────────────────────
    ax.text(7, 17.6, 'Sơ đồ Use Case — VietForces', ha='center',
            fontsize=16, fontweight='bold', color=RED)

    # ══ Android App system boundary ════════════════════════════════
    sys_app = FancyBboxPatch((2.2, 4.5), 9.2, 12.5,
                             boxstyle='round,pad=0.15',
                             edgecolor=GREEN, facecolor=LGREEN, linewidth=2.5, zorder=1,
                             alpha=0.4)
    ax.add_patch(sys_app)
    ax.text(6.8, 16.75, 'VietForces Android App', ha='center',
            fontsize=11, fontweight='bold', color=GREEN, zorder=2)

    # ══ Use cases: Authentication ═══════════════════════════════════
    ax.text(6.8, 16.3, '── Xác thực & Onboarding ──', ha='center',
            fontsize=8.5, color=GRAY, style='italic', zorder=2)
    uc_ellipse(ax, 6.8, 15.8, 4.2, 0.65, 'Đăng ký / Đăng nhập')
    uc_ellipse(ax, 6.8, 14.9, 3.2, 0.55, 'Đăng xuất')
    uc_ellipse(ax, 9.8, 15.35, 2.8, 0.55, 'Onboarding\n(4 bước)', fc=LGREEN, ec=GREEN)

    # ══ Use cases: Game & ELO ═══════════════════════════════════════
    ax.text(6.8, 14.25, '── Gameplay ──', ha='center',
            fontsize=8.5, color=GRAY, style='italic', zorder=2)
    uc_ellipse(ax, 5.5, 13.6, 4.0, 0.65, 'Chơi game mode\n(8 loại)')
    uc_ellipse(ax, 9.8, 13.6, 2.8, 0.55, 'Cập nhật ELO\n(server-side)', fc=LGREEN, ec=GREEN)

    # ══ Use cases: Daily Challenge ══════════════════════════════════
    ax.text(6.8, 12.85, '── Daily Habit ──', ha='center',
            fontsize=8.5, color=GRAY, style='italic', zorder=2)
    uc_ellipse(ax, 5.5, 12.2, 4.2, 0.65, 'Thực hiện\nDaily Challenge')
    uc_ellipse(ax, 9.8, 12.2, 2.8, 0.55, 'Cập nhật Streak\n(server-side)', fc=LGREEN, ec=GREEN)

    # ══ Use cases: Social / Leaderboard ════════════════════════════
    ax.text(6.8, 11.45, '── Social & Ranking ──', ha='center',
            fontsize=8.5, color=GRAY, style='italic', zorder=2)
    uc_ellipse(ax, 5.2, 10.8, 3.8, 0.65, 'Xem bảng\nxếp hạng (real-time)')
    uc_ellipse(ax, 5.2, 9.9,  3.5, 0.65, 'Kết bạn / Theo dõi')
    uc_ellipse(ax, 9.5, 10.35, 2.8, 0.55, 'Xem hồ sơ\nngười khác', fc=LGREEN, ec=GREEN)

    # ══ Use cases: AI Features ══════════════════════════════════════
    ax.text(6.8, 9.15, '── Tính năng AI ──', ha='center',
            fontsize=8.5, color=GRAY, style='italic', zorder=2)
    uc_ellipse(ax, 4.8, 8.55, 3.5, 0.65, 'AI Roleplay\n(luyện hội thoại)',
               fc='#FCE4EC', ec='#C2185B')
    uc_ellipse(ax, 8.5, 8.55, 3.5, 0.65, 'AI Writing Practice\n(viết + chấm bài)',
               fc='#FCE4EC', ec='#C2185B')

    # ══ Use cases: Notifications ════════════════════════════════════
    ax.text(6.8, 7.6, '── Thông báo ──', ha='center',
            fontsize=8.5, color=GRAY, style='italic', zorder=2)
    uc_ellipse(ax, 6.8, 7.0, 4.0, 0.65, 'Nhận thông báo\nnhắc học (FCM)')

    # ══ Actor: Người học ═══════════════════════════════════════════
    actor(ax, 1.1, 12.0, 'Người học\n(Learner)', color=BLUE, fontsize=9)

    # Arrows from Learner to use cases
    learner_ucs = [
        (6.8, 15.8), (6.8, 14.9), (5.5, 13.6),
        (5.5, 12.2), (5.2, 10.8), (5.2, 9.9),
        (4.8, 8.55), (8.5, 8.55), (6.8, 7.0),
    ]
    for ux, uy in learner_ucs:
        ax.annotate('', xy=(ux - 1.8, uy), xytext=(1.45, 12.0),
                    arrowprops=dict(arrowstyle='->', color=BLUE,
                                    lw=1.2, connectionstyle='arc3,rad=0.0'),
                    zorder=5)

    # Include/extend dashed arrows
    dashed_pairs = [
        ((6.8, 15.8), (9.8, 15.35), '«extend»'),
        ((5.5, 13.6), (9.8, 13.6),  '«include»'),
        ((5.5, 12.2), (9.8, 12.2),  '«include»'),
        ((5.2, 9.9),  (9.5, 10.35), '«extend»'),
    ]
    for (x1, y1), (x2, y2), lbl in dashed_pairs:
        ax.annotate('', xy=(x2 - 1.2, y2), xytext=(x1 + 1.8, y1),
                    arrowprops=dict(arrowstyle='->', color=ORANGE,
                                    lw=1.2, linestyle=(0, (5, 3))),
                    zorder=5)
        mx, my = (x1 + 1.8 + x2 - 1.2) / 2, (y1 + y2) / 2
        ax.text(mx, my + 0.08, lbl, ha='center', fontsize=7.5,
                color=ORANGE, style='italic', zorder=6)

    # ══ Web Admin system boundary ════════════════════════════════
    sys_admin = FancyBboxPatch((2.2, 0.4), 9.2, 3.75,
                               boxstyle='round,pad=0.15',
                               edgecolor=BLUE, facecolor=LBLUE, linewidth=2.5, zorder=1,
                               alpha=0.4)
    ax.add_patch(sys_admin)
    ax.text(6.8, 3.95, 'Web Admin Dashboard', ha='center',
            fontsize=11, fontweight='bold', color=BLUE, zorder=2)

    # Admin use cases
    admin_ucs = [
        (4.0, 3.2, 'Quản lý từ vựng\n(CRUD + ảnh)'),
        (7.5, 3.2, 'Quản lý người dùng'),
        (4.0, 2.2, 'Xem analytics\n(DAU, retention)'),
        (7.5, 2.2, 'Lên lịch daily\nchallenge'),
        (6.0, 1.2, 'Ban / Unban user'),
    ]
    for ux, uy, lbl in admin_ucs:
        uc_ellipse(ax, ux, uy, 3.4, 0.65, lbl, ec=BLUE, fc=LBLUE, fontsize=8)

    # Arrow: Manage Users → Ban user (extend)
    ax.annotate('', xy=(6.5, 1.55), xytext=(7.5, 1.88),
                arrowprops=dict(arrowstyle='->', color=ORANGE,
                                lw=1.2, linestyle=(0, (5, 3))), zorder=5)
    ax.text(7.3, 1.7, '«extend»', ha='center', fontsize=7, color=ORANGE, style='italic')

    # Actor: Admin
    actor(ax, 1.1, 2.2, 'Quản trị\nviên (Admin)', color=BLUE, fontsize=9)
    for ux, uy, _ in admin_ucs[:4]:
        ax.annotate('', xy=(ux - 1.5, uy), xytext=(1.45, 2.2),
                    arrowprops=dict(arrowstyle='->', color=BLUE, lw=1.2), zorder=5)

    # Actor: System/Cron
    actor(ax, 12.5, 9.5, 'Hệ thống\n(Cron)', color=GREEN, fontsize=9)
    arrow(ax, 12.2, 9.9, 8.8, 7.3, color=GREEN, lw=1.3)
    ax.text(11.2, 8.8, 'tự động', fontsize=7.5, color=GREEN, style='italic')
    arrow(ax, 12.5, 9.5, 9.8, 12.2, color=GREEN, lw=1.3)

    save(fig, 'usecase.png')


# ════════════════════════════════════════════════════════════════════
# 3. CLASS / DOMAIN DIAGRAM
# ════════════════════════════════════════════════════════════════════
def draw_class_diagram():
    fig, ax = plt.subplots(figsize=(15, 10))
    ax.set_xlim(0, 15); ax.set_ylim(0, 10)
    ax.axis('off')
    fig.patch.set_facecolor(WHITE)

    ax.text(7.5, 9.75, 'Sơ đồ Lớp (Domain Model) — VietForces', ha='center',
            fontsize=14, fontweight='bold', color=RED)
    ax.text(7.5, 9.4, 'Android Data Models  ·  Supabase PostgreSQL Schema',
            ha='center', fontsize=9, color=GRAY, style='italic')

    # ── User ─────────────────────────────────────────────────────
    class_box(ax, 0.4, 9.0, 'User',
              ['id: UUID  (PK)', 'username: String',
               'timezone: String', 'avatar_url: String?',
               'is_banned: Boolean', 'fcm_token: String?',
               'created_at: Timestamp'],
              w=3.2, ec=RED, fc_hdr=LRED)

    # ── UserProgress ─────────────────────────────────────────────
    class_box(ax, 4.2, 9.0, 'UserProgress',
              ['user_id: UUID  (FK → User)',
               'elo_score: Int',
               'streak_count: Int',
               'streak_freeze_count: Int',
               'last_practice_date: Date?',
               'total_games: Int',
               'words_learned: JSONB'],
              w=3.5, ec=BLUE, fc_hdr=LBLUE)

    # ── VocabularyItem ───────────────────────────────────────────
    class_box(ax, 0.4, 5.6, 'VocabularyItem',
              ['id: String  (PK)',
               'word: String',
               'translation: String',
               'category: String',
               'imageRes: Int',
               'classifiers: List<String>',
               'distractors: List<String>'],
              w=3.2, ec=GREEN, fc_hdr=LGREEN)

    # ── DailyChallenge ───────────────────────────────────────────
    class_box(ax, 4.2, 5.6, 'DailyChallenge',
              ['id: UUID  (PK)',
               'challenge_date: Date',
               'question_data: JSONB',
               'difficulty: String',
               'created_by: String'],
              w=3.5, ec=ORANGE, fc_hdr=LORANGE)

    # ── LeaderboardEntry ─────────────────────────────────────────
    class_box(ax, 8.2, 9.0, 'LeaderboardEntry',
              ['user_id: UUID  (FK → User)',
               'username: String',
               'elo_score: Int',
               'weekly_elo: Int',
               'rank_tier: EloRank',
               'updated_at: Timestamp'],
              w=3.5, ec=RED, fc_hdr=MRED)

    # ── Friendship ───────────────────────────────────────────────
    class_box(ax, 11.9, 9.0, 'Friendship',
              ['follower_id: UUID  (FK → User)',
               'following_id: UUID (FK → User)',
               'created_at: Timestamp'],
              w=2.9, ec=BLUE, fc_hdr=MBLUE)

    # ── ActivityFeedItem ─────────────────────────────────────────
    class_box(ax, 8.2, 5.6, 'ActivityFeedItem',
              ['id: UUID  (PK)',
               'user_id: UUID  (FK → User)',
               'event_type: String',
               'metadata: JSONB',
               'created_at: Timestamp'],
              w=3.5, ec=ORANGE, fc_hdr=MORANGE)

    # ── EloRank enum ─────────────────────────────────────────────
    class_box(ax, 11.9, 5.5, '<<enum>>  EloRank',
              ['BRONZE  (<1200)',
               'SILVER  (1200–2099)',
               'GOLD    (2100–2399)',
               'PLATINUM(2400–2699)',
               'DIAMOND (≥2700)'],
              w=2.9, ec=ORANGE, fc_hdr=MORANGE)

    # ── GameMode enum ─────────────────────────────────────────────
    class_box(ax, 0.4, 2.1, '<<enum>>  GameMode',
              ['IMAGE_TO_WORD', 'WORD_TO_IMAGE',
               'SYLLABLE_MATCH', 'WORD_SEARCH',
               'FILL_BLANK', 'WORD_CHAIN',
               'SENTENCE_ORDER', 'WRITING'],
              w=3.2, ec=GREEN, fc_hdr=MGREEN)

    # ── Relationships ─────────────────────────────────────────────
    rel_style = dict(arrowstyle='->', lw=1.5, color=GRAY)

    # User 1──1 UserProgress
    ax.annotate('', xy=(4.2, 7.1), xytext=(3.6, 7.1),
                arrowprops=dict(**rel_style), zorder=5)
    ax.text(3.9, 7.25, '1', ha='center', fontsize=8, color=GRAY)
    ax.text(4.15, 7.25, '1', ha='center', fontsize=8, color=GRAY)

    # User 1──* LeaderboardEntry
    ax.plot([2.0, 2.0, 9.95], [8.98, 8.35, 8.35], color=MGRAY, lw=1.5, zorder=5)
    ax.annotate('', xy=(9.95, 7.82), xytext=(9.95, 8.35),
                arrowprops=dict(arrowstyle='->', color=MGRAY, lw=1.5), zorder=5)
    ax.text(1.8, 8.2, '1', fontsize=8, color=MGRAY)
    ax.text(10.05, 7.9, '*', fontsize=10, color=MGRAY)

    # User 1──* Friendship
    ax.plot([2.0, 2.0, 13.35], [8.98, 8.25, 8.25], color=MGRAY, lw=1.3, ls='--', zorder=5)
    ax.annotate('', xy=(13.35, 7.8), xytext=(13.35, 8.25),
                arrowprops=dict(arrowstyle='->', color=MGRAY, lw=1.3), zorder=5)

    # User 1──* ActivityFeedItem
    ax.plot([2.0, 2.0, 9.95], [8.98, 7.65, 7.65], color=MGRAY, lw=1.3, ls='--', zorder=5)
    ax.annotate('', xy=(9.95, 5.3), xytext=(9.95, 7.65),
                arrowprops=dict(arrowstyle='->', color=MGRAY, lw=1.3), zorder=5)

    # UserProgress ──* VocabularyItem
    ax.annotate('', xy=(2.0, 3.5), xytext=(5.95, 5.55),
                arrowprops=dict(arrowstyle='->', color=MGRAY, lw=1.3, ls='--'), zorder=5)
    ax.text(3.7, 4.8, 'learns *', fontsize=7.5, color=MGRAY, style='italic')

    # EloRank annotation
    ax.text(13.35, 6.8, 'used by\nLeaderboardEntry', ha='center',
            fontsize=7.5, color=MGRAY, style='italic')
    ax.plot([13.35, 13.35], [6.65, 6.35], color=MGRAY, lw=1.2, ls='--')
    ax.annotate('', xy=(11.5, 7.2), xytext=(13.35, 6.35),
                arrowprops=dict(arrowstyle='->', color=MGRAY, lw=1.2, ls='--'), zorder=5)

    save(fig, 'class_diagram.png')


# ════════════════════════════════════════════════════════════════════
# 4. MVVM ARCHITECTURE (Android Layers)
# ════════════════════════════════════════════════════════════════════
def draw_mvvm():
    fig, ax = plt.subplots(figsize=(9, 8))
    ax.set_xlim(0, 9); ax.set_ylim(0, 8)
    ax.axis('off')
    fig.patch.set_facecolor(WHITE)

    ax.text(4.5, 7.7, 'Kiến trúc MVVM + Repository + Hilt', ha='center',
            fontsize=13, fontweight='bold', color=BLUE)
    ax.text(4.5, 7.35, 'Android App — Jetpack Compose', ha='center',
            fontsize=9, color=GRAY, style='italic')

    layers = [
        (6.4, 6.5, 'UI Layer — Jetpack Compose',
         'Screens (26 routes)  ·  Components  ·  Theme/Material 3',
         BLUE, LBLUE),
        (6.4, 5.3, 'ViewModel Layer — MVVM',
         'AuthVM  ·  DailyChallengeVM  ·  LeaderboardVM\nSocialVM  ·  ActivityFeedVM',
         BLUE, '#BBDEFB'),
        (6.4, 4.0, 'Repository Layer',
         'AuthRepo  ·  VocabRepo  ·  ProgressRepo\nStreakRepo  ·  EloRepo  ·  SocialRepo',
         GREEN, LGREEN),
        (6.4, 2.7, 'Remote / Manager Layer',
         'AiManager  ·  OpenAiClient  ·  FCMTokenManager\nRemoteProgressSource',
         ORANGE, LORANGE),
        (6.4, 1.4, 'Supabase SDK',
         'Auth  ·  Postgrest  ·  Realtime  ·  Storage',
         RED, LRED),
    ]

    for lx, ly, title, sub, ec, fc in layers:
        r = FancyBboxPatch((0.6, ly - 0.52), 7.8, 1.0,
                           boxstyle='round,pad=0.06',
                           edgecolor=ec, facecolor=fc, linewidth=2.2, zorder=3)
        ax.add_patch(r)
        ax.text(lx, ly + 0.14, title, ha='center', va='center',
                fontsize=10.5, fontweight='bold', color=GRAY, zorder=4)
        ax.text(lx, ly - 0.2, sub, ha='center', va='center',
                fontsize=8.5, color=GRAY, zorder=4, multialignment='center')

    # Arrows between layers
    arrow_ys = [5.98, 4.78, 3.48, 2.18]
    labels = ['StateFlow / collectAsStateWithLifecycle',
              'suspend fun / Flow<T> → Result<T>',
              'suspend fun → Result<T>',
              'Supabase SDK calls (coroutines)']
    for y, lbl in zip(arrow_ys, labels):
        ax.annotate('', xy=(4.5, y - 0.28), xytext=(4.5, y),
                    arrowprops=dict(arrowstyle='<->', color=MGRAY, lw=1.8), zorder=5)
        ax.text(4.5, y - 0.14, lbl, ha='center', va='center',
                fontsize=7.5, color=MGRAY, style='italic', zorder=6)

    # Hilt DI badge
    hilt = FancyBboxPatch((7.0, 3.2), 1.8, 3.3,
                          boxstyle='round,pad=0.05',
                          edgecolor=GREEN, facecolor=LGREEN,
                          linewidth=1.5, alpha=0.7, zorder=2)
    ax.add_patch(hilt)
    ax.text(7.9, 4.85, 'Hilt DI', ha='center', fontsize=9,
            fontweight='bold', color=GREEN, zorder=3, rotation=90)
    ax.text(7.9, 4.2, '@HiltViewModel\n@Singleton\n@Inject', ha='center',
            fontsize=7, color=GREEN, zorder=3, rotation=90,
            multialignment='center')

    save(fig, 'mvvm_layers.png')


# ════════════════════════════════════════════════════════════════════
# 5. AI ARCHITECTURE
# ════════════════════════════════════════════════════════════════════
def draw_ai():
    fig, ax = plt.subplots(figsize=(10, 7))
    ax.set_xlim(0, 10); ax.set_ylim(0, 7)
    ax.axis('off')
    fig.patch.set_facecolor(WHITE)

    ax.text(5, 6.7, 'Kiến trúc AI Integration — VietForces', ha='center',
            fontsize=13, fontweight='bold', color=RED)
    ax.text(5, 6.35,
            '"grade locally first — call AI only when necessary"',
            ha='center', fontsize=9, color=GRAY, style='italic')

    # ── Left column: request flow ────────────────────────────────
    ai_layers = [
        (3.4, 5.7, 'UI Layer (Compose)',
         'WritingPractice · Game Screens · LearningPath · Mascot',
         BLUE, LBLUE),
        (3.4, 4.5, 'AiManager',
         'Builds prompts · Parses JSON · AI toggles · Fallback',
         ORANGE, LORANGE),
        (3.4, 3.3, 'OpenAiClient',
         'OkHttp POST · response_format=json_object · Timeouts',
         RED, LRED),
        (3.4, 2.1, 'Supabase Edge Function: openai-proxy',
         'Deno serverless · keeps API key server-side',
         GREEN, LGREEN),
        (3.4, 0.9, 'OpenAI API  (gpt-4o-mini)',
         'Structured JSON output · Temperature 0.4 · max_tokens 900',
         RED, MRED),
    ]

    for lx, ly, title, sub, ec, fc in ai_layers:
        r = FancyBboxPatch((0.4, ly - 0.48), 6.0, 0.9,
                           boxstyle='round,pad=0.06',
                           edgecolor=ec, facecolor=fc, linewidth=2, zorder=3)
        ax.add_patch(r)
        ax.text(lx, ly + 0.12, title, ha='center',
                fontsize=10, fontweight='bold', color=GRAY, zorder=4)
        ax.text(lx, ly - 0.18, sub, ha='center',
                fontsize=8, color=GRAY, zorder=4)

    # Arrows
    for y in [5.22, 4.02, 2.82, 1.62]:
        arrow(ax, 3.4, y, 3.4, y - 0.22, color=MGRAY, lw=2)

    # Return arrow on right side
    ax.annotate('', xy=(6.8, 4.02), xytext=(6.8, 0.42),
                arrowprops=dict(arrowstyle='->', color=GREEN,
                                lw=2, connectionstyle='arc3,rad=0.0'), zorder=5)
    ax.text(7.3, 2.2, 'JSON\nresponse', ha='center', fontsize=8.5,
            color=GREEN, fontweight='bold')

    # ── Right column: 5 AI features ────────────────────────────────
    ax.text(8.7, 5.8, '5 tính năng AI', ha='center', fontsize=10,
            fontweight='bold', color=RED)
    features = [
        ('Writing Grading',    '#C2185B', '#FCE4EC'),
        ('Open-answer Grading','#C2185B', '#FCE4EC'),
        ('Mascot Reactions',   ORANGE,    LORANGE),
        ('Learning Path',      BLUE,      LBLUE),
        ('Roleplay Tutor ★',   GREEN,     LGREEN),
    ]
    for i, (name, ec, fc) in enumerate(features):
        fy = 5.35 - i * 0.78
        r = FancyBboxPatch((7.5, fy - 0.28), 2.3, 0.52,
                           boxstyle='round,pad=0.04',
                           edgecolor=ec, facecolor=fc, linewidth=1.5, zorder=3)
        ax.add_patch(r)
        ax.text(8.65, fy, name, ha='center', va='center',
                fontsize=8.5, color=GRAY, zorder=4)

    save(fig, 'ai_architecture.png')


# ════════════════════════════════════════════════════════════════════
# RUN ALL
# ════════════════════════════════════════════════════════════════════
if __name__ == '__main__':
    print("\nGenerating VietForces report diagrams...\n")
    draw_architecture()
    draw_usecase()
    draw_class_diagram()
    draw_mvvm()
    draw_ai()
    print("\nDone! All PNG files saved to:", OUT)
