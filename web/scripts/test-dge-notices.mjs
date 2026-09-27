import assert from 'node:assert/strict';
import test from 'node:test';
import { detectNoticeProvider } from '../lib/notices/provider.ts';

test('Daegu has its own provider before generic NTT markup', () => {
  assert.equal(detectNoticeProvider('https://suseong.dge.ms.kr/suseongm/main.do', '<a href="/suseongm/na/ntt/selectNttList.do">가정통신문</a>'), 'dge-board');
  assert.notEqual(detectNoticeProvider('https://notdge.ms.kr/main.do', ''), 'dge-board');
});

import { readFileSync } from 'node:fs';
import { parseGoehsNoticeBoardUrl, parseGoehsNoticeList } from '../lib/notices/ntt-board.ts';
const boardUrl = 'https://suseong.dge.ms.kr/suseongm/na/ntt/selectNttList.do?mi=10080407&bbsId=10080407';
const fixture = readFileSync(new URL('./fixtures/dge-suseong-notices.html', import.meta.url), 'utf8');

test('finds family notices rather than announcements and decodes ampersands', () => {
  const html = '<a href="/suseongm/na/ntt/selectNttList.do?mi=1&bbsId=1">공지사항</a><a href="/suseongm/na/ntt/selectNttList.do?mi=10080407&amp;bbsId=10080407"><span>가정통신문</span></a>';
  assert.equal(parseGoehsNoticeBoardUrl('https://suseong.dge.ms.kr/suseongm/main.do', html), boardUrl);
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
