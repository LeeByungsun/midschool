"use client";

import { useEffect, useMemo, useState } from "react";

import {
  EmptyState,
  ErrorState,
  InfoState,
  LoadingState,
  SetupRequiredState,
} from "@/components/data-state";
import { DashboardCard } from "@/components/dashboard-card";
import { useHydrated } from "@/hooks/use-hydrated";
import { formatDateKey, formatKoreanDateLabel } from "@/lib/date";
import type { MealInfo } from "@/lib/neis/types";
import {
  type CacheStatus,
  fetchMeals,
  formatCacheStatusMessage,
} from "@/lib/school-api";
import { useStudentPreferences } from "@/hooks/use-student-preferences";

type MealState = {
  requestToken: string;
  items: MealInfo[];
  error: string | null;
  cacheStatus: CacheStatus;
  cachedAt: number | null;
};

const initialState: MealState = {
  requestToken: "",
  items: [],
  error: null,
  cacheStatus: "network",
  cachedAt: null,
};

const allergyCodeMap: Record<string, string> = {
  "1": "난류",
  "2": "우유",
  "3": "메밀",
  "4": "땅콩",
  "5": "대두",
  "6": "밀",
  "7": "고등어",
  "8": "게",
  "9": "새우",
  "10": "돼지고기",
  "11": "복숭아",
  "12": "토마토",
  "13": "아황산류",
  "14": "호두",
  "15": "닭고기",
  "16": "쇠고기",
  "17": "오징어",
  "18": "조개류",
  "19": "잣",
};

function splitMealLines(value: string) {
  return value
    .split(/<br\s*\/?>/i)
    .map((line) => line.replace(/\s+/g, " ").trim())
    .filter(Boolean);
}

function parseMenuItems(menu: string) {
  return splitMealLines(menu).map((line) => {
    const codes = Array.from(
      new Set(
        Array.from(line.matchAll(/\(([\d.,\s]+)\)/g))
          .flatMap((match) =>
            match[1]
              .split(",")
              .map((value) => value.trim())
              .filter(Boolean),
          )
          .filter((code) => allergyCodeMap[code]),
      ),
    );

    return {
      name: line.replace(/\(([\d.,\s]+)\)/g, "").trim(),
      allergyLabels: codes.map((code) => allergyCodeMap[code]),
    };
  });
}

function parseInfoLines(value: string) {
  return splitMealLines(value).map((line) => line.replace(/\*+/g, "").trim());
}

function MealDetailCard({ meal }: { meal: MealInfo }) {
  const menuItems = parseMenuItems(meal.menu);
  const nutritionLines = parseInfoLines(meal.nutritionInfo);
  const originLines = parseInfoLines(meal.originInfo);

  return (
    <article className="rounded-3xl border border-slate-200 bg-white p-5 shadow-[0_18px_50px_rgba(15,23,42,0.08)]">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.24em] text-sky-700">
            {formatKoreanDateLabel(meal.date)}
          </p>
          <h3 className="mt-2 text-lg font-semibold text-slate-900">
            {meal.mealType || "급식"}
          </h3>
        </div>
        <span className="rounded-full bg-amber-50 px-3 py-1 text-xs font-semibold text-amber-700">
          {meal.calorieInfo || "칼로리 정보 없음"}
        </span>
      </div>

      <section className="mt-4">
        <h4 className="text-sm font-semibold text-slate-900">메뉴</h4>
        {menuItems.length > 0 ? (
          <ul className="mt-3 grid gap-2">
            {menuItems.map((item) => (
              <li
                key={`${meal.date}-${meal.mealType}-${item.name}`}
                className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3"
              >
                <p className="text-sm font-medium text-slate-900">{item.name}</p>
                {item.allergyLabels.length > 0 ? (
                  <p className="mt-1 text-xs text-slate-500">
                    알레르기 유발 가능 성분: {item.allergyLabels.join(", ")}
                  </p>
                ) : null}
              </li>
            ))}
          </ul>
        ) : (
          <p className="mt-3 text-sm text-slate-500">제공된 메뉴 정보가 없어요.</p>
        )}
      </section>

      <div className="mt-4 grid gap-4 lg:grid-cols-2">
        <section className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4">
          <h4 className="text-sm font-semibold text-slate-900">영양 정보</h4>
          {nutritionLines.length > 0 ? (
            <ul className="mt-3 grid gap-2">
              {nutritionLines.map((line) => (
                <li key={`${meal.date}-nutrition-${line}`} className="text-sm text-slate-600">
                  {line}
                </li>
              ))}
            </ul>
          ) : (
            <p className="mt-3 text-sm text-slate-500">제공된 영양 정보가 없어요.</p>
          )}
        </section>

        <section className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4">
          <h4 className="text-sm font-semibold text-slate-900">원산지 정보</h4>
          {originLines.length > 0 ? (
            <ul className="mt-3 grid gap-2">
              {originLines.map((line) => (
                <li key={`${meal.date}-origin-${line}`} className="text-sm text-slate-600">
                  {line}
                </li>
              ))}
            </ul>
          ) : (
            <p className="mt-3 text-sm text-slate-500">제공된 원산지 정보가 없어요.</p>
          )}
        </section>
      </div>
    </article>
  );
}

