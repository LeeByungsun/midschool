/** 가정통신문 카드 상태 분기 규칙을 검증하는 경량 스크립트 테스트입니다. */

import assert from 'node:assert/strict';
import test from 'node:test';

import { resolveNoticeCardState } from '../lib/notices/view-state.ts';

const studentInfo = {
  schoolName: '테스트중학교',
  officeCode: 'B10',
  schoolCode: '7010536',
  homepage: 'https://example.school',
  grade: '2',
  classroom: '3',
};

test('returns not-configured when preferences are missing after hydration', () => {
  const state = resolveNoticeCardState({
    hydrated: true,
    studentInfo: null,
    loadState: { status: 'idle' },
  });

  assert.equal(state.status, 'not-configured');
});

test('returns loading before hydration and during active fetches', () => {
  assert.equal(
    resolveNoticeCardState({
      hydrated: false,
      studentInfo: null,
      loadState: { status: 'idle' },
    }).status,
    'loading',
  );

  assert.equal(
    resolveNoticeCardState({
      hydrated: true,
      studentInfo,
      loadState: { status: 'loading' },
    }).status,
    'loading',
  );
});

test('returns empty when a successful fetch has no notice items', () => {
  const state = resolveNoticeCardState({
    hydrated: true,
    studentInfo,
    loadState: { status: 'success', items: [] },
  });

  assert.equal(state.status, 'empty');
});

test('returns error with retry flag when fetch fails', () => {
  const state = resolveNoticeCardState({
    hydrated: true,
    studentInfo,
    loadState: {
      status: 'error',
      message: '대구교육청 학교 홈페이지는 가정통신문 조회를 아직 지원하지 않습니다.',
      canRetry: false,
    },
  });

  assert.deepEqual(state, {
    status: 'error',
    message: '대구교육청 학교 홈페이지는 가정통신문 조회를 아직 지원하지 않습니다.',
    canRetry: false,
  });
});

test('returns success when notice items exist', () => {
  const state = resolveNoticeCardState({
    hydrated: true,
    studentInfo,
    loadState: {
      status: 'success',
      items: [
        {
          id: '1',
          title: '가정통신문 제목',
          url: 'https://example.school/notice/1',
          date: '2026-05-22',
          author: '행정실',
        },
      ],
    },
  });

  assert.equal(state.status, 'success');
  assert.equal(state.items.length, 1);
});
