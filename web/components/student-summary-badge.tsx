"use client";

/** 현재 저장된 학생 정보를 짧게 요약해 보여주는 배지 컴포넌트입니다. */

import {
  formatStudentPreferences,
} from "@/lib/storage/preferences";
import { useHydrated } from "@/hooks/use-hydrated";
import { useStudentPreferences } from "@/hooks/use-student-preferences";

export function StudentSummaryBadge() {
  const hydrated = useHydrated();
  const studentInfo = useStudentPreferences();

  const isConfigured = hydrated && Boolean(studentInfo);
  const title = hydrated
    ? formatStudentPreferences(studentInfo)
    : "학교 / 학년 / 반 확인 중...";

  return (
    <div className="rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-slate-200">
      <p className="font-medium">{title}</p>
      <p className="mt-1 text-xs text-slate-400">
        {!hydrated
          ? "브라우저 저장값을 불러오는 중입니다."
          : isConfigured
          ? "저장된 학년/반 기준으로 시간표를 조회합니다."
          : "초기 설정에서 학교 이름과 학년/반을 저장하면 실제 조회에 사용됩니다."}
      </p>
    </div>
  );
}
