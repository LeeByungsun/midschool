import assert from 'node:assert/strict';
import test from 'node:test';

import {
  createTimerSnapshot,
  formatTimerClock,
  pauseTimer,
  resumeTimer,
  startTimer,
  syncTimerSnapshot,
} from '../lib/timer.ts';

test('running timer keeps target time and remaining time in sync', () => {
  const initial = createTimerSnapshot('집중', 25 * 60 * 1000, 1_000);
  const running = startTimer(initial, 1_000);
  const midway = syncTimerSnapshot(running, 61_000);

  assert.equal(running.status, 'running');
  assert.equal(running.targetTime, 1_501_000);
  assert.equal(midway.remainingMs, 1_440_000);
});

test('paused timer resumes from the preserved remaining time', () => {
  const initial = createTimerSnapshot('휴식', 5 * 60 * 1000, 0);
  const running = startTimer(initial, 10_000);
  const paused = pauseTimer(running, 40_000);
  const resumed = resumeTimer(paused, 50_000);

  assert.equal(paused.status, 'paused');
  assert.equal(paused.remainingMs, 270_000);
  assert.equal(resumed.status, 'running');
  assert.equal(resumed.targetTime, 320_000);
});

test('expired timer becomes completed on restore', () => {
  const initial = createTimerSnapshot('딥포커스', 50 * 60 * 1000, 5_000);
  const running = startTimer(initial, 5_000);
  const completed = syncTimerSnapshot(running, 3_100_000);

  assert.equal(completed.status, 'completed');
  assert.equal(completed.remainingMs, 0);
  assert.ok(completed.completedAt);
});

test('clock formatting rounds up remaining seconds for display', () => {
  assert.equal(formatTimerClock(0), '00:00');
  assert.equal(formatTimerClock(1_000), '00:01');
  assert.equal(formatTimerClock(61_000), '01:01');
});
