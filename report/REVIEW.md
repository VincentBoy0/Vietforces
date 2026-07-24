# Code Review — VietForces LaTeX Report

**Reviewed:** `main.tex`, `brochure.tex`
**Date:** 2026-07-24
**Reviewer:** AI adversarial code reviewer
**Compiler target:** XeLaTeX (primary) / pdfLaTeX fallback

---

## Summary

Both files are well-structured, visually rich LaTeX documents with ambitious TikZ diagrams and tcolorbox layouts. Overall coverage of the professor's requirements is solid. However, there is **one guaranteed compilation blocker** in `main.tex` (invalid `tcolorbox` key) and **one probable compilation blocker** in `brochure.tex` (Arial font unavailable on Overleaf Linux). Several warnings around multi-pass rendering, emoji glyphs, and conflicting TikZ positioning options also need attention. The report cannot be submitted in its current state — fix the two blockers first, then address the warnings.

---

## Critical Issues (must fix before compile)

### CR-01 — Invalid `tcolorbox` key: `rounded corners`

**File:** `main.tex`, line 159
**Error type:** Compilation failure — `pgfkeys Error: I do not know the key '/tcb/rounded corners'`

`rounded corners` is a **TikZ path option**, not a `tcolorbox` key. Without the `enhanced` skin and an explicit TikZ passthrough, tcolorbox does not recognise it. The document will not compile past this point.

```latex
% BROKEN — line 159
\begin{tcolorbox}[colback=boxbg, colframe=vfred, title={\textbf{Bài toán chính}},
  fonttitle=\bfseries, rounded corners]

% FIX — replace `rounded corners` with the tcolorbox arc option
\begin{tcolorbox}[colback=boxbg, colframe=vfred, title={\textbf{Bài toán chính}},
  fonttitle=\bfseries, arc=4pt]
```

Note: everywhere else in the document that `rounded corners=Xpt` appears it is inside TikZ `\tikzset{}` or `\node[...]` syntax, which is perfectly valid. Only this one instance inside `\begin{tcolorbox}[...]` is wrong.

---

### CR-02 — `Arial` font unavailable on Overleaf's Linux server

**File:** `brochure.tex`, line 16
**Error type:** Probable compilation failure with XeLaTeX — `fontspec error: font not found`

Arial is a Microsoft proprietary font. Overleaf runs on Ubuntu Linux where it is **not** installed by default (only the `msttcorefonts` package installs it, and that package is not reliably present in all TeX Live distributions). If `fontspec` cannot find the font, XeLaTeX aborts immediately.

```latex
% CURRENT — line 16 (risky)
\setmainfont{Arial}[BoldFont={Arial Bold}]

% SAFE ALTERNATIVE — TeX Gyre Heros is Arial-metric-compatible, freely available
\setmainfont{TeX Gyre Heros}[
  BoldFont      = {TeX Gyre Heros Bold},
  ItalicFont    = {TeX Gyre Heros Italic},
  BoldItalicFont= {TeX Gyre Heros Bold Italic}
]
```

Alternatively, add a `local/` fonts folder to the Overleaf project and reference via `Path`. The same risk applies for `DejaVu Sans Mono`, which is generally available on Linux, so that one is lower risk.

`main.tex` uses `Times New Roman` (line 17–19) — this is more likely to be available via `msttcorefonts` on Overleaf, but for safety the same strategy (use `TeX Gyre Termes` as a drop-in) is recommended.

---

## Warnings (may cause layout or rendering problems)

### WR-01 — `remember picture` + `overlay` requires two compile passes

**File:** `brochure.tex`, lines 61–64

```latex
\begin{tikzpicture}[overlay, remember picture]
  \fill[vfred!70, opacity=0.5] (-1, 0.4) circle (1.8cm);
  \fill[white, opacity=0.05] (18, -0.2) circle (2.5cm);
\end{tikzpicture}
```

`remember picture` writes positional data to the `.aux` file; the shapes are only placed correctly on the **second** XeLaTeX run. On a fresh Overleaf project compiled once, the decorative circles will be at (0,0) or missing. Always compile **twice** (or hit "Recompile" twice on Overleaf). Consider adding a comment in the file.

---

### WR-02 — Emoji characters in `tcolorbox` titles may render as tofu boxes

