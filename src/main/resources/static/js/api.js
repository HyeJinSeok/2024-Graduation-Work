// /js/api.js
const JSON_HEADERS = { 'Content-Type': 'application/json' };

// 공통: 응답 로깅 & JSON 파싱 (디버깅 편의)
async function parseJsonWithLog(res, tag) {
    const raw = await res.text();
    console.log(`[${tag}] status=`, res.status, 'raw=', raw);
    if (!res.ok) throw new Error(raw || `${tag} failed (${res.status})`);
    try {
        return raw ? JSON.parse(raw) : null;
    } catch {
        throw new Error(`${tag}: invalid JSON response`);
    }
}

export async function fetchTasks() {
    console.log('[api] GET /api/tasks');
    const res = await fetch('/api/tasks');
    const data = await parseJsonWithLog(res, 'tasks');
    // AnalyzeController가 문자열 배열을 반환하는 경우도 고려
    if (Array.isArray(data) && (data.length === 0 || typeof data[0] === 'string')) {
        const enumNames = ['MEETING', 'STUDY', 'CREATIVE', 'OPS', 'ADMIN'];
        return (data || []).map((label, i) => ({ name: enumNames[i] || String(label).toUpperCase(), label }));
    }
    return data; // [{name, label}]
}

export async function analyze(body) {
    console.log('[api] POST /api/analyze body=', body);
    const res = await fetch('/api/analyze', {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify(body),
    });
    return parseJsonWithLog(res, 'analyze'); // Analyzer.Result
}

export async function coach(body) {
    console.log('[api] POST /api/coach body=', body);
    const res = await fetch('/api/coach', {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify(body),
    });
    return parseJsonWithLog(res, 'coach'); // { analysis, advice }
}