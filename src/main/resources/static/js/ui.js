// /js/ui.js
// ─────────────────────────────────────────────────────────────────────────────
// 전역 상태: 선택된 작업(task)과 6개 슬라이더 값
// ─────────────────────────────────────────────────────────────────────────────
export const state = {
    task: null, // { name, label }
    // [라벨, 초기값]
    sliders: [
        ['Mental', 50], ['Physical', 50], ['Temporal', 50],
        ['Performance', 50], ['Effort', 50], ['Frustration', 50],
    ],
};

// ─────────────────────────────────────────────────────────────────────────────
// 작업 카드 렌더링
// - container: #taskCards
// - tasks: [{name,label}, ...]
// - onSelect: 선택 시 콜백
// ─────────────────────────────────────────────────────────────────────────────
export function renderTasks(container, tasks, onSelect) {
    container.innerHTML = '';
    (tasks || []).forEach((t) => {
        const div = document.createElement('button'); // 접근성↑: button 요소 사용
        div.type = 'button';
        div.className = 'card';
        div.textContent = t.label;
        div.setAttribute('aria-pressed', 'false');

        div.onclick = () => {
            state.task = t;

            // 모든 카드 비활성화 후 현재 카드 활성화
            [...container.children].forEach((c) => {
                c.classList.remove('active');
                c.setAttribute('aria-pressed', 'false');
            });
            div.classList.add('active');
            div.setAttribute('aria-pressed', 'true');

            onSelect?.(t);
        };

        container.appendChild(div);
    });
}

// ─────────────────────────────────────────────────────────────────────────────
/* 슬라이더 렌더링
   - container: #sliders
   - 각 줄은 라벨 / range input / 현재값으로 구성
*/
// ─────────────────────────────────────────────────────────────────────────────
export function renderSliders(container) {
    container.innerHTML = '';
    state.sliders.forEach(([name, val], idx) => {
        const row = document.createElement('div');
        row.className = 'row';

        const lab = document.createElement('label');
        lab.textContent = name;

        const sld = document.createElement('input');
        sld.type = 'range';
        sld.min = 0;
        sld.max = 100;
        sld.value = val;
        sld.setAttribute('aria-label', name);

        const out = document.createElement('div');
        out.className = 'out';
        out.textContent = val;

        sld.oninput = (e) => {
            const v = Number(e.target.value);
            out.textContent = v;
            state.sliders[idx][1] = v;
        };

        row.append(lab, sld, out);
        container.appendChild(row);
    });
}

// ─────────────────────────────────────────────────────────────────────────────
// 서버로 보낼 요청 바디 생성
// ─────────────────────────────────────────────────────────────────────────────
export function buildRequestBody() {
    if (!state.task) throw new Error('작업을 선택하세요');
    const vals = state.sliders.map((x) => x[1]);
    return {
        task: state.task.name,
        mental: vals[0], physical: vals[1], temporal: vals[2],
        performance: vals[3], effort: vals[4], frustration: vals[5],
    };
}

// ─────────────────────────────────────────────────────────────────────────────
// 분석 결과를 패널에 칠하기 (요약 텍스트만; (u,v)/사분면은 플롯이 담당)
// ─────────────────────────────────────────────────────────────────────────────
export function paintAnalysis(a) {
    setText('taskLabel', state.task?.label ?? '-');
    setText('tlx',    safeNum(a?.tlxMean, 1));
    setText('stress', safeNum(a?.stress, 1));
    setText('tone',   a?.tone ?? '-'); // 새 레이아웃에도 존재
    // (u,v)와 quadrant 텍스트는 제거 (시각화로 대체)
}

