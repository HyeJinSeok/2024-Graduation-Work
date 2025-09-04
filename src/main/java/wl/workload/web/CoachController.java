package wl.workload.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import wl.workload.analysis.*;
import wl.workload.llm.LlmService;
import wl.workload.model.*;

import java.util.Arrays;
import java.util.Objects;

///api/coach REST API 엔드포인트 제공

@RestController
@RequestMapping("/api")
public class CoachController {

    private final Analyzer analyzer;
    private final LlmService llm;

    // 생성자
    public CoachController(LlmService llm) {

        // PCA 모델을 리소스에서 로드
        var model = PCAModel.loadFromResource("/pca_model.json");

        if (model == null) {
            throw new IllegalStateException("pca_model.json not found on classpath (expected at src/main/resources)");
        }

        this.analyzer = new Analyzer(model);
        this.llm = Objects.requireNonNull(llm, "LlmService must not be null");
    }


    @PostMapping("/coach")
    public CoachResponse coach(@RequestBody AnalyzeRequest req) {

        // ① TaskType 유효성 검사
        final TaskType task;

        try {
            task = TaskType.valueOf(req.task());

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unknown task: " + req.task() + " (expected one of " + Arrays.toString(TaskType.values()) + ")"
            );
        }

        // ② TLX 점수를 객체로 포장
        var scores = new TlxScores(
                req.mental(), req.physical(), req.temporal(),
                req.performance(), req.effort(), req.frustration()
        );

        // ③ 하나의 Submission 단위로 묶음 (task + 점수)
        var sub = new Submission(task, scores);

        // ④ PCA 기반 분석 (항상 수행)
        Analyzer.Result analysis = analyzer.analyze(sub);

        // ⑤ LLM 조언 생성
        String advice;

        try {
            advice = llm.generateAdvice(analysis);  // 내부적으로 OpenAI 호출

        } catch (Exception ex) {
            // 오류는 서버 로그에만 남기고, 클라이언트엔 빈 문자열 반환
            System.err.println("[LLM ERROR] " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            advice = "";
        }

        // ⑥ 분석 결과 + 조언 함께 반환
        return new CoachResponse(analysis, advice);
    }

    // 요청 DTO
    public record AnalyzeRequest(
            String task,
            double mental, double physical, double temporal,
            double performance, double effort, double frustration
    ) {}

    // 응답 DTO
    public record CoachResponse(
            Analyzer.Result analysis,
            String advice
    ) {}

}