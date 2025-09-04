package wl.workload.model;

/*
 * 사용자 입력: 작업 유형 + TLX 점수 세트
 */
public record Submission(
        TaskType task,
        TlxScores tlx
) {}