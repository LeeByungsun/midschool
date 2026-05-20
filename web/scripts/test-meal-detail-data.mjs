import assert from "node:assert/strict";
import test from "node:test";

import { resolveMealDetailState } from "../lib/meal-detail.ts";

const mondayMeal = {
  date: "20260518",
  mealType: "중식",
  menu: "비빔밥",
  calorieInfo: "500kcal",
  nutritionInfo: "",
  originInfo: "",
};

const tuesdayMeal = {
  date: "20260519",
  mealType: "중식",
  menu: "돈까스",
  calorieInfo: "650kcal",
  nutritionInfo: "",
  originInfo: "",
};

const wednesdayMeal = {
  date: "20260520",
  mealType: "중식",
  menu: "카레라이스",
  calorieInfo: "620kcal",
  nutritionInfo: "",
  originInfo: "",
};

test("weekly meal detail keeps every returned day when the requested date exists", () => {
  const result = resolveMealDetailState(
    [wednesdayMeal, mondayMeal, tuesdayMeal],
    "20260519",
  );

  assert.equal(result.hasRequestedDate, true);
  assert.deepEqual(
    result.visibleMeals.map((meal) => meal.date),
    ["20260518", "20260519", "20260520"],
  );
});

test("weekly meal detail still exposes the returned week when the requested date is missing", () => {
  const result = resolveMealDetailState(
    [wednesdayMeal, mondayMeal],
    "20260519",
  );

  assert.equal(result.hasRequestedDate, false);
  assert.deepEqual(
    result.visibleMeals.map((meal) => meal.date),
    ["20260518", "20260520"],
  );
});
