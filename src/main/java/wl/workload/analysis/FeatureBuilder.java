package wl.workload.analysis;

import wl.workload.model.TlxScores;

// TLX 점수에서 파생된 8차원 특징 벡터 생성
public final class FeatureBuilder {

    public static double[] build8D(TlxScores s) {

        // Performance는 역산해서 OutcomePressure로 사용
        final double outcomePressure = 100.0 - s.performance();

        // 6축 배열
        final double[] sixAxes = {
                s.mental(), s.physical(), s.temporal(),
                outcomePressure, s.effort(), s.frustration()
        };

        // TLX 평균
        final double tlxMean = (sixAxes[0] + sixAxes[1] + sixAxes[2] +
                                sixAxes[3] + sixAxes[4] + sixAxes[5]) / 6.0;

        // 축별 편차
        final double[] deltas = new double[6];

        for (int i = 0; i < 6; i++) deltas[i] = sixAxes[i] - tlxMean;

        // Stress 계산식 (가중치는 임의로 설정됨)
        final double stress = 0.5 * s.frustration()
                             + 0.3 * s.temporal()
                             + 0.2 * s.effort();

        // 8D 벡터 반환
        return new double[] {
                deltas[0], deltas[1], deltas[2], deltas[3],
                deltas[4], deltas[5], tlxMean, stress
        };
    }

}
