"use client";

/** 학생 기본 정보와 학교 선택값을 저장하는 설정 폼 컴포넌트입니다. */

import { FormEvent, KeyboardEvent, useMemo, useState } from "react";
import { useRouter } from "next/navigation";

import type { SchoolInfo } from "@/lib/neis/types";
import { fetchSchoolByCode, fetchSchools } from "@/lib/school-api";
import {
  clearStudentPreferences,
  formatStudentPreferences,
  isStudentPreferencesComplete,
  saveStudentPreferences,
  type StudentPreferences,
} from "@/lib/storage/preferences";
import { useStudentPreferences } from "@/hooks/use-student-preferences";

type SettingsFormBodyProps = {
  initialPreferences: StudentPreferences | null;
  onSaved: (message: string) => void;
  onError: (message: string) => void;
  onCleared: (message: string) => void;
  redirectToOnSave?: string;
};

function SettingsFormBody({
  initialPreferences,
  onSaved,
  onError,
  onCleared,
  redirectToOnSave,
}: SettingsFormBodyProps) {
  const router = useRouter();
  const schoolNameHintId = "school-name-hint";
  const schoolSearchMessageId = "school-search-message";
  const schoolResultsLabelId = "school-results-label";
  const gradeHintId = "grade-hint";
  const classroomHintId = "classroom-hint";
  const [schoolQuery, setSchoolQuery] = useState(initialPreferences?.schoolName ?? "");
  const [selectedSchool, setSelectedSchool] = useState<SchoolInfo | null>(
    initialPreferences
      ? {
          officeCode: initialPreferences.officeCode,
          officeName: "",
          schoolCode: initialPreferences.schoolCode,
          schoolName: initialPreferences.schoolName,
          schoolKind: initialPreferences.schoolKind ?? "중학교",
          location: "",
          jurisdiction: "",
          foundation: "",
          roadAddress: "",
          telephone: "",
          homepage: initialPreferences.homepage ?? "",
        }
      : null,
  );
  const [grade, setGrade] = useState(initialPreferences?.grade ?? "");
  const [classroom, setClassroom] = useState(initialPreferences?.classroom ?? "");
  const [searchResults, setSearchResults] = useState<SchoolInfo[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [searchMessage, setSearchMessage] = useState("");

  const handleSearch = async () => {
    const query = schoolQuery.trim();

    if (query.length < 2) {
      setSearchMessage("학교 이름은 두 글자 이상 입력해 주세요.");
      setSearchResults([]);
      setSelectedSchool(null);
      return;
    }

    setIsSearching(true);
    setSearchMessage("");
    onError("");
    onSaved("");

    try {
      const items = await fetchSchools({ query });
      setSearchResults(items);

      if (items.length === 0) {
        setSelectedSchool(null);
        setSearchMessage(
          "검색된 초등학교/중학교가 없어요. 학교 이름을 다시 확인해 주세요.",
        );
      } else if (items.length === 1) {
        setSelectedSchool(items[0]);
        setSchoolQuery(items[0].schoolName);
        setSearchMessage("학교를 찾았어요. 학년과 반을 입력해 저장해 주세요.");
      } else {
        setSelectedSchool(null);
        setSearchMessage("검색 결과에서 학교를 선택해 주세요.");
      }
    } catch (error) {
      setSearchResults([]);
      setSelectedSchool(null);
      onError(
        error instanceof Error ? error.message : "학교 검색 중 오류가 발생했어요.",
      );
    } finally {
      setIsSearching(false);
    }
  };

  const handleSchoolSelect = (school: SchoolInfo) => {
    setSelectedSchool(school);
    setSchoolQuery(school.schoolName);
    setSearchMessage(`${school.schoolName}를 선택했어요.`);
    onError("");
  };

  const handleSchoolQueryKeyDown = async (
    event: KeyboardEvent<HTMLInputElement>,
  ) => {
    if (event.key !== "Enter" || event.nativeEvent.isComposing) {
      return;
    }

    event.preventDefault();
    await handleSearch();
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    let homepage = selectedSchool?.homepage?.trim() ?? "";

    if (!homepage && selectedSchool?.officeCode && selectedSchool?.schoolCode) {
      try {
        const schoolDetail = await fetchSchoolByCode({
          officeCode: selectedSchool.officeCode,
          schoolCode: selectedSchool.schoolCode,
        });

        homepage = schoolDetail?.homepage?.trim() ?? "";
      } catch {
        homepage = "";
      }
    }

    const nextValue: StudentPreferences = {
      schoolName: selectedSchool?.schoolName ?? schoolQuery.trim(),
      officeCode: selectedSchool?.officeCode ?? "",
      schoolCode: selectedSchool?.schoolCode ?? "",
      schoolKind: selectedSchool?.schoolKind ?? "",
      homepage,
      grade: grade.trim(),
      classroom: classroom.trim(),
    };

    if (!selectedSchool) {
      onError("학교 검색 후 목록에서 학교를 선택해 주세요.");
      onSaved("");
      return;
    }

    if (!isStudentPreferencesComplete(nextValue)) {
      onError("학교, 학년, 반 정보를 모두 준비해 주세요.");
      onSaved("");
      return;
    }

    const wasSaved = saveStudentPreferences(nextValue);

    if (!wasSaved) {
      onError("브라우저 저장소에 접근하지 못했어요. 다시 시도해 주세요.");
      onSaved("");
      return;
    }

    onError("");
    onSaved("학교, 학년, 반을 저장했어요.");

    if (redirectToOnSave) {
      router.push(redirectToOnSave);
    }
  };

  const handleReset = () => {
    clearStudentPreferences();
    setSchoolQuery("");
    setSelectedSchool(null);
    setGrade("");
    setClassroom("");
    setSearchResults([]);
    setSearchMessage("");
    onError("");
    onCleared("저장된 초기 설정을 지웠어요.");
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="rounded-3xl border border-slate-200 bg-white p-5 shadow-[0_18px_50px_rgba(15,23,42,0.08)]"
    >
      <div className="grid gap-4">
        <div className="grid gap-2 text-sm text-slate-700">
          <label htmlFor="schoolName" className="font-medium">
            학교 이름
          </label>
          <div className="flex flex-col gap-3 sm:flex-row">
            <input
              id="schoolName"
              value={schoolQuery}
              onChange={(event) => {
                setSchoolQuery(event.target.value);
                setSelectedSchool(null);
              }}
              onKeyDown={handleSchoolQueryKeyDown}
              placeholder="예: 미사초등학교 / 미사중학교"
              aria-describedby={[
                schoolNameHintId,
                searchMessage ? schoolSearchMessageId : "",
              ]
                .filter(Boolean)
                .join(" ")}
              className="flex-1 rounded-2xl border border-slate-200 px-4 py-3 text-base text-slate-900 outline-none transition focus:border-sky-400 focus:ring-4 focus:ring-sky-100"
            />
            <button
              type="button"
              onClick={handleSearch}
              className="rounded-full bg-slate-950 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800"
            >
              {isSearching ? "검색 중..." : "학교 검색"}
            </button>
          </div>
          <p id={schoolNameHintId} className="text-sm text-slate-500">
            현재는 초등학교와 중학교 검색 결과를 표시합니다.
          </p>
        </div>

        {searchMessage ? (
          <p
            id={schoolSearchMessageId}
            className="rounded-2xl bg-slate-50 px-4 py-3 text-sm text-slate-600"
            role="status"
            aria-live="polite"
          >
            {searchMessage}
          </p>
        ) : null}

        {searchResults.length > 0 ? (
          <div className="grid gap-3">
            <p id={schoolResultsLabelId} className="text-sm font-medium text-slate-700">
              검색 결과
            </p>
            <ul className="grid gap-3" aria-labelledby={schoolResultsLabelId}>
              {searchResults.map((school) => {
                const isSelected =
                  selectedSchool?.officeCode === school.officeCode &&
                  selectedSchool?.schoolCode === school.schoolCode;

                return (
                  <li key={`${school.officeCode}-${school.schoolCode}`}>
                    <button
                      type="button"
                      onClick={() => handleSchoolSelect(school)}
                      aria-pressed={isSelected}
                      aria-label={
                        isSelected
                          ? `${school.schoolName} 선택됨`
                          : `${school.schoolName} 선택`
                      }
                      className={`w-full rounded-2xl border px-4 py-3 text-left transition ${
                        isSelected
                          ? "border-sky-300 bg-sky-50"
                          : "border-slate-200 bg-slate-50 hover:border-slate-300 hover:bg-white"
                      }`}
                    >
                      <p className="text-sm font-semibold text-slate-900">
                        {school.schoolName}
                      </p>
                      <p className="mt-1 text-sm text-slate-500">
                        {school.officeName || school.location}
                        {school.schoolKind ? ` · ${school.schoolKind}` : ""}
                      </p>
                      {school.roadAddress ? (
                        <p className="mt-1 text-sm text-slate-500">{school.roadAddress}</p>
                      ) : null}
                    </button>
                  </li>
                );
              })}
            </ul>
          </div>
        ) : null}

        <div className="grid gap-4 sm:grid-cols-2">
          <label className="grid gap-2 text-sm text-slate-700">
            <span className="font-medium">학년</span>
            <input
              id="grade"
              name="grade"
              inputMode="numeric"
              value={grade}
              onChange={(event) => setGrade(event.target.value)}
              placeholder="예: 2"
              aria-describedby={gradeHintId}
              className="rounded-2xl border border-slate-200 px-4 py-3 text-base text-slate-900 outline-none transition focus:border-sky-400 focus:ring-4 focus:ring-sky-100"
            />
            <span id={gradeHintId} className="text-xs text-slate-500">
              숫자만 입력해 주세요. 예: 1, 2, 3
            </span>
          </label>

          <label className="grid gap-2 text-sm text-slate-700">
            <span className="font-medium">반</span>
            <input
              id="classroom"
              name="classroom"
              inputMode="numeric"
              value={classroom}
              onChange={(event) => setClassroom(event.target.value)}
              placeholder="예: 3"
              aria-describedby={classroomHintId}
              className="rounded-2xl border border-slate-200 px-4 py-3 text-base text-slate-900 outline-none transition focus:border-sky-400 focus:ring-4 focus:ring-sky-100"
            />
            <span id={classroomHintId} className="text-xs text-slate-500">
              숫자만 입력해 주세요. 예: 1, 2, 3
            </span>
          </label>
        </div>

        <p className="text-sm text-slate-500">
          저장된 학교 코드와 학년/반을 기준으로 시간표, 급식, 일정 조회에 사용합니다.
        </p>

        <div className="mt-1 flex flex-wrap gap-3">
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
      </div>
    </form>
  );
}

type SettingsFormProps = {
  redirectToOnSave?: string;
};

export function SettingsForm({ redirectToOnSave }: SettingsFormProps) {
  const savedPreferences = useStudentPreferences();
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const currentSummary = useMemo(
    () => formatStudentPreferences(savedPreferences),
    [savedPreferences],
  );
  const formKey = savedPreferences
    ? `${savedPreferences.officeCode}-${savedPreferences.schoolCode}-${savedPreferences.grade}-${savedPreferences.classroom}`
    : "empty";

  return (
    <div className="grid gap-4">
      <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-[0_18px_50px_rgba(15,23,42,0.08)]">
        <h2 className="text-base font-semibold text-slate-900">현재 저장 상태</h2>
        <p className="mt-2 text-sm text-slate-600">{currentSummary}</p>
        <p className="mt-1 text-sm text-slate-500">
          저장된 학교 이름과 학년/반은 이후 대시보드, 시간표, 일정 조회의 기준으로 사용됩니다.
        </p>
      </section>

      {error ? (
        <p
          className="rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700"
          role="alert"
          aria-live="assertive"
        >
          {error}
        </p>
      ) : null}
      {message ? (
        <p
          className="rounded-2xl bg-emerald-50 px-4 py-3 text-sm text-emerald-700"
          role="status"
          aria-live="polite"
        >
          {message}
        </p>
      ) : null}

      <SettingsFormBody
        key={formKey}
        initialPreferences={savedPreferences}
        onSaved={setMessage}
        onError={setError}
        onCleared={setMessage}
        redirectToOnSave={redirectToOnSave}
      />
    </div>
  );
}
