"use client";

import { FormEvent, useMemo, useState } from "react";

import {
  clearStudentPreferences,
  formatStudentPreferences,
  isStudentPreferencesComplete,
  saveStudentPreferences,
} from "@/lib/storage/preferences";
import { useStudentPreferences } from "@/hooks/use-student-preferences";

export function SettingsForm() {
  const savedPreferences = useStudentPreferences();
  const [message, setMessage] = useState<string>("");
  const [error, setError] = useState<string>("");

  const currentSummary = useMemo(
    () => formatStudentPreferences(savedPreferences),
    [savedPreferences],
  );
  const formKey = savedPreferences
    ? `${savedPreferences.grade}-${savedPreferences.classroom}`
    : "empty";

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const formData = new FormData(event.currentTarget);

    const nextValue = {
      schoolName: String(formData.get("schoolName") ?? "").trim(),
      grade: String(formData.get("grade") ?? "").trim(),
      classroom: String(formData.get("classroom") ?? "").trim(),
    };

    if (!isStudentPreferencesComplete(nextValue)) {
      setError("학교 이름, 학년, 반을 모두 입력해 주세요.");
      setMessage("");
      return;
    }

    const wasSaved = saveStudentPreferences(nextValue);

    if (!wasSaved) {
      setError("브라우저 저장소에 접근하지 못했어요. 다시 시도해 주세요.");
      setMessage("");
      return;
    }

    setError("");
    setMessage("학교 이름과 학년/반을 저장했어요.");
  };

  const handleReset = () => {
    clearStudentPreferences();
    setMessage("저장된 학교/학년/반을 지웠어요.");
    setError("");
  };

  return (
    <div className="grid gap-4">
      <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-[0_18px_50px_rgba(15,23,42,0.08)]">
        <h2 className="text-base font-semibold text-slate-900">현재 저장 상태</h2>
        <p className="mt-2 text-sm text-slate-600">{currentSummary}</p>
        <p className="mt-1 text-sm text-slate-500">
          저장된 학교 이름과 학년/반은 이후 대시보드, 시간표, 일정 조회의 기준으로 사용됩니다.
        </p>
      </section>

      <form
        key={formKey}
        onSubmit={handleSubmit}
        className="rounded-3xl border border-slate-200 bg-white p-5 shadow-[0_18px_50px_rgba(15,23,42,0.08)]"
      >
        <div className="grid gap-4">
          <label className="grid gap-2 text-sm text-slate-700">
            <span className="font-medium">학교 이름</span>
            <input
              name="schoolName"
              defaultValue={savedPreferences?.schoolName ?? ""}
              placeholder="예: 미사중학교"
              className="rounded-2xl border border-slate-200 px-4 py-3 text-base text-slate-900 outline-none transition focus:border-sky-400 focus:ring-4 focus:ring-sky-100"
            />
          </label>

          <div className="grid gap-4 sm:grid-cols-2">
            <label className="grid gap-2 text-sm text-slate-700">
            <span className="font-medium">학년</span>
            <input
              name="grade"
              inputMode="numeric"
              defaultValue={savedPreferences?.grade ?? ""}
              placeholder="예: 2"
              className="rounded-2xl border border-slate-200 px-4 py-3 text-base text-slate-900 outline-none transition focus:border-sky-400 focus:ring-4 focus:ring-sky-100"
            />
          </label>

            <label className="grid gap-2 text-sm text-slate-700">
            <span className="font-medium">반</span>
            <input
              name="classroom"
              inputMode="numeric"
              defaultValue={savedPreferences?.classroom ?? ""}
              placeholder="예: 3"
              className="rounded-2xl border border-slate-200 px-4 py-3 text-base text-slate-900 outline-none transition focus:border-sky-400 focus:ring-4 focus:ring-sky-100"
            />
          </label>
          </div>
        </div>

        <p className="mt-4 text-sm text-slate-500">
          아직 저장하지 않았다면 홈 화면과 상세 조회에서 “초기 설정 필요” 상태로 보입니다.
        </p>

        {error ? (
          <p className="mt-4 rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700">
            {error}
          </p>
        ) : null}
        {message ? (
          <p className="mt-4 rounded-2xl bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
            {message}
          </p>
        ) : null}

        <div className="mt-5 flex flex-wrap gap-3">
          <button
            type="submit"
            className="rounded-full bg-slate-950 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-slate-800"
          >
            저장하기
          </button>
          <button
            type="button"
            onClick={handleReset}
            className="rounded-full border border-slate-200 px-5 py-2.5 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
          >
            저장값 지우기
          </button>
        </div>
      </form>
    </div>
  );
}
