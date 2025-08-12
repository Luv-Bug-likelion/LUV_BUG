package likelion.traditional_market.CreateMission.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import likelion.traditional_market.CreateMission.Dto.MissionDetailDto;
import likelion.traditional_market.CreateMission.Dto.MissionStatusResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class ChatGptService {

    @Value("${chatgpt.api.key}")
    private String openaiApiKey;

    public MissionStatusResponse generateMission(int storyId, int budget) {
        String prompt = buildPrompt(storyId, budget);

        WebClient webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1/chat/completions")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openaiApiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> requetBody = Map.of(
                "model", "gpt-5",
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a helpful assistant that only responds in JSON format."),
                        Map.of("role", "user", "content", prompt)
                )
        );

        Map<String, Object> apiResponse = webClient.post()
                .bodyValue(requetBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        // API 응답에서 content 추출
        Map<String, Object> choice = ((List<Map<String, Object>>) apiResponse.get("choices")).get(0);
        String content = (String) ((Map<String, Object>) choice.get("message")).get("content");

        // Jackson ObjectMapper를 사용하여 JSON 문자열을 DTO로 변환
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            MissionStatusResponse response = objectMapper.readValue(content, MissionStatusResponse.class);
            response.setCode(200);
            response.setMessage("ChatGPT 응답 성공");
            return response;
        } catch (Exception e) {
            // 예외 처리 로직 (파싱 실패 시)
            throw new RuntimeException("Failed to parse ChatGPT response", e);
        }
    }

    private String buildPrompt(int storyId, int budget) {
        return String.format(
                "스토리 ID %d와 예산 %d원을 활용하여, 시장에서 구입할 간단한 음식 재료 미션 5개 이상을 생성해줘. JSON은 'missionTitle', 'missionList' 키를 포함해야 해. 'missionList'의 각 항목은 'missionDetail', 'expectedPrice', 'is_success' 키를 반드시 포함해야 해. 'missionTitle'은 음식 이름으로 작성하고, 'missionDetail'은 '~을 구매한다' 형식으로 작성해줘. 'is_success' 값은 false로 고정해줘. 응답은 오직 JSON 형식으로만 제공해줘.",
                storyId,
                budget
        );
    }

    public String classifyKeyword(String keyword) {
        if ("깻잎".equals(keyword) || "상추".equals(keyword)) {
            return "농산물";
        } else if ("고등어".equals(keyword)) {
            return "수산물";
        } else if ("바나나".equals(keyword)) {
            return "과일";
        } else {
            return "식품";
        }
    }
}