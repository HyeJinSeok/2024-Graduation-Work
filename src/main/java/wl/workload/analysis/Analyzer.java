package wl.workload.analysis;

import wl.workload.model.Submission;

// 입력 → 8D 특징 → PCA 투영 → 사분면/톤 → 요약값
public final class Analyzer {

    private final PCAModel lens;

    public Analyzer(PCAModel lens) { this.lens = lens; }

    // 결과 컨테이너
    public static record Result(

            Submission input,
            double tlxMean,
            double stress,
            double u,
            double v,
            Quadrant quadrant,
            String tone
    ) {}

    // 사용자가 제출한 Submission 통해서 Result 반납하는 analyze 메서드
    public Result analyze(Submission sub) {

        final double[] vec8 = FeatureBuilder.build8D(sub.tlx());
        final double tlx = vec8[6], stress = vec8[7];
        final double[] uv = lens.project(vec8);

        final Quadrant q = Quadrant.classify(uv[0], uv[1]);
        final String tone = q.tone();

        return new Result(sub, tlx, stress, uv[0], uv[1], q, tone);
    }

}