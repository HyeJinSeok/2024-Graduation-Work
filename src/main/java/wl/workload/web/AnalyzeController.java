package wl.workload.web;

import org.springframework.web.bind.annotation.*;
import wl.workload.analysis.*;
import wl.workload.model.*;

import java.util.List;

/*
 * REST API 컨트롤러
 *
 *  - /api/tasks : 작업유형 목록 조회
 *  - /api/analyze : TLX 입력 받아 PCA 분석결과 반환
 */

@RestController
@RequestMapping("/api")
public class AnalyzeController {

    private final Analyzer analyzer = new Analyzer(
            PCAModel.loadFromResource("/pca_model.json")
    );

    // 작업유형 5개 반환 (UI 카드용)
    @GetMapping("/tasks")
    public List<String> tasks() {

        return List.of(
                TaskType.MEETING.label(),
                TaskType.STUDY.label(),
                TaskType.CREATIVE.label(),
                TaskType.OPS.label(),
                TaskType.ADMIN.label()
        );
    }

    // 분석 실행
    @PostMapping("/analyze")
    public Analyzer.Result analyze(@RequestBody AnalyzeRequest req) {

        var task = TaskType.valueOf(req.task());

        var scores = new TlxScores(
                req.mental(), req.physical(), req.temporal(),
                req.performance(), req.effort(), req.frustration()
        );

        var sub = new Submission(task, scores);

        return analyzer.analyze(sub);
    }

    // 요청 DTO
    public record AnalyzeRequest(

            String task,
            double mental,
            double physical,
            double temporal,
            double performance,
            double effort,
            double frustration
    ) {}

}