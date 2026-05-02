/** 모든 페이지에 공통으로 적용되는 메타데이터와 루트 레이아웃을 구성합니다. */

import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "학교도우미",
  description: "시간표, 급식, 일정, 타이머를 한 화면에서 보는 학교도우미 웹앱",
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
