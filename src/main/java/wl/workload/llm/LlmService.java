package wl.workload.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import wl.workload.analysis.Analyzer;

@Service
public class LlmService {

    // OpenAI API 호출용 인스턴스
    private final WebClient http;

    // JSON 파싱을 위한 Jackson ObjectMapper
    private final ObjectMapper om = new ObjectMapper();

    // 사용할 OpenAI 모델명 (환경변수 OPENAI_MODEL 없으면, gpt-4o-mini 기본값)
    private final String model;

    // 디버그 모드 여부 (환경변수 OPENAI_DEBUG=1이면, true)
    private final boolean DEBUG;

    // 생성자
    public LlmService() {

        // ① OpenAI API Key 설정
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY 환경변수를 설정하세요.");
        }

        // ② 모델 선택
        this.model = Optional.ofNullable(System.getenv("OPENAI_MODEL"))
                .orElse("gpt-4o-mini");

        // ③ 디버그 모드 여부
        this.DEBUG = "1".equals(System.getenv("OPENAI_DEBUG"));

        // ④ WebClient 빌드 (Authorization, Content-Type 등 헤더 미리 세팅)
        this.http = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

    }

    // LLM에 넘길 프롬프트 문자열 생성
    public String buildPrompt(Analyzer.Result r) {

        String taskName;

        try {
            // enum → 문자열
            taskName = String.valueOf(r.input().task());
        } catch (Exception e) {
            taskName = "UNKNOWN";
        }

        // 💡프롬프트 구성 명령어
        return String.join("\n",
                "지시사항:",
                "- 3~4줄의 조언을 한국어로 작성하세요.",
                "- 각 줄은 동사로 시작하세요.",
                "- 반드시 선택된 작업 유형(" + taskName + ")과 관련하여 맥락 있게 조언하세요.",
                "- Tone 단어('" + r.tone() + "')를 반영하되, 똑같은 어휘를 반복 사용하는 것은 지양하세요."
        );
    }


    // OpenAI Chat Completions API를 호출하여 advice 생성
    public String generateAdvice(Analyzer.Result r) {
        String prompt = buildPrompt(r);

        try {
            // API 요청 바디 구성
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are a concise assistant."),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.7, // 약간의 다양성 부여
                    "max_tokens", 300 // 답변 제한
            );

            // API 호출 (최대 60초 대기)
            String json = http.post()
                    .uri("/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(60));

            if (json == null || json.isBlank()) return "";
            if (DEBUG) System.out.println(">>> RAW JSON (chat): " + json);

            // JSON 파싱 (choices → message → content)
            JsonNode root = om.readTree(json);
            JsonNode choices = root.path("choices");

            if (choices.isArray() && choices.size() > 0) {
                JsonNode msg = choices.get(0).path("message").path("content");

                // content가 문자열일 경우
                if (msg.isTextual()) {
                    return msg.asText().trim();
                }
                // content가 배열(JSON array) 형태일 경우
                if (msg.isArray()) {
                    for (JsonNode part : msg) {
                        String t = part.asText("");
                        if (!t.isBlank()) return t.trim();
                    }
                }
            }
        } catch (Exception e) {
            // 에러 발생 시
            if (DEBUG) e.printStackTrace();
            return "[LLM Error] " + e.getClass().getSimpleName();
        }

        return "";
    }

}
