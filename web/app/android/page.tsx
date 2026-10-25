import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";

const playStoreUrl =
  "https://play.google.com/store/apps/details?id=com.lbs.schoolhelper&hl=ko";

const screenshots = [
  { src: "/android/screen1.jpg", title: "홈", description: "급식과 다가오는 학교 일정을 한눈에" },
  { src: "/android/screen2.jpg", title: "시간표", description: "우리 반의 하루 수업을 빠르게" },
  { src: "/android/screen3.jpg", title: "급식", description: "요일별 메뉴와 영양 정보까지" },
  { src: "/android/screen4.jpg", title: "학사 일정", description: "이번 달 학교 행사를 놓치지 않게" },
] as const;

export const metadata: Metadata = {
  title: "스쿨온 Android 앱 | 학교 급식·시간표·학사 일정",
  description:
    "스쿨온 Android 앱에서 우리 학교 급식, 시간표, 학사 일정과 가정통신문을 확인하세요. 여러 자녀 프로필, 홈 화면 위젯, 집중 타이머도 사용할 수 있습니다.",
  alternates: { canonical: "/android" },
  openGraph: {
    title: "스쿨온 Android 앱 | 우리 학교 생활을 한눈에",
    description: "급식, 시간표, 학사 일정, 가정통신문을 한곳에서. Google Play에서 스쿨온을 만나보세요.",
    url: "/android",
    type: "website",
    locale: "ko_KR",
  },
};

function PlayStoreLink({ secondary = false }: { secondary?: boolean }) {
  return (
    <a
      href={playStoreUrl}
      target="_blank"
      rel="noopener noreferrer"
      className={
        secondary
          ? "inline-flex min-h-12 items-center justify-center rounded-full bg-white px-7 py-3 text-base font-bold text-blue-800 shadow-sm transition hover:bg-blue-50 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-white"
          : "inline-flex min-h-12 items-center justify-center rounded-full bg-blue-700 px-7 py-3 text-base font-bold text-white shadow-lg shadow-blue-900/20 transition hover:bg-blue-800 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-blue-700"
      }
    >
      Google Play에서 다운로드 <span aria-hidden="true" className="ml-2">↗</span>
    </a>
  );
}

