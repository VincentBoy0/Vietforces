import type { Metadata, Viewport } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "VietForces — Học tiếng Việt qua trò chơi",
  description:
    "Ứng dụng học từ vựng tiếng Việt gamified với AI, bảng xếp hạng ELO và thách đấu hàng ngày. Miễn phí trên Android.",
  keywords: ["học tiếng Việt", "vietnamese learning", "gamified", "ELO", "VietForces"],
  openGraph: {
    title: "VietForces — Học tiếng Việt qua trò chơi",
    description:
      "Ứng dụng học từ vựng tiếng Việt gamified với AI, bảng xếp hạng ELO và thách đấu hàng ngày.",
    type: "website",
  },
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="vi">
      <body>{children}</body>
    </html>
  );
}