**File:** `brochure.tex`, lines 100, 121, 149, 167, 186, 206, 236, 262, 279

Titles like `{🎯  Mục tiêu đề tài}`, `{🛠  Tech Stack}`, `{🎮 8 Game Modes}` etc. contain Unicode emoji (U+1F3AF, U+1F6E0, …). The main font (Arial / TeX Gyre Heros) does **not** contain these code points. With XeLaTeX, fontspec will attempt a font fallback; if no system emoji font is registered, each emoji renders as a `□` (missing glyph box) and produces a `Missing character` warning stream.

**Fix options:**
1. Replace emoji with LaTeX symbols: `$\star$`, `\textbullet`, `\(\triangleright\)`, or custom colored squares via `\textcolor{...}{\rule{0.8em}{0.8em}}`.
2. Use LuaLaTeX with `\usepackage{emoji}` + `\emoji{target}` (not XeLaTeX compatible).
3. Declare a fallback emoji font in fontspec:
   ```latex
   \newfontfamily\emojifont{Noto Color Emoji}[Renderer=HarfBuzz]
   % then wrap each emoji: {\emojifont 🎯}
   ```
   (requires Noto Emoji installed on the compile server).

---

### WR-03 — Conflicting node positioning: `below=` and explicit `at` on same node

**File:** `main.tex`, lines 270–273

```latex
\node[system, minimum width=6.5cm, minimum height=5.5cm,
      label={...},
      below=1.5cm of sysbox] (adminbox) at (4.75, -16) {};
```

Both the `positioning` library key `below=1.5cm of sysbox` and the explicit `at (4.75, -16)` set the node's center coordinate. TikZ resolves this by letting the explicit `at` **override** the `below=` option silently. The `below=` is dead code here. If the two positions disagree (and they do — `sysbox` center is at `(4.75, -6)` with height 13 cm, so its south is at ≈ y=-12.5; 1.5 cm below puts the adminbox center at ≈ y=-14, but `at` places it at y=-16), the gap between the two boxes will be larger than intended.

**Fix:** Remove `below=1.5cm of sysbox` from the options and keep only `at (4.75, -16)`, or vice versa.

---

### WR-04 — Dead TikZ arrow styles with legacy (potentially broken) syntax

**File:** `main.tex`, lines 440–441

```latex
comp/.style={-diamond, thick},
agg/.style={-open diamond, thick},
```

`-diamond` and `-open diamond` are **deprecated** tip names from the old `arrows` library. Since `arrows.meta` is loaded (line 36), the canonical names are `-{Diamond}` and `-{Diamond[open]}`. The old names may still work due to a compatibility shim, but emit warnings.

More importantly, **neither `comp` nor `agg` is ever used** in the document (confirmed by grep). These are dead code. Either remove them or update to `arrows.meta` syntax if you plan to draw composition/aggregation arrows:

```latex
comp/.style={-{Diamond[fill=vfblue, length=4mm]}, thick},
agg/.style={-{Diamond[open, length=4mm]}, thick},
```

---

### WR-05 — `\fvset{breaklines=true}` — non-standard `fancyvrb` key

**File:** `main.tex`, lines 112–114

```latex
\fvset{frame=single, framerule=0.4pt, rulecolor=\color{boxframe},
  fontsize=\footnotesize, breaklines=true, xleftmargin=4pt, xrightmargin=4pt}
```

`breaklines` is **not** a recognized key in `fancyvrb` before version 4 (added in 2021). On TeX Live 2020 or earlier, this triggers `fancyvrb Error: Unknown option 'breaklines'` during preamble processing — a hard error. On TeX Live 2022+ (Overleaf default as of 2023) it works. This is a version portability risk.

Additionally, no `\begin{Verbatim}` environment exists anywhere in the document, making the entire `\fvset` call effectively dead. **Consider removing `\fvset` entirely** (and possibly `\usepackage{fancyvrb}` too).

---

### WR-06 — All figure environments are missing `\label`

**File:** `main.tex`, figures at lines 216, 427, 546, 583, 681, 741, 756, 771

Seven `\begin{figure}[H]` environments have `\caption{...}` but **none have a corresponding `\label{fig:...}`**. This means none of the diagrams (Use Case, Class Diagram, Architecture, AI Layer, etc.) can be referenced via `\ref{}` elsewhere in the text. This is particularly notable because the report text occasionally says things like "sơ đồ sau" (the following diagram) with no formal reference number.

