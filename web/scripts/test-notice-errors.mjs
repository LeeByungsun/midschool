import assert from 'node:assert/strict';
import test from 'node:test';

import { isRecoverableNoticeError } from '../lib/notices/errors.ts';

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
