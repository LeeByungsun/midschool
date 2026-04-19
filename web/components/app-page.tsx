import { ReactNode } from "react";

import { MobileNav } from "@/components/mobile-nav";
import { StudentSummaryBadge } from "@/components/student-summary-badge";
import { navigationItems } from "@/lib/site-data";

type AppPageProps = {
  title: string;
  description: string;
  activePath: string;
  children: ReactNode;
};

export function AppPage({
  title,
  description,
  activePath,
  children,
}: AppPageProps) {
  return (
    <main className="mx-auto flex min-h-screen w-full max-w-6xl flex-col px-4 pb-28 pt-6 sm:px-6 lg:px-8">
      <header className="rounded-[2rem] bg-slate-950 px-6 py-6 text-white shadow-[0_24px_60px_rgba(15,23,42,0.28)]">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.28em] text-sky-200">
              중학교도우미 Web
            </p>
            <h1 className="mt-2 text-2xl font-semibold sm:text-3xl">{title}</h1>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-300 sm:text-base">
              {description}
            </p>
          </div>
          <StudentSummaryBadge />
        </div>
      </header>

      <section className="mt-6 flex-1">{children}</section>

      <MobileNav
        items={navigationItems.map((item) => ({
          ...item,
          active: item.href === activePath,
        }))}
      />
    </main>
  );
}
