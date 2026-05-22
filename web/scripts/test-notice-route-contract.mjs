import assert from 'node:assert/strict';
import test from 'node:test';

import {
  createNoticeErrorResponse,
  createNoticeSuccessResponse,
} from '../lib/notices/response.ts';

test('notice success response marks non-empty items as success', () => {
  const response = createNoticeSuccessResponse([
    {
      id: '1',
      title: '가정통신문',
      date: '2026-05-22',
      author: '학교',
      url: 'https://example.com/notices/1',
      sourceUrl: 'https://example.com/notices',
    },
  ]);

  assert.deepEqual(response, {
    status: 'success',
    items: [
      {
        id: '1',
        title: '가정통신문',
        date: '2026-05-22',
        author: '학교',
        url: 'https://example.com/notices/1',
        sourceUrl: 'https://example.com/notices',
      },
    ],
  });
});

test('notice success response marks empty items as empty', () => {
  assert.deepEqual(createNoticeSuccessResponse([]), {
    status: 'empty',
    items: [],
  });
});

test('notice error response keeps machine-readable status and code while preserving items array', () => {
  const response = createNoticeErrorResponse({
    message: '대구교육청 학교 홈페이지는 가정통신문 조회를 아직 지원하지 않습니다.',
    status: 'unsupported',
    errorCode: 'UNSUPPORTED_PROVIDER',
  });

  assert.deepEqual(response, {
    status: 'unsupported',
    errorCode: 'UNSUPPORTED_PROVIDER',
    message: '대구교육청 학교 홈페이지는 가정통신문 조회를 아직 지원하지 않습니다.',
    items: [],
  });
});

test('notice error response defaults to error with empty items for transport-safe failures', () => {
  const response = createNoticeErrorResponse({
    message: '가정통신문 목록을 불러오지 못했어요.',
    errorCode: 'INTERNAL_ERROR',
  });

  assert.deepEqual(response, {
    status: 'error',
    errorCode: 'INTERNAL_ERROR',
    message: '가정통신문 목록을 불러오지 못했어요.',
    items: [],
  });
});
