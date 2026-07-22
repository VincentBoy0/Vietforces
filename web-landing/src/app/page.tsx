export default function Home() {
  return (
    <main className="min-h-screen font-sans antialiased">
      {/* ── NAVBAR ── */}
      <header className="fixed top-0 left-0 right-0 z-50 backdrop-blur-md bg-[#1A1A1A]/80 border-b border-white/10">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between">
          {/* Logo */}
          <a href="#" className="flex items-center gap-2 no-underline">
            <span className="text-2xl leading-none">🇻🇳</span>
            <span className="text-white font-bold text-lg tracking-tight">VietForces</span>
          </a>

          {/* Desktop nav */}
          <nav className="hidden md:flex items-center gap-8">
            <a href="#features" className="text-white/80 hover:text-white text-sm transition-colors">
              Tính năng
            </a>
            <a href="#screenshots" className="text-white/80 hover:text-white text-sm transition-colors">
              Giao diện
            </a>
            <a
              href="#download"
              className="bg-[#DA251D] hover:bg-[#b81e17] text-white text-sm font-semibold px-4 py-2 rounded-full transition-colors"
            >
              Tải về
            </a>
          </nav>

          {/* Mobile hamburger — pure CSS toggle */}
          <label htmlFor="nav-toggle" className="md:hidden cursor-pointer flex flex-col gap-[5px] p-1">
            <span className="block w-6 h-0.5 bg-white rounded" />
            <span className="block w-6 h-0.5 bg-white rounded" />
            <span className="block w-6 h-0.5 bg-white rounded" />
          </label>
        </div>

        {/* Mobile menu */}
        <input type="checkbox" id="nav-toggle" className="hidden peer" />
        <div className="hidden peer-checked:flex md:hidden flex-col bg-[#1A1A1A] border-t border-white/10 px-4 py-4 gap-4">
          <a href="#features" className="text-white/80 hover:text-white text-base transition-colors">
            Tính năng
          </a>
          <a href="#screenshots" className="text-white/80 hover:text-white text-base transition-colors">
            Giao diện
          </a>
          <a
            href="#download"
            className="bg-[#DA251D] text-white text-base font-semibold px-4 py-2 rounded-full text-center transition-colors"
          >
            Tải về
          </a>
        </div>
      </header>

      {/* ── HERO SECTION (LAND-02) ── */}
      <section
        className="min-h-screen flex items-center pt-16"
        style={{
          background: "linear-gradient(135deg, #1A1A1A 0%, #2d0a08 50%, #DA251D 100%)",
        }}
      >
        <div className="max-w-6xl mx-auto px-4 sm:px-6 py-20 w-full">
          <div className="flex flex-col md:flex-row items-center gap-12 md:gap-16">
            {/* Left: Content */}
            <div className="flex-1 text-center md:text-left">
              {/* App icon */}
              <div className="inline-flex items-center justify-center w-20 h-20 bg-white rounded-2xl shadow-2xl mb-6">
                <span className="text-5xl leading-none">🇻🇳</span>
              </div>

              <h1 className="text-white text-5xl sm:text-6xl md:text-7xl font-extrabold tracking-tight leading-none mb-4">
                VietForces
              </h1>

              <p className="text-[#FFCD00] text-xl sm:text-2xl font-semibold mb-4">
                Học tiếng Việt qua trò chơi 🎮
              </p>

              <p className="text-white/70 text-base sm:text-lg leading-relaxed mb-8 max-w-xl mx-auto md:mx-0">
                Gamified Vietnamese vocabulary learning with AI-powered hints, real-time ELO
                leaderboards, and daily challenges. Free on Android.
              </p>

              <div className="flex flex-col sm:flex-row gap-3 justify-center md:justify-start">
                <a
                  href="#download"
                  className="inline-flex items-center justify-center gap-2 bg-[#FFCD00] hover:bg-yellow-300 text-[#1A1A1A] font-bold text-base px-7 py-3.5 rounded-full shadow-lg transition-all hover:scale-105"
                >
                  <span>▶</span> Tải về ngay — Miễn phí
                </a>
                <a
                  href="#features"
                  className="inline-flex items-center justify-center gap-2 border-2 border-white/40 hover:border-white text-white font-semibold text-base px-7 py-3.5 rounded-full transition-all hover:bg-white/10"
                >
                  Xem tính năng ↓
                </a>
              </div>
            </div>

            {/* Right: CSS Phone Mockup */}
            <div className="flex-shrink-0 flex justify-center md:justify-end">
              <div
                className="relative"
                style={{
                  width: "260px",
                  height: "520px",
                  borderRadius: "36px",
                  border: "10px solid #2a2a2a",
                  boxShadow:
                    "0 0 0 2px #444, 0 40px 80px rgba(0,0,0,0.7), 0 20px 40px rgba(218,37,29,0.3)",
                  background: "#111",
                  overflow: "hidden",
                }}
              >
                {/* Notch */}
                <div
                  className="absolute top-0 left-1/2 -translate-x-1/2 z-10"
                  style={{
                    width: "100px",
                    height: "28px",
                    background: "#2a2a2a",
                    borderRadius: "0 0 18px 18px",
                  }}
                />

                {/* Screen content */}
                <div className="w-full h-full flex flex-col" style={{ background: "#1a1a1a" }}>
                  {/* Status bar */}
                  <div className="flex justify-between items-center px-5 pt-8 pb-2">
                    <span className="text-white text-xs font-bold">9:41</span>
                    <span className="text-white text-xs">📶 🔋</span>
                  </div>

                  {/* App header */}
                  <div
                    className="mx-3 rounded-2xl p-4 mb-3"
                    style={{ background: "linear-gradient(135deg, #DA251D, #ff4a3a)" }}
                  >
                    <div className="flex items-center gap-2 mb-2">
                      <span className="text-xl">🇻🇳</span>
                      <span className="text-white font-bold text-sm">VietForces</span>
                    </div>
                    <div className="text-white/80 text-xs mb-1">Chào mừng trở lại!</div>
                    <div className="flex gap-3 mt-2">
                      <div className="bg-white/20 rounded-lg px-2 py-1 text-xs text-white">
                        🔥 7 ngày
                      </div>
                      <div className="bg-white/20 rounded-lg px-2 py-1 text-xs text-white">
                        🏆 #42
                      </div>
                    </div>
                  </div>

                  {/* Game mode cards */}
                  <div className="px-3 grid grid-cols-2 gap-2 flex-1">
                    {[
                      { icon: "🖼️", label: "Hình ảnh", color: "#3b82f6" },
                      { icon: "📝", label: "Từ vựng", color: "#8b5cf6" },
                      { icon: "🎵", label: "Phát âm", color: "#10b981" },
                      { icon: "📅", label: "Hôm nay", color: "#f59e0b" },
                    ].map((m) => (
                      <div
                        key={m.label}
                        className="rounded-xl p-3 flex flex-col items-center justify-center gap-1"
                        style={{ background: m.color + "22", border: `1px solid ${m.color}44` }}
                      >
                        <span className="text-2xl">{m.icon}</span>
                        <span className="text-white text-xs font-medium">{m.label}</span>
                      </div>
                    ))}
                  </div>

                  {/* Bottom nav */}
                  <div
                    className="flex justify-around items-center px-4 py-3 mt-2 border-t"
                    style={{ borderColor: "#333" }}
                  >
                    {["🏠", "🏆", "📅", "👤"].map((icon) => (
                      <span key={icon} className="text-lg">
                        {icon}
                      </span>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ── FEATURES SECTION (LAND-03) ── */}
      <section id="features" className="bg-white py-20 sm:py-28">
        <div className="max-w-6xl mx-auto px-4 sm:px-6">
          {/* Header */}
          <div className="text-center mb-14">
            <span className="text-[#DA251D] text-sm font-bold uppercase tracking-widest mb-3 block">
              Tính năng nổi bật
            </span>
            <h2 className="text-[#1A1A1A] text-3xl sm:text-4xl md:text-5xl font-extrabold mb-4">
              Tại sao chọn VietForces?
            </h2>
            <p className="text-gray-500 text-lg max-w-2xl mx-auto">
              Kết hợp game, AI và cộng đồng để tạo ra trải nghiệm học tiếng Việt hiệu quả nhất.
            </p>
          </div>

          {/* Cards grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {[
              {
                icon: "🎮",
                title: "5 Chế Độ Chơi",
                desc: "Hình ảnh → Từ, Từ → Nghĩa, Phát âm, Flashcard và Thách đấu hàng ngày",
              },
              {
                icon: "🤖",
                title: "AI Thông Minh",
                desc: "GPT-4o tạo gợi ý cá nhân hóa, giải thích từ trong ngữ cảnh thực tế",
              },
              {
                icon: "🏆",
                title: "Bảng Xếp Hạng",
                desc: "ELO rating thời gian thực, cạnh tranh lành mạnh với người chơi toàn cầu",
              },
              {
                icon: "🔥",
                title: "Streak System",
                desc: "Duy trì chuỗi ngày học liên tiếp. Streak freeze tự động giúp bạn không mất chuỗi",
              },
              {
                icon: "👥",
                title: "Kết Nối Bạn Bè",
                desc: "Follow bạn bè, xem feed hoạt động và cùng nhau tiến bộ mỗi ngày",
              },
              {
                icon: "📅",
                title: "Thách Đấu Hàng Ngày",
                desc: "Challenge mới mỗi 00:00 UTC. Hoàn thành nhận +50 bonus ELO và điểm streak",
              },
            ].map((feature) => (
              <div
                key={feature.title}
                className="bg-white rounded-2xl shadow-sm hover:shadow-md border border-gray-100 p-7 flex flex-col gap-3 transition-shadow"
              >
                <span className="text-4xl">{feature.icon}</span>
                <h3 className="text-[#1A1A1A] text-lg font-bold">{feature.title}</h3>
                <p className="text-gray-500 text-sm leading-relaxed">{feature.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── SCREENSHOTS SECTION (LAND-05) ── */}
      <section id="screenshots" className="bg-gray-50 py-20 sm:py-28 overflow-hidden">
        <div className="max-w-6xl mx-auto px-4 sm:px-6">
          {/* Header */}
          <div className="text-center mb-14">
            <span className="text-[#DA251D] text-sm font-bold uppercase tracking-widest mb-3 block">
              Giao diện
            </span>
            <h2 className="text-[#1A1A1A] text-3xl sm:text-4xl md:text-5xl font-extrabold mb-4">
              Giao Diện Ứng Dụng
            </h2>
            <p className="text-gray-500 text-lg max-w-xl mx-auto">
              Thiết kế tối giản, dễ dùng — tập trung vào trải nghiệm học tập.
            </p>
          </div>

          {/* Scrollable phone frames */}
          <div className="flex gap-6 overflow-x-auto pb-4 snap-x snap-mandatory scrollbar-hide justify-start sm:justify-center">
            {[
              {
                label: "🏠 Màn hình chính",
                bg: "linear-gradient(135deg, #DA251D 0%, #ff6b35 100%)",
              },
              {
                label: "🏆 Bảng xếp hạng",
                bg: "linear-gradient(135deg, #4338ca 0%, #7c3aed 100%)",
              },
              {
                label: "📅 Thách đấu hôm nay",
                bg: "linear-gradient(135deg, #059669 0%, #10b981 100%)",
              },
              {
                label: "👤 Hồ sơ cá nhân",
                bg: "linear-gradient(135deg, #7c3aed 0%, #a855f7 100%)",
              },
            ].map((screen) => (
              <div
                key={screen.label}
                className="flex-shrink-0 snap-center"
                style={{
                  width: "200px",
                  height: "400px",
                  borderRadius: "32px",
                  border: "8px solid #333",
                  boxShadow: "0 20px 50px rgba(0,0,0,0.2), 0 8px 20px rgba(0,0,0,0.1)",
                  overflow: "hidden",
                  background: screen.bg,
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                }}
              >
                <div className="text-center px-4">
                  <p className="text-white font-bold text-sm leading-relaxed text-center drop-shadow">
                    {screen.label}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── DOWNLOAD SECTION (LAND-04) ── */}
      <section
        id="download"
        className="py-20 sm:py-28 text-center"
        style={{ background: "#1A1A1A" }}
      >
        <div className="max-w-3xl mx-auto px-4 sm:px-6">
          <span className="text-[#FFCD00] text-sm font-bold uppercase tracking-widest mb-4 block">
            Sẵn sàng bắt đầu?
          </span>
          <h2 className="text-white text-3xl sm:text-4xl md:text-5xl font-extrabold mb-4 leading-tight">
            Bắt Đầu Học Ngay —{" "}
            <span className="text-[#FFCD00]">Miễn Phí</span>
          </h2>
          <p className="text-white/60 text-lg mb-10">
            Tải VietForces và bắt đầu hành trình học tiếng Việt của bạn ngay hôm nay.
          </p>

          <div className="flex flex-col sm:flex-row gap-4 justify-center items-center mb-8">
            {/* Google Play button */}
            <a
              href="#"
              className="inline-flex items-center gap-3 bg-white hover:bg-gray-100 text-[#1A1A1A] font-bold text-base px-6 py-4 rounded-2xl shadow-xl transition-all hover:scale-105"
            >
              {/* Play Store icon */}
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
                <path
                  d="M3 20.5v-17c0-.83 1-.92 1.5-.5l14 8.5c.5.29.5 1.21 0 1.5l-14 8.5c-.5.37-1.5.27-1.5-.5z"
                  fill="#00C853"
                />
                <path
                  d="M3 20.5L13.66 12 3 3.5v17z"
                  fill="#00BCD4"
                  opacity="0.6"
                />
                <path
                  d="M3 3.5l10.66 8.5L18.5 9.5 5.5 2C4.5 1.5 3 2 3 3.5z"
                  fill="#FFD600"
                  opacity="0.8"
                />
                <path
                  d="M3 20.5l10.66-8.5L18.5 14.5l-13 7.5c-1 .5-2.5 0-2.5-1.5z"
                  fill="#FF3D00"
                  opacity="0.8"
                />
              </svg>
              <div className="text-left">
                <div className="text-xs text-gray-500 font-normal leading-tight">Tải trên</div>
                <div className="text-sm font-bold leading-tight">Google Play</div>
              </div>
            </a>

            {/* APK direct */}
            <a
              href="#"
              className="inline-flex items-center gap-2 border-2 border-white/30 hover:border-white text-white font-semibold text-base px-6 py-4 rounded-2xl transition-all hover:bg-white/10"
            >
              <span>📦</span> Tải APK trực tiếp
            </a>
          </div>

          <p className="text-white/40 text-sm">
            Không cần thẻ tín dụng · Học mọi lúc mọi nơi · Android 7.0+
          </p>
        </div>
      </section>

      {/* ── FOOTER ── */}
      <footer style={{ background: "#111" }} className="border-t border-white/10">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 py-12">
          <div className="flex flex-col md:flex-row justify-between items-center gap-6">
            {/* Brand */}
            <div className="flex flex-col items-center md:items-start gap-1">
              <div className="flex items-center gap-2">
                <span className="text-2xl">🇻🇳</span>
                <span className="text-white font-bold text-lg">VietForces</span>
              </div>
              <span className="text-white/40 text-sm">Học tiếng Việt qua trò chơi</span>
            </div>

            {/* Links */}
            <nav className="flex flex-wrap justify-center gap-x-6 gap-y-2">
              {[
                { label: "Chính sách bảo mật", href: "#" },
                { label: "Điều khoản", href: "#" },
                { label: "Liên hệ", href: "#" },
              ].map((link) => (
                <a
                  key={link.label}
                  href={link.href}
                  className="text-white/50 hover:text-white text-sm transition-colors"
                >
                  {link.label}
                </a>
              ))}
            </nav>
          </div>

          <div className="mt-8 pt-6 border-t border-white/10 text-center">
            <p className="text-white/30 text-xs">
              © 2025 VietForces. Đồ án tốt nghiệp — HCMUS
            </p>
          </div>
        </div>
      </footer>
    </main>
  );
}
