"use client";

import Link from "next/link";

import {
  formatStudentPreferences,
} from "@/lib/storage/preferences";
import { useHydrated } from "@/hooks/use-hydrated";
import { useStudentPreferences } from "@/hooks/use-student-preferences";

export function StudentSetupCallout() {
  const hydrated = useHydrated();
  const studentInfo = useStudentPreferences();

  if (!hydrated) {
    return (
      <div className="rounded-3xl border border-slate-200 bg-slate-50 px-4 py-4 text-sm text-slate-600">
        브라우저 저장값을 확인하는 중입니다...
      </div>
    );
  }

  if (studentInfo) {
    return (
      <div className="rounded-3xl border border-emerald-200 bg-emerald-50 px-4 py-4 text-sm text-emerald-800">
        현재 저장된 조회 기준은 <strong>{formatStudentPreferences(studentInfo)}</strong>
        입니다. 다음 단계에서 이 값을 기준으로 실제 NEIS 데이터를 연결합니다.
      </div>
    );
  }

  return (
    <div className="rounded-3xl border border-amber-200 bg-amber-50 px-4 py-4 text-sm text-amber-900">
      <p className="font-semibold">아직 초기 설정이 필요해요.</p>
      <p className="mt-1 leading-6">
        초기 설정에서 학교 이름과 학년/반을 저장하면 대시보드와 시간표 조회에 바로 반영됩니다.
      </p>
      <Link
        href="/setup"
        className="mt-3 inline-flex rounded-full bg-amber-900 px-4 py-2 text-sm font-semibold text-white transition hover:bg-amber-800"
      >
        초기 설정 하러 가기
      </Link>
    </div>
  );
}