export function MealBrowser() {
  const hydrated = useHydrated();
  const studentInfo = useStudentPreferences();
  const [selectedDate, setSelectedDate] = useState(() => new Date());
  const [state, setState] = useState<MealState>(initialState);
  const [reloadCount, setReloadCount] = useState(0);

  const dateKey = useMemo(() => formatDateKey(selectedDate), [selectedDate]);
  const dateLabel = useMemo(
    () => formatKoreanDateLabel(selectedDate),
    [selectedDate],
  );
  const requestKey = studentInfo
    ? `${studentInfo.officeCode}-${studentInfo.schoolCode}-${dateKey}`
    : "";
  const requestToken = `${requestKey}:${reloadCount}`;

  useEffect(() => {
    let isCancelled = false;

    if (!hydrated || !studentInfo) {
      return;
    }

    fetchMeals({
      officeCode: studentInfo.officeCode,
      schoolCode: studentInfo.schoolCode,
      date: dateKey,
    })
      .then((result) => {
        if (isCancelled) {
          return;
        }

        setState({
          requestToken,
          items: result.items,
          error: null,
          cacheStatus: result.cacheStatus,
          cachedAt: result.cachedAt,
        });
      })
      .catch((error: unknown) => {
        if (isCancelled) {
          return;
        }

        setState({
          requestToken,
          items: [],
          error:
            error instanceof Error
              ? error.message
              : "급식 정보를 불러오지 못했어요.",
          cacheStatus: "network",
          cachedAt: null,
        });
      });

    return () => {
      isCancelled = true;
    };
  }, [dateKey, hydrated, requestToken, studentInfo]);

  const isLoading = hydrated && Boolean(studentInfo) && state.requestToken !== requestToken;

  const moveDate = (offset: number) => {
    setSelectedDate((prev) => {
      const next = new Date(prev);
      next.setDate(prev.getDate() + offset);
      return next;
    });
  };

  const jumpToToday = () => {
    setSelectedDate(new Date());
  };

  const retryFetch = () => {
    setReloadCount((prev) => prev + 1);
  };
  const cacheNotice = formatCacheStatusMessage(
    state.cacheStatus,
    state.cachedAt,
    "급식",
  );

  const selectedMeals = state.items.filter((meal) => meal.date === dateKey);
  const visibleMeals = selectedMeals.length > 0 ? selectedMeals : state.items;
  const summaryDate =
    visibleMeals[0]?.date ? formatKoreanDateLabel(visibleMeals[0].date) : dateLabel;

  return (
    <DashboardCard
      title="급식 상세"
      subtitle={`${summaryDate} 기준`}
      action={
        <div className="flex flex-wrap items-center gap-2">
          <button
            type="button"
            onClick={() => moveDate(-1)}
            className="rounded-full border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-700 transition hover:bg-slate-50"
          >
            이전 날
          </button>
          <button
            type="button"
            onClick={jumpToToday}
            className="rounded-full border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-700 transition hover:bg-slate-50"
          >
            오늘
          </button>
          <button
            type="button"
            onClick={() => moveDate(1)}
            className="rounded-full border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-700 transition hover:bg-slate-50"
          >
            다음 날
          </button>
        </div>
      }
    >
      {!hydrated ? (
        <LoadingState message="브라우저 날짜를 맞추는 중..." />
      ) : !studentInfo ? (
        <SetupRequiredState message="급식 상세를 보려면 학교 이름과 학년/반을 먼저 저장해 주세요." />
      ) : isLoading ? (
        <LoadingState message="선택한 날짜의 급식을 불러오는 중..." />
      ) : state.error ? (
        <ErrorState message={state.error} onRetry={retryFetch} />
      ) : visibleMeals.length === 0 ? (
        <EmptyState
          title="급식 정보가 없어요."
          message="선택한 날짜에는 표시할 급식 정보가 없어요."
        />
      ) : (
        <div className="grid gap-4">
          {cacheNotice ? <InfoState message={cacheNotice} /> : null}
          {visibleMeals.map((meal) => (
            <MealDetailCard
              key={`${meal.date}-${meal.mealType}-${meal.calorieInfo}`}
              meal={meal}
            />
          ))}
        </div>
      )}

      {visibleMeals[0]?.date && visibleMeals[0].date !== dateKey ? (
        <p className="mt-4 text-sm text-slate-500">
          요청한 날짜와 정확히 일치하는 급식이 없어, 제공된 급식 데이터를 대신 표시하고 있어요.
        </p>
      ) : null}
    </DashboardCard>
  );
}
