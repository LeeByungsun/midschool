import assert from 'node:assert/strict';
import test from 'node:test';

import { buildLegacyHomepageAliases, resolveRedirectTargetUrl } from '../lib/notices/url.ts';
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

test('jje schools use the jje provider for homepage and direct board urls', () => {
  const homepageProvider = detectNoticeProvider(
    'https://school.jje.go.kr/tamna/',
    '<a href="/tamna/na/ntt/selectNttList.do?mi=118220&bbsId=118403"><span>가정통신문</span></a>',
  );
  const boardProvider = detectNoticeProvider(
    'https://school.jje.go.kr/tamna/na/ntt/selectNttList.do?mi=118220&bbsId=118403',
    '<title>탐라중학교 > 알림마당 > 가정통신문</title>',
  );

  assert.equal(homepageProvider, 'jje-board');
  assert.equal(boardProvider, 'jje-board');
});

test('use schools use the use provider for homepage and direct board urls', () => {
  const homepageProvider = detectNoticeProvider(
    'https://school.use.go.kr/ulsanms-m/M01/',
    '<a href="/ulsanms-m/M01050302/list"><span>가정통신문</span></a>',
  );
  const boardProvider = detectNoticeProvider(
    'https://school.use.go.kr/ulsanms-m/M01050302/list',
    '<title>가정통신문<학부모마당<커뮤니티<울산중학교</title>',
  );

  assert.equal(homepageProvider, 'use-board');
  assert.equal(boardProvider, 'use-board');
});

test('cbe schools use the cbe provider for homepage and direct board urls', () => {
  const homepageProvider = detectNoticeProvider(
    'https://school.cbe.go.kr/cms-m',
    '<a href="/cms-m/M010302/"><span>가정통신문</span></a>',
  );
  const boardProvider = detectNoticeProvider(
    'https://school.cbe.go.kr/cms-m/M010302/list',
    '<title>가정통신문<학부모마당<커뮤니티<충북중학교</title>',
  );

  assert.equal(homepageProvider, 'cbe-board');
  assert.equal(boardProvider, 'cbe-board');
});

test('jbe schools use the jbe provider for homepage and direct board urls', () => {
  const homepageProvider = detectNoticeProvider(
    'https://school.jbedu.kr/muju/index.do',
    '<a href="/muju/MABAIAD/index.do"><span>가정통신문</span></a>',
  );
  const boardProvider = detectNoticeProvider(
    'https://school.jbedu.kr/muju/MABAIAD/index.do',
    '<title>가정통신문<학부모방<무주중학교</title>',
  );

  assert.equal(homepageProvider, 'jbe-board');
  assert.equal(boardProvider, 'jbe-board');
});

test('gen schools use the xhomenews provider for homepage and direct board urls', () => {
  const homepageProvider = detectNoticeProvider(
    'http://gshin.gen.ms.kr/main/main.php',
    '<a href="../xhomenews/board.php?tbnum=1"><span>가정통신문</span></a>',
  );
  const boardProvider = detectNoticeProvider(
    'http://gshin.gen.ms.kr/xhomenews/board.php?tbnum=1',
    '<title>학부모마당 &gt; 가정통신문</title>',
  );

  assert.equal(homepageProvider, 'gen-xhomenews');
  assert.equal(boardProvider, 'gen-xhomenews');
});

test('icems schools use the gwe-style board provider for homepage and direct board urls', () => {
  const homepageProvider = detectNoticeProvider(
    'https://incheonms.icems.kr/main.do',
    '<a href="/boardCnts/list.do?boardID=40507&m=0302&s=incheonms"><span>가정통신문</span></a>',
  );
  const boardProvider = detectNoticeProvider(
    'https://incheonms.icems.kr/boardCnts/list.do?boardID=40507&m=0302&s=incheonms',
    '<title>가정통신문 ＜ 알림마당</title>',
  );

  assert.equal(homepageProvider, 'gwe-board');
  assert.equal(boardProvider, 'gwe-board');
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

test('legacy jje homepage aliases map to school.jje.go.kr root and board paths', () => {
  assert.deepEqual(buildLegacyHomepageAliases('http://tamna.jje.ms.kr'), [
    'https://school.jje.go.kr/tamna/',
  ]);
  assert.deepEqual(
    buildLegacyHomepageAliases(
      'http://tamna.jje.ms.kr/na/ntt/selectNttList.do?mi=118220&bbsId=118403',
    ),
    [
      'https://school.jje.go.kr/tamna/na/ntt/selectNttList.do?mi=118220&bbsId=118403',
    ],
  );
});
