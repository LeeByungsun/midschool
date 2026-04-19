"use client";

import {
  formatStudentPreferences,
} from "@/lib/storage/preferences";
import { useStudentPreferences } from "@/hooks/use-student-preferences";

export function StudentSummaryBadge() {
  const studentInfo = useStudentPreferences();

  const isConfigured = Boolean(studentInfo);

  return (
    <div className="rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-slate-200">
      <p className="font-medium">{formatStudentPreferences(studentInfo)}</p>
      <p className="mt-1 text-xs text-slate-400">
        {isConfigured
          ? "저장된 학년/반 기준으로 시간표를 조회합니다."
          : "설정에서 학년/반을 저장하면 실제 조회에 사용됩니다."}
      </p>
    </div>
  );
}