// ─────────────────────────────────────────────────────────────────────────────
// 사분면 플롯 그리기 (SVG)
// container: #quadPlot, 입력: Analyzer.Result(a.u, a.v, a.quadrant, a.tone)
// 컨테이너가 없으면 조용히 무시
// ─────────────────────────────────────────────────────────────────────────────
export function paintQuadrant(a, opts = {}) {
    const el = document.getElementById('quadPlot');
    if (!el) return;

    const u = Number(a?.u), v = Number(a?.v);
    const tone = a?.tone ?? '-';

    setText('uvU', isFinite(u) ? u.toFixed(3) : '-');
    setText('uvV', isFinite(v) ? v.toFixed(3) : '-');
    setText('tone', tone);

    // 컨테이너 실제 크기 기준으로 정사각 사이즈 결정
    const box = el.getBoundingClientRect();
    const size = Math.max(300, Math.floor(Math.min(box.width, box.height || box.width))); // 최소 300 보장
    const range = opts.range ?? 3;
    const pad = 10;

    const clamp = (x, lo, hi) => Math.max(lo, Math.min(hi, x));
    const mapX = x => ((clamp(x, -range, range) + range) / (2 * range)) * (size - pad*2) + pad;
    const mapY = y => ((range - clamp(y, -range, range)) / (2 * range)) * (size - pad*2) + pad;

    const cx = isFinite(u) ? mapX(u) : size/2;
    const cy = isFinite(v) ? mapY(v) : size/2;

    const qTitles = {
        q1: "Q1: 속도/압박↑·실행/추진",
        q2: "Q2: 목표·범위·품질 정교화",
        q3: "Q3: 부담↓·루틴/정비",
        q4: "Q4: 효율 향상·절차 개선"
    };

    const svg = `
    <svg width="100%" height="100%" viewBox="0 0 ${size} ${size}" role="img" aria-label="PCA Quadrant Plot">
      <defs>
        <linearGradient id="g" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stop-color="#ffffff"/>
          <stop offset="1" stop-color="#f8fafc"/>
        </linearGradient>
      </defs>

      <rect x="0" y="0" width="${size}" height="${size}" rx="14" fill="url(#g)" stroke="${getCssVar('--line')}" />
      <line x1="${size/2}" y1="${pad}" x2="${size/2}" y2="${size-pad}" stroke="#cbd5e1" />
      <line x1="${pad}" y1="${size/2}" x2="${size-pad}" y2="${size/2}" stroke="#cbd5e1" />

      <text x="${size - pad}" y="${pad + 14}" text-anchor="end" font-size="12" fill="#475569">${qTitles.q1}</text>
      <text x="${pad}"          y="${pad + 14}" text-anchor="start" font-size="12" fill="#475569">${qTitles.q2}</text>
      <text x="${pad}"          y="${size - pad - 4}" text-anchor="start" font-size="12" fill="#475569">${qTitles.q3}</text>
      <text x="${size - pad}"   y="${size - pad - 4}" text-anchor="end"  font-size="12" fill="#475569">${qTitles.q4}</text>

      <circle cx="${cx}" cy="${cy}" r="6" fill="${getCssVar('--pri')}" />
      <circle cx="${cx}" cy="${cy}" r="8" fill="none" stroke="#93c5fd" />

      ${[-range, 0, range].map(x => `
        <text x="${mapX(x)}" y="${size/2 + 16}" font-size="10" text-anchor="middle" fill="#64748b">${x}</text>
      `).join('')}
      ${[-range, 0, range].map(y => `
        <text x="${size/2 - 10}" y="${mapY(y) + 4}" font-size="10" text-anchor="end" fill="#64748b">${y}</text>
      `).join('')}
    </svg>
  `;

    el.innerHTML = svg;
}

// ─────────────────────────────────────────────────────────────────────────────
// 텍스트를 특정 요소에 그리기 (존재하면만 업데이트 → 안전)
// ─────────────────────────────────────────────────────────────────────────────
export function paintText(id, text) {
    setText(id, text || '');
}

// ─────────────────────────────────────────────────────────────────────────────
// LLM 프롬프트 미리보기(README용 캡처 등에서 유용)
// ─────────────────────────────────────────────────────────────────────────────
export function buildPrompt(a) {
    return [
        `# Context`,
        `Task: ${state.task?.label} (${state.task?.name})`,
        `TLX_mean: ${safeNum(a?.tlxMean, 1)}`,
        `Stress: ${safeNum(a?.stress, 1)}`,
        `PCA: u=${safeNum(a?.u, 3)}, v=${safeNum(a?.v, 3)}, quadrant=${a?.quadrant ?? '-'}`,
        ``,
        `# Instruction`,
        `- 톤: ${a?.tone ?? '-'}`,
        `- 동사로 시작하는 3~5줄 체크리스트.`,
        `- TLX/Stress를 강도·맥락에 반영.`,
    ].join('\n');
}

// ─────────────────────────────────────────────────────────────────────────────
// 내부 유틸
// ─────────────────────────────────────────────────────────────────────────────
function safeNum(v, digits = 1) {
    const n = Number(v);
    return Number.isFinite(n) ? n.toFixed(digits) : '-';
}

function setText(id, val){
    const el = document.getElementById(id);
    if (el) el.textContent = val;
}

function getCssVar(name){
    try {
        return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || '#2563eb';
    } catch {
        return '#2563eb';
    }
}