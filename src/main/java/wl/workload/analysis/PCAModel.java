package wl.workload.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;

/*
 * PCA 모델을 불러와 8차원 입력 벡터(TLX → FeatureBuilder 결과)를
 * 2차원 좌표 (u, v) 로 투영(projection)하는 역할을 담당하는 클래스
 *
 * - PCA 학습 결과는 JSON 파일(mu, sigma, W 행렬)로 저장되어 있음
 * - JSON을 읽어 들여 모델 파라미터를 초기화
 * - 입력 벡터를 표준화(z-score) 후, 투영 행렬(W)을 곱해 2D 좌표 계산
 */
public final class PCAModel {

    public final double[] mu;    // 평균 (길이 8)
    public final double[] sigma; // 표준편차 (길이 8)
    public final double[][] W;   // 투영 행렬 (8x2)

    public PCAModel(double[] mu, double[] sigma, double[][] W) {
        this.mu = mu; this.sigma = sigma; this.W = W;
    }

    // PCA 모델 JSON 파일을 읽어 PCAModel 객체로 변환
    public static PCAModel loadFromResource(String resourcePath) {

        try (InputStream is = PCAModel.class.getResourceAsStream(resourcePath)) {

            var om = new ObjectMapper();
            var dto = om.readValue(is, Spec.class);
            return new PCAModel(dto.mu, dto.sigma, dto.W);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load PCA model: " + resourcePath, e);
        }

    }

    // 8차원 특징 벡터(x8)를 받아 2차원 (u, v) 좌표로 투영
    public double[] project(double[] x8) {

        double[] z = new double[8];

        // 1. 표준화 (각 차원별 평균/표준편차 적용)
        for (int i = 0; i < 8; i++) {
            double s = (sigma[i] == 0 ? 1 : sigma[i]); // 분모=0 방지
            z[i] = (x8[i] - mu[i]) / s;
        }

        double u = 0, v = 0;

        // 2. 선형 결합 → (u, v) 계산
        for (int i = 0; i < 8; i++) {
            u += z[i] * W[i][0];
            v += z[i] * W[i][1];
        }

        return new double[]{u, v};
    }

    // JSON 매핑용 DTO
    public static final class Spec {
        public double[] mu;
        public double[] sigma;
        public double[][] W;
    }

}