// /js/app.js
import { fetchTasks, analyze, coach } from './api.js';
import {
    state,
    renderTasks,
    renderSliders,
    buildRequestBody,
    paintAnalysis,
    paintText,
    paintQuadrant,          // ✅ 사분면 플롯
} from './ui.js';

console.log('[boot] app.js loaded');

// 버튼 연타 방지 유틸
async function withLock(button, fn) {
    if (!button) return;
    if (button.disabled) return;
    button.disabled = true;
    const oldText = button.textContent;
    try {
        button.textContent = '처리 중...';
        await fn();
    } catch (e) {
        console.error('[ui] error:', e);
        alert(e?.message || e || '오류가 발생했어요');
    } finally {
        button.textContent = oldText;
        button.disabled = false;
    }
}

async function init() {
    console.log('[init] start');

    // 초기 로드: 작업 카드 + 슬라이더
    try {
        const tasks = await fetchTasks();
        console.log('[init] tasks=', tasks);
        renderTasks(document.getElementById('taskCards'), tasks, (t) => {
            paintText('taskLabel', t.label);
        });
    } catch (e) {
        console.error('[init] fetchTasks error:', e);
        alert('작업 목록을 불러오지 못했습니다: ' + (e?.message || e));
    }

    renderSliders(document.getElementById('sliders'));

    // 버튼 핸들러
    const btnAnalyze = document.getElementById('btnAnalyze');
    const btnCoach   = document.getElementById('btnCoach');

    // 결과 섹션 스크롤 유틸(존재하면만)
    const scrollToResult = () => {
        const sec = document.querySelector('[aria-label="분석 결과"]');
        if (sec) sec.scrollIntoView({ behavior: 'smooth', block: 'start' });
    };

    btnAnalyze.onclick = () =>
        withLock(btnAnalyze, async () => {
            console.log('[click] analyze');
            const body = buildRequestBody();
            console.log('[click] analyze body=', body);
            const r = await analyze(body);
            console.log('[analyze] result=', r);

            paintAnalysis(r);          // 요약 텍스트
            paintQuadrant(r);          // 사분면 플롯
            paintText('advice', '');   // 분석 모드에선 조언 비움

            scrollToResult();
        });

    btnCoach.onclick = () =>
        withLock(btnCoach, async () => {
            console.log('[click] coach');
            const body = buildRequestBody();
            console.log('[click] coach body=', body);
            const { analysis, advice } = await coach(body);
            console.log('[coach] analysis=', analysis);
            console.log('[coach] advice=', advice);

            paintAnalysis(analysis);        // 요약 텍스트
            paintQuadrant(analysis);        // 사분면 플롯
            paintText('advice', advice || '');

            scrollToResult();
        });
}

init();