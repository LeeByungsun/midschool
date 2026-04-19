import Link from "next/link";
import { ReactNode } from "react";

type StateTone = "neutral" | "warning" | "danger";

type StateNoticeProps = {
  title?: string;
  message: string;
  tone?: StateTone;
  action?: ReactNode;
};

const toneClasses: Record<StateTone, string> = {
  neutral: "border-slate-200 bg-slate-50 text-slate-700",
  warning: "border-amber-200 bg-amber-50 text-amber-900",
  danger: "border-rose-200 bg-rose-50 text-rose-800",
};

function StateNotice({
  title,
  message,
  tone = "neutral",
  action,
}: StateNoticeProps) {
  return (
    <div className={`rounded-3xl border px-4 py-4 ${toneClasses[tone]}`}>
      {title ? <p className="font-semibold">{title}</p> : null}
      <p className={`text-sm leading-7 ${title ? "mt-1" : ""}`}>{message}</p>
      {action ? <div className="mt-3">{action}</div> : null}
    </div>
  );
}

export function LoadingState({ message }: { message: string }) {
  return <StateNotice message={message} />;
}

export function EmptyState({
  title = "표시할 내용이 없어요.",
  message,
}: {
  title?: string;
  message: string;
}) {
  return <StateNotice title={title} message={message} />;
}

export function ErrorState({
  message,
  onRetry,
  retryLabel = "다시 시도",
}: {
  message: string;
  onRetry?: () => void;
  retryLabel?: string;
}) {
  return (
    <StateNotice
      title="불러오는 중 문제가 발생했어요."
      message={message}
      tone="danger"
      action={
        onRetry ? (
          <button
            type="button"
            onClick={onRetry}
            className="inline-flex rounded-full bg-rose-700 px-4 py-2 text-sm font-semibold text-white transition hover:bg-rose-800"
          >
            {retryLabel}
          </button>
        ) : null
      }
    />
  );
}

export function SetupRequiredState({
  title = "먼저 초기 설정이 필요해요.",
  message,
  href = "/setup",
  ctaLabel = "초기 설정 하러 가기",
}: {
  title?: string;
  message: string;
  href?: string;
  ctaLabel?: string;
}) {
  return (
    <StateNotice
      title={title}
      message={message}
      tone="warning"
      action={
        <Link
          href={href}
          className="inline-flex rounded-full bg-amber-900 px-4 py-2 text-sm font-semibold text-white transition hover:bg-amber-800"
        >
          {ctaLabel}
        </Link>
      }
    />
  );
}

export function InfoState({
  title,
  message,
}: {
  title?: string;
  message: string;
}) {
  return <StateNotice title={title} message={message} tone="neutral" />;
}
