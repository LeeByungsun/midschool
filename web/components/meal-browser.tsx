"use client";

/** 홈 급식 카드에서 이동한 뒤 일주일치 급식을 보여주는 클라이언트 컴포넌트입니다. */

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
import {
  formatDateKey,
  formatKoreanDateLabel,
  formatKoreanDateRange,
  getWeekDates,
} from "@/lib/date";
import type { MealInfo } from "@/lib/neis/types";
import {
  type CacheStatus,
  fetchMeals,
  formatCacheStatusMessage,
  readCachedMeals,
  shouldUpdateListFromNetwork,
} from "@/lib/school-api";
import { useStudentPreferences } from "@/hooks/use-student-preferences";

type MealState = {
  requestToken: string;
  items: MealInfo[];
  error: string | null;
  cacheStatus: CacheStatus;
  cachedAt: number | null;
};

type MealDetailCardProps = {
  meal: MealInfo;
  showDate?: boolean;
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

function MealDetailCard({ meal, showDate = true }: MealDetailCardProps) {
  const menuItems = parseMenuItems(meal.menu);
  const nutritionLines = parseInfoLines(meal.nutritionInfo);
  const originLines = parseInfoLines(meal.originInfo);

  return (
    <article className="rounded-3xl border border-slate-200 bg-white p-5 shadow-[0_18px_50px_rgba(15,23,42,0.08)]">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          {showDate ? (
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-sky-700">
              {formatKoreanDateLabel(meal.date)}
            </p>
          ) : null}
          <h3 className={`${showDate ? "mt-2 " : ""}text-lg font-semibold text-slate-900`}>
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

  const weekDates = useMemo(() => getWeekDates(selectedDate), [selectedDate]);
  const weekStart = weekDates[0];
  const weekEnd = weekDates[weekDates.length - 1];
  const weekStartKey = useMemo(() => formatDateKey(weekStart), [weekStart]);
  const weekEndKey = useMemo(() => formatDateKey(weekEnd), [weekEnd]);
  const weekLabel = useMemo(
    () => formatKoreanDateRange(weekStart, weekEnd),
    [weekEnd, weekStart],
  );
  const requestKey = studentInfo
    ? `${studentInfo.officeCode}-${studentInfo.schoolCode}-${weekStartKey}-${weekEndKey}`
    : "";
  const requestToken = `${requestKey}:${reloadCount}`;

  useEffect(() => {
    let isCancelled = false;

    if (!hydrated || !studentInfo) {
      return;
    }

    const params = {
      officeCode: studentInfo.officeCode,
      schoolCode: studentInfo.schoolCode,
      date: weekStartKey,
      endDate: weekEndKey,
    };
    const cached = readCachedMeals(params);

    if (cached) {
      queueMicrotask(() => {
        if (isCancelled) {
          return;
        }

        setState({
          requestToken,
          items: cached.items,
          error: null,
          cacheStatus: cached.cacheStatus,
          cachedAt: cached.cachedAt,
        });
      });
    }

    fetchMeals(params, { skipFreshCache: true })
      .then((result) => {
        if (isCancelled) {
          return;
        }

        if (cached && !shouldUpdateListFromNetwork(cached.items, result.items)) {
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
  }, [hydrated, requestToken, studentInfo, weekEndKey, weekStartKey]);

  const isLoading = hydrated && Boolean(studentInfo) && state.requestToken !== requestToken;

  const moveWeek = (offset: number) => {
    setSelectedDate((prev) => {
      const next = new Date(prev);
      next.setDate(prev.getDate() + offset);
      return next;
    });
  };

  const jumpToCurrentWeek = () => {
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

  const weekMealGroups = useMemo(() => {
    const sortedMeals = [...state.items].sort(
      (left, right) =>
        left.date.localeCompare(right.date) ||
        left.mealType.localeCompare(right.mealType),
    );

    return weekDates.map((date) => {
      const dateKey = formatDateKey(date);

      return {
        date,
        dateKey,
        meals: sortedMeals.filter((meal) => meal.date === dateKey),
      };
    });
  }, [state.items, weekDates]);
  const hasAnyMeal = weekMealGroups.some((group) => group.meals.length > 0);

  return (
    <DashboardCard
      title="주간 급식 상세"
      subtitle={`${weekLabel} 기준`}
      action={
        <div className="flex flex-wrap items-center gap-2">
          <button
            type="button"
            onClick={() => moveWeek(-7)}
            aria-label="급식 주간 범위를 한 주 이전으로 이동"
            className="rounded-full border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-700 transition hover:bg-slate-50"
          >
            이전 주
          </button>
          <button
            type="button"
            onClick={jumpToCurrentWeek}
            aria-label="급식 주간 범위를 이번 주로 이동"
            className="rounded-full border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-700 transition hover:bg-slate-50"
          >
            이번 주
          </button>
          <button
            type="button"
            onClick={() => moveWeek(7)}
            aria-label="급식 주간 범위를 한 주 다음으로 이동"
            className="rounded-full border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-700 transition hover:bg-slate-50"
          >
            다음 주
          </button>
        </div>
      }
    >
      {!hydrated ? (
        <LoadingState message="브라우저 날짜를 맞추는 중..." />
      ) : !studentInfo ? (
        <SetupRequiredState message="급식 상세를 보려면 학교 이름과 학년/반을 먼저 저장해 주세요." />
      ) : isLoading ? (
        <LoadingState message="선택한 주의 급식을 불러오는 중..." />
      ) : state.error ? (
        <ErrorState message={state.error} onRetry={retryFetch} />
      ) : !hasAnyMeal ? (
        <EmptyState
          title="급식 정보가 없어요."
          message="선택한 주에는 표시할 급식 정보가 없어요."
        />
      ) : (
        <div className="grid gap-4">
          {cacheNotice ? <InfoState message={cacheNotice} /> : null}
          {weekMealGroups.map((group) => (
            <section key={group.dateKey} className="grid gap-3">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <h3 className="text-sm font-semibold text-slate-900">
                  {formatKoreanDateLabel(group.date)}
                </h3>
                <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">
                  {group.meals.length > 0 ? `${group.meals.length}식` : "급식 없음"}
                </span>
              </div>

              {group.meals.length > 0 ? (
                group.meals.map((meal) => (
                  <MealDetailCard
                    key={`${meal.date}-${meal.mealType}-${meal.calorieInfo}`}
                    meal={meal}
                    showDate={false}
                  />
                ))
              ) : (
                <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-4 py-5 text-sm text-slate-500">
                  해당 날짜의 급식 정보가 없어요.
                </div>
              )}
            </section>
          ))}
        </div>
      )}
    </DashboardCard>
  );
}
