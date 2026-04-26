import assert from 'node:assert/strict';
import test from 'node:test';

import { resolveRedirectTargetUrl } from '../lib/notices/url.ts';
import { detectNoticeProvider } from '../lib/notices/provider.ts';
import { buildNoticeHomepageCandidates } from '../lib/notices/candidates.ts';

test('client redirect keeps the redirected school path as the relative base', () => {
  const nextUrl = resolveRedirectTargetUrl({
    requestedUrl: 'https://school.busanedu.net/busan-m',
    responseUrl: 'https://school.busanedu.net/busan-m/',
    redirectPath: 'intro.do',
  });

  assert.equal(nextUrl, 'https://school.busanedu.net/busan-m/intro.do');
});

test('client redirect still resolves absolute paths from the final response url', () => {
  const nextUrl = resolveRedirectTargetUrl({
    requestedUrl: 'https://example.edu/school',
    responseUrl: 'https://example.edu/school/',
    redirectPath: '/board/list.do',
  });

  assert.equal(nextUrl, 'https://example.edu/board/list.do');
});

test('goehs schools keep the goehs provider even when the page contains common selectNttList markup', () => {
  const provider = detectNoticeProvider(
    'https://bansong-m.goehs.kr/bansong-m/main.do',
    '<a href="/na/ntt/selectNttList.do?mi=12000&bbsId=5000">가정통신문</a>',
  );

  assert.equal(provider, 'goehs-board');
});

test('busan schools still use the busan provider', () => {
  const provider = detectNoticeProvider(
    'https://school.busanedu.net/busan-m/main.do',
    '<a href="/na/ntt/selectNttList.do?mi=12000&bbsId=5000">가정통신문</a>',
  );

  assert.equal(provider, 'busan-school');
});

test('gne schools use the gne provider for homepage and direct board urls', () => {
  const homepageProvider = detectNoticeProvider(
    'https://bsg-m.gne.go.kr/',
    '<a href="/bsg-m/na/ntt/selectNttList.do?mi=88638&bbsId=76109"><span>가정통신문</span></a>',
  );
  const boardProvider = detectNoticeProvider(
    'https://bsg-m.gne.go.kr/bsg-m/na/ntt/selectNttList.do?mi=88638&bbsId=76109',
    '<title>가정통신문 | 반송여자중학교</title>',
  );

  assert.equal(homepageProvider, 'gne-board');
  assert.equal(boardProvider, 'gne-board');
});

test('gyo6 schools use the gyo6 provider for homepage and direct board urls', () => {
  const homepageProvider = detectNoticeProvider(
    'https://school.gyo6.net/booksamms/',
    '<a href="/booksamms/na/ntt/selectNttList.do?mi=106667&bbsId=31684"><span>가정통신문</span></a>',
  );
  const boardProvider = detectNoticeProvider(
    'https://school.gyo6.net/booksamms/na/ntt/selectNttList.do?mi=106667&bbsId=31684',
    '<title>북삼중학교 > 학교생활 > 가정통신문목록</title>',
  );

  assert.equal(homepageProvider, 'gyo6-board');
  assert.equal(boardProvider, 'gyo6-board');
});

test('notice homepage candidates keep a stale saved homepage first and then fall back to the resolved school homepage', () => {
  const candidates = buildNoticeHomepageCandidates({
    requestedHomepage: 'https://bansong-m.goehs.kr/',
    resolvedHomepage: 'https://ban-song-m.goehs.kr/',
  });

  assert.deepEqual(candidates, [
    'https://bansong-m.goehs.kr/',
    'https://ban-song-m.goehs.kr/',
  ]);
});

test('notice homepage candidates dedupe identical homepages', () => {
  const candidates = buildNoticeHomepageCandidates({
    requestedHomepage: 'https://ban-song-m.goehs.kr/',
    resolvedHomepage: 'https://ban-song-m.goehs.kr/',
  });

  assert.deepEqual(candidates, ['https://ban-song-m.goehs.kr/']);
});