**Fix:** Add `\label{fig:usecase}`, `\label{fig:classdiagram}`, etc. immediately after each `\caption`:

```latex
\caption{Sơ đồ Use Case tổng thể của hệ thống VietForces.}
\label{fig:usecase}    % ← add this
```

---

### WR-07 — Class diagram: long-distance crossing arrows

**File:** `main.tex`, lines 530–537

```latex
\draw[ass] ([xshift=2mm]lbody.north west) -- node[above,font=\tiny]{ref} ([xshift=-2mm]ubody.south east);
\draw[ass] ([xshift=2mm]fbody.north west) -- node[above,font=\tiny]{n..n} ([xshift=-2mm]ubody.south east);
```

`LeaderboardEntry` is at `(0, -11.0)` and `Friendship` is at `(1.5cm, -11.0)` (relative), while `User` is at `(0, 0)`. The arrows travel ≈11 cm vertically and cross through all intermediate nodes (UserProgress, VocabularyItem, DailyChallenge). The diagram will be visually confusing with overlapping lines. This compiles but the output is unprofessional.

**Fix:** Reroute the arrows to the right side of the diagram to avoid crossings, or add `to[bend left=...]` / waypoint coordinates.

---

### WR-08 — `fonttitle=\small\bfseries\color{white}` — `\color` inside font spec

**File:** `brochure.tex`, lines 262, 279

```latex
fonttitle=\small\bfseries\color{white},
```

`\color{white}` inside `fonttitle=` is not the `tcolorbox`-canonical way to set title text colour. The proper key is `coltitle=white` (which is already present on the other boxes). While this likely works because `\color{white}` is a valid LaTeX command, mixing font and colour specification in `fonttitle=` is fragile — tcolorbox may reset the colour after applying `fonttitle`. Use `coltitle=white` exclusively:

```latex
% WR-08 fix
fonttitle=\small\bfseries, coltitle=white,
```

---

## Info (suggestions, low priority)

### IN-01 — Dead package imports (4 unused packages)

**File:** `main.tex`, lines 33, 37, 45, 46

The following packages are imported but **never used** in the document:

| Package | Why loaded | Used? |
|---|---|---|
| `subcaption` | subfigure environments | No `\begin{subfigure}` found |
| `mindmap` (tikzlibrary) | mindmap diagrams | No `mindmap` TikZ usage found |
| `tabularx` | tabularx environment | No `\begin{tabularx}` found |
| `makecell` | `\makecell`, `\thead` | Neither command found |

Remove unused imports to reduce preamble load time and potential conflicts:

```latex
% Remove these four lines:
\usepackage{subcaption}      % line 33
% ...and from the tikzlibrary line:
backgrounds, decorations.pathmorphing, mindmap}  % remove 'mindmap'
% ...and:
\usepackage{tabularx}        % line 45
\usepackage{makecell}        % line 46
```

---

### IN-02 — UI Screenshots section contains only placeholder `\fbox` content

**File:** `main.tex`, lines 741–776

All three figure environments in Section 8 contain placeholder `\fbox` boxes instead of real screenshots. The report lists filenames in comments (`images/screenshot_home.png`, etc.) but the images directory is empty (only `images/README.md` exists). The report **cannot be submitted** without real screenshot images.

**Action required:** Add the actual PNG files to `report/images/` and uncomment the `\includegraphics` lines.

---

### IN-03 — Demo video link is a blank underline

**File:** `main.tex`, line 800

```latex
\textbf{Link video demo:} \underline{\hspace{8cm}} (điền vào khi có)
```

This is a hand-fill-in blank that will appear literally in the PDF. The "when available" note acknowledges incompleteness. Remember to fill this in before submission.

---

### IN-04 — Sections 7 (AI Integration) and 10 (Tính năng nổi bật) substantially overlap

**File:** `main.tex`, lines 671 and 804

Section 7 covers the 5 AI features, security, cost, and architecture in detail. Section 10 then re-lists AI Integration as item 1 with bullet sub-points, plus 4 more gamification/social/FCM items. The AI content is duplicated verbatim.

**Suggestion:** Merge Section 10 into the respective earlier sections, or restructure so Section 10 is a pure executive summary pointing back to Sections 6–9, rather than repeating AI content.