export default function AndroidPage() {
  return (
    <main className="min-h-screen bg-[#f5f8fd] text-slate-900">
      <div className="mx-auto max-w-7xl px-5 pb-20 sm:px-8">
        <header className="flex items-center justify-between gap-4 py-5 sm:py-7">
          <Link href="/" className="flex items-center gap-3 rounded-lg focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-blue-700" aria-label="스쿨온 웹 홈으로 이동">
            <Image src="/android/icon.png" alt="" width={44} height={44} className="rounded-xl" />
            <span className="text-xl font-extrabold tracking-tight">스쿨온</span>
          </Link>
          <Link href="/" className="rounded-full px-3 py-2 text-sm font-semibold text-slate-600 hover:bg-white hover:text-blue-700 focus-visible:outline-2 focus-visible:outline-blue-700">
            웹에서 이용하기 <span aria-hidden="true">↗</span>
          </Link>
        </header>

        <section className="relative isolate overflow-hidden rounded-[2rem] bg-gradient-to-br from-[#eaf3ff] via-white to-[#d7eaff] px-6 pb-10 pt-12 sm:px-10 lg:min-h-[650px] lg:px-16 lg:py-16">
          <div aria-hidden="true" className="pointer-events-none absolute -right-24 -top-28 -z-10 size-96 rounded-full bg-blue-200/70 blur-3xl" />
          <div aria-hidden="true" className="pointer-events-none absolute -bottom-44 left-1/3 -z-10 size-96 rounded-full bg-sky-200/70 blur-3xl" />
          <div className="grid items-center gap-12 lg:grid-cols-[1fr_0.9fr]">
            <div className="max-w-xl">
              <p className="mb-5 inline-flex rounded-full border border-blue-200 bg-white/80 px-4 py-2 text-sm font-bold text-blue-700">
                Android 앱 · 스쿨온
              </p>
              <h1 className="text-4xl font-extrabold leading-[1.18] tracking-tight text-slate-950 sm:text-5xl lg:text-6xl">
                우리 학교의 오늘,<br />
                <span className="text-blue-700">한눈에 확인해요.</span>
              </h1>
              <p className="mt-6 max-w-lg text-base leading-8 text-slate-600 sm:text-lg">
                급식, 시간표, 학사 일정부터 가정통신문까지. 필요한 학교 정보를 스쿨온에서 편하게 찾아보세요.
              </p>
              <div className="mt-8 flex flex-wrap items-center gap-4">
                <PlayStoreLink />
                <span className="text-sm text-slate-500">초·중·고 학교 생활 정보</span>
              </div>
            </div>
            <div className="relative mx-auto w-full max-w-[280px] sm:max-w-[340px] lg:max-w-[365px]">
              <div aria-hidden="true" className="absolute inset-8 translate-x-5 translate-y-6 rounded-[2.5rem] bg-blue-600/20 blur-2xl" />
              <div className="relative overflow-hidden rounded-[2.5rem] border-[8px] border-slate-900 bg-slate-900 shadow-[0_30px_80px_rgba(30,64,175,0.28)]">
                <Image
                  src="/android/screen1.jpg"
                  alt="스쿨온 Android 앱 홈 화면: 선택한 학교와 오늘 급식, 학사 일정"
                  width={1080}
                  height={2340}
                  priority
                  className="h-auto w-full"
                />
              </div>
              <p className="mt-4 text-center text-xs text-slate-500">예시 화면 · 학교별 제공 정보는 다를 수 있습니다.</p>
            </div>
          </div>
        </section>

        <section className="py-16 sm:py-20" aria-labelledby="features-title">
          <div className="max-w-2xl">
            <p className="text-sm font-bold text-blue-700">학교 생활, 더 간편하게</p>
            <h2 id="features-title" className="mt-3 text-3xl font-extrabold tracking-tight sm:text-4xl">필요한 정보만 빠르게</h2>
            <p className="mt-4 leading-7 text-slate-600">학교를 선택하면 매일 확인하는 정보를 한곳에 모아 보여줍니다.</p>
          </div>
          <div className="mt-8 grid gap-4 md:grid-cols-2 lg:grid-cols-4">
            {[
              { icon: "📚", title: "우리 반 시간표", body: "학년과 반을 설정해 날짜별 수업을 확인해요." },
              { icon: "🍱", title: "오늘과 이번 주 급식", body: "메뉴부터 알레르기·영양 정보까지 살펴봐요." },
              { icon: "🗓️", title: "학사 일정·가정통신문", body: "학교 일정과 지원되는 학교의 새 소식을 확인해요." },
              { icon: "⏱️", title: "집중 타이머", body: "공부와 쉬는 시간을 나눠 집중 흐름을 이어가요." },
            ].map((feature) => (
              <div key={feature.title} className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
                <span aria-hidden="true" className="text-3xl">{feature.icon}</span>
                <h3 className="mt-5 text-lg font-bold">{feature.title}</h3>
                <p className="mt-3 text-sm leading-6 text-slate-600">{feature.body}</p>
              </div>
            ))}
          </div>
        </section>

        <section className="rounded-[2rem] bg-white px-6 py-12 sm:px-10" aria-labelledby="screenshots-title">
          <div className="flex flex-wrap items-end justify-between gap-4">
            <div>
              <p className="text-sm font-bold text-blue-700">앱 미리보기</p>
              <h2 id="screenshots-title" className="mt-3 text-3xl font-extrabold tracking-tight sm:text-4xl">스쿨온 화면 둘러보기</h2>
            </div>
            <p className="max-w-sm text-sm leading-6 text-slate-500">화면의 학교와 날짜는 예시이며, 실제 정보는 선택한 학교에 따라 달라집니다.</p>
          </div>
          <div className="mt-10 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
            {screenshots.map((screen) => (
              <figure key={screen.src} className="min-w-0">
                <div className="overflow-hidden rounded-[1.7rem] border-4 border-slate-900 bg-slate-100 shadow-lg">
                  <Image
                    src={screen.src}
                    alt={`스쿨온 Android 앱 ${screen.title} 화면`}
                    width={1080}
                    height={2340}
                    className="h-auto w-full"
                  />
                </div>
                <figcaption className="mt-4 text-center">
                  <strong className="block text-base">{screen.title}</strong>
                  <span className="mt-1 block text-sm text-slate-500">{screen.description}</span>
                </figcaption>
              </figure>
            ))}
          </div>
        </section>

        <section className="mt-16 flex flex-col items-start justify-between gap-6 rounded-[2rem] bg-blue-800 px-7 py-10 text-white sm:px-10 lg:flex-row lg:items-center" aria-labelledby="download-title">
          <div>
            <h2 id="download-title" className="text-2xl font-extrabold sm:text-3xl">오늘의 학교 생활, 스쿨온에서 시작하세요.</h2>
            <p className="mt-3 text-sm leading-6 text-blue-100">Android 기기에서 Google Play를 통해 설치할 수 있습니다.</p>
          </div>
          <PlayStoreLink secondary />
        </section>

        <footer className="flex flex-wrap items-center justify-between gap-4 pt-10 text-sm text-slate-500">
          <span>© 스쿨온</span>
          <div className="flex flex-wrap gap-x-6 gap-y-2">
            <Link href="/">웹에서 이용하기</Link>
            <a href="https://leebyungsun.github.io/schoolon-privacy/" target="_blank" rel="noopener noreferrer">개인정보처리방침</a>
          </div>
        </footer>
      </div>
    </main>
  );
}
