import Link from "next/link";

type NavItem = {
  href: string;
  label: string;
  icon: string;
  active?: boolean;
};

type MobileNavProps = {
  items: NavItem[];
};

export function MobileNav({ items }: MobileNavProps) {
  return (
    <nav
      aria-label="주요 탐색"
      className="sticky bottom-4 z-20 mx-auto mt-8 w-full max-w-md rounded-full border border-slate-200 bg-white/95 px-3 py-2 shadow-[0_12px_40px_rgba(15,23,42,0.14)] backdrop-blur"
    >
      <ul className="grid grid-cols-5 gap-1">
        {items.map((item) => (
          <li key={item.href}>
            <Link
              href={item.href}
              className={`flex flex-col items-center justify-center rounded-2xl px-2 py-2 text-xs font-medium transition ${
                item.active
                  ? "bg-sky-50 text-sky-700"
                  : "text-slate-500 hover:bg-slate-50 hover:text-slate-900"
              }`}
            >
              <span aria-hidden="true" className="text-base">
                {item.icon}
              </span>
              <span className="mt-1">{item.label}</span>
            </Link>
          </li>
        ))}
      </ul>
    </nav>
  );
}
