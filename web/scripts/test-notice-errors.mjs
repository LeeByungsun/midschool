/** 가정통신문 오류 처리 규칙을 검증하는 경량 스크립트 테스트입니다. */

import assert from 'node:assert/strict';
import test from 'node:test';

import {
  canRetryNoticeRequest,
  createNoticeRequestError,
  isDgeSchoolHomepage,
  isRecoverableNoticeError,
  NoticeRequestError,
} from '../lib/notices/errors.ts';

test('notice parser failures are treated as recoverable', () => {
  assert.equal(
    isRecoverableNoticeError(new Error('가정통신문 게시판 링크를 찾지 못했어요.')),
    true,
  );
  assert.equal(
    isRecoverableNoticeError(new Error('학교 홈페이지에서 가정통신문 구조를 찾지 못했어요.')),
    true,
  );
  assert.equal(
    isRecoverableNoticeError(
      new Error('학교 홈페이지 응답 대기 시간이 초과되었어요: https://example.com'),
    ),
    true,
  );
});

test('unexpected failures are still treated as errors', () => {
  assert.equal(isRecoverableNoticeError(new Error('boom')), false);
  assert.equal(isRecoverableNoticeError('boom'), false);
});

test('unsupported notice providers stay non-retryable independently of display copy', () => {
  const error = new NoticeRequestError(
    '표시 문구는 정책과 독립적으로 바뀔 수 있습니다.',
    'UNSUPPORTED_PROVIDER',
  );

  assert.equal(canRetryNoticeRequest(error), false);
  assert.equal(
    canRetryNoticeRequest(
      new NoticeRequestError('일시적인 수집 실패', 'NOTICE_SOURCE_UNAVAILABLE'),
    ),
    true,
  );
  assert.equal(canRetryNoticeRequest(new Error('알 수 없는 실패')), true);
});

test('notice response errors preserve their machine-readable code', () => {
  const error = createNoticeRequestError({
    message: '새로운 사용자 안내 문구',
    errorCode: 'UNSUPPORTED_PROVIDER',
  });

  assert.equal(error instanceof NoticeRequestError, true);
  assert.equal(error.message, '새로운 사용자 안내 문구');
  assert.equal(error.errorCode, 'UNSUPPORTED_PROVIDER');
});

test('DGE homepage detection respects hostname label boundaries', () => {
  assert.equal(isDgeSchoolHomepage('https://school.dge.ms.kr/main'), true);
  assert.equal(isDgeSchoolHomepage('https://dge.ms.kr'), true);
  assert.equal(isDgeSchoolHomepage('https://notdge.ms.kr'), false);
  assert.equal(isDgeSchoolHomepage('not a url'), false);
});
