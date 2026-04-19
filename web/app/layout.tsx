import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "중학교도우미 Web",
  description: "시간표, 급식, 일정, 타이머를 한 화면에서 보는 중학교도우미 웹앱",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko" className="h-full antialiased">
      <body className="min-h-full flex flex-col">{children}</body>
    </html>
  );
}
