import assert from 'node:assert/strict';
import test from 'node:test';
import { detectNoticeProvider } from '../lib/notices/provider.ts';

test('Daegu has its own provider before generic NTT markup', () => {
  assert.equal(detectNoticeProvider('https://suseong.dge.ms.kr/suseongm/main.do', '<a href="/suseongm/na/ntt/selectNttList.do">가정통신문</a>'), 'dge-board');
  assert.equal(detectNoticeProvider('https://ndg.dge.es.kr/ndge/main.do', ''), 'dge-board');
  assert.equal(detectNoticeProvider('https://dgjeil.dge.hs.kr/dgjeilh/main.do', ''), 'dge-board');
  assert.notEqual(detectNoticeProvider('https://notdge.ms.kr/main.do', ''), 'dge-board');
});

test('Gyeonggi goesw school homepages use the shared NTT provider', () => {
  assert.equal(
    detectNoticeProvider('https://susung-h.goesw.kr/susung-h/main.do', ''),
    'goehs-board',
  );
});

import { readFileSync } from 'node:fs';
import { parseGoehsNoticeBoardUrl, parseGoehsNoticeList } from '../lib/notices/ntt-board.ts';
const boardUrl = 'https://suseong.dge.ms.kr/suseongm/na/ntt/selectNttList.do?mi=10080407&bbsId=10080407';
const fixture = readFileSync(new URL('./fixtures/dge-suseong-notices.html', import.meta.url), 'utf8');

test('finds family notices rather than announcements and decodes ampersands', () => {
  const html = '<a href="/suseongm/na/ntt/selectNttList.do?mi=1&bbsId=1">공지사항</a><a href="/suseongm/na/ntt/selectNttList.do?mi=10080407&amp;bbsId=10080407"><span>가정통신문</span></a>';
  assert.equal(parseGoehsNoticeBoardUrl('https://suseong.dge.ms.kr/suseongm/main.do', html), boardUrl);
});

test('finds school-branded education notices used instead of the family-notice label', () => {
  const html = '<a href="/daeseoe/na/ntt/selectNttList.do?mi=10026513&amp;bbsId=10026513">공지사항</a><a href="/daeseoe/na/ntt/selectNttList.do?mi=10026514&amp;bbsId=10026514"><span>대서교육통신</span></a>';

  assert.equal(
    parseGoehsNoticeBoardUrl('https://daeseo.dge.es.kr/daeseoe/main.do', html),
    'https://daeseo.dge.es.kr/daeseoe/na/ntt/selectNttList.do?mi=10026514&bbsId=10026514',
  );
});

test('parses actual public Suseong board rows, dates, authors and detail URLs', () => {
  const items = parseGoehsNoticeList(boardUrl, fixture, 2);
  assert.equal(items.length, 2);
  assert.ok(items.every(item => item.id && item.title && /^2026\.\d{2}\.\d{2}$/.test(item.date)));
  assert.equal(items[0].sourceUrl, boardUrl);
  assert.equal(new URL(items[0].url).searchParams.get('nttSn'), items[0].id);
  assert.ok(new URL(items[0].url).pathname.endsWith('/selectNttInfo.do'));
  assert.ok(items[0].author);
});

test('empty board returns empty data and unrelated rows are ignored', () => {
  assert.deepEqual(parseGoehsNoticeList(boardUrl, '<tr><td>등록된 게시물이 없습니다.</td></tr>', 3), []);
});
