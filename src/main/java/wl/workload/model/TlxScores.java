package wl.workload.model;

/*
 * NASA TLX 6개 항목 점수 (0~100)
 */

public record TlxScores(
        double mental,
        double physical,
        double temporal,
        double performance,
        double effort,
        double frustration
) {}