---

### IN-05 — `\today` on title page not localized to Vietnamese

**File:** `main.tex`, line 139

```latex
{\large \today\par}
```

With `fontspec`/XeLaTeX (no `babel`), `\today` outputs the date in English (e.g., "July 24, 2026"). A Vietnamese graduation report should use Vietnamese date format.

**Fix:**
```latex
% Option A: hardcode
{\large Ngày 24 tháng 7 năm 2026\par}

% Option B: use babel (add to preamble, after fontspec)
\usepackage[vietnamese]{babel}
% \today then outputs "24 tháng 7 năm 2026"
```

---

### IN-06 — No `\listoffigures` or `\listoftables`

**File:** `main.tex`, after `\tableofcontents` (line 142)

The report has 7 figures and 5 tables, all with captions. A formal đồ án report typically includes a List of Figures and List of Tables immediately after the Table of Contents.

```latex
\tableofcontents
\listoffigures      % ← add
\listoftables       % ← add
\newpage
```

---

### IN-07 — `\fbox` placeholder figures may overflow page vertically

**File:** `main.tex`, lines 742–750

```latex
\fbox{\parbox{0.3\textwidth}{\centering\vspace{3cm}\small[Home Screen]\vspace{3cm}}}
```

Each `\fbox` is ≈6 cm tall (3+3 cm of `\vspace`). Three side-by-side fboxes of this height, with `\onehalfspacing` and figure caption, consume ≈7–8 cm per figure group. Three such figure environments back-to-back in Section 8 may exceed the page budget and push the section header of Section 9 to the next page. Not a compilation error but monitor layout once real images are added.

---

### IN-08 — TikZ `node distance=0.0cm` in class diagram is fragile

**File:** `main.tex`, line 428

```latex
\begin{tikzpicture}[...node distance=0.0cm]
```

Setting the global `node distance` to 0 cm makes `below=of X` semantically identical to "touch the bottom of X with no gap". Any rounding or border offsets in TikZ may introduce a 1–2 pt visual seam or overlap. A small positive value like `node distance=0.02cm` or use of `inner sep=0pt` on the relevant nodes is more robust.

---

## Professor Requirements Coverage

| # | Requirement | Status | Notes |
|---|---|---|---|
| 1 | Problem statement | ✅ Complete | Section 1, 4 sub-sections, scope table |
| 2 | Use case model with diagram | ✅ Complete | Section 2, TikZ use-case diagram + detailed table |
| 3 | Glossary | ✅ Complete | Section 3, 13-entry longtable with technical terms |
| 4 | Supplementary specification | ✅ Complete | Section 4, non-functional requirements table |
| 5 | Class diagram | ✅ Complete | Section 5, TikZ domain model + layer diagram |
| 6 | Software architecture | ✅ Complete | Section 6, system diagram + MVVM layer + Supabase table |
| 7 | Advanced features / AI | ✅ Complete | Section 7 (AI detail) + Section 10 (feature list); overlap is redundant |
| 8 | UI screenshots section | ⚠️ Incomplete | Section 8 exists but contains only `\fbox` placeholders — **no actual screenshots** |
| 9 | Demo video section | ⚠️ Incomplete | Section 9 exists but video link is blank — **must fill before submission** |
| 10 | A4 brochure (brochure.tex) | ⚠️ Blocked | `brochure.tex` is well-designed but will not compile until Arial font issue (CR-02) is resolved |

---

## Verdict

**FAIL — Fix CR-01 and CR-02 before compile; PASS WITH WARNINGS after.**

Two compilation blockers must be resolved before the document will build on Overleaf:

1. **CR-01** (`rounded corners` in `tcolorbox`, `main.tex:159`) → change to `arc=4pt`
2. **CR-02** (Arial font, `brochure.tex:16`) → switch to `TeX Gyre Heros`

After those fixes, the document will compile and the output quality is high. The most important non-blocker items before submission are:

- Add real screenshots to Section 8 (IN-02)
- Fill in the demo video link in Section 9 (IN-03)
- Compile twice for correct `remember picture` placement (WR-01)
- Test emoji rendering in `brochure.tex` and replace with LaTeX symbols if they appear as tofu boxes (WR-02)

_Reviewed by: AI code reviewer_
_Depth: standard + cross-file analysis_
