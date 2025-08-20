package likelion.traditional_market.CreateMission.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import likelion.traditional_market.CreateMission.Dto.MissionStatusResponse;
import likelion.traditional_market.KakaoMap.dto.StoreInfoDto;
import likelion.traditional_market.common.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatGptService {

    @Value("${chatgpt.api.key}")
    private String openaiApiKey;

    public ApiResponse<MissionStatusResponse> generateMission(int storyId, int budget) {
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

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String, Object> choice = ((List<Map<String, Object>>) apiResponse.get("choices")).get(0);
            String content = (String) ((Map<String, Object>) choice.get("message")).get("content");
            MissionStatusResponse response = objectMapper.readValue(content, MissionStatusResponse.class);
            return ApiResponse.success("ChatGPT 응답 성공", response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse ChatGPT response", e);
        }
    }

    private String buildPrompt(int storyId, int budget) {
        return String.format(
                "스토리 ID %d와 예산 %d원을 활용하여, 시장에서 구입할 간단한 음식 재료 미션 5개 이상을 생성해줘. JSON은 'missionTitle', 'missionList' 키를 포함해야 해. 'missionList'의 각 항목은 'missionDetail', 'expectedPrice', 'is_success' 키를 반드시 포함해야 해. 'missionDetail'은 '~을 구매한다' 형식으로 작성해줘. 'is_success' 값은 false로 고정해줘. 응답은 오직 JSON 형식으로만 제공해줘.",
                storyId,
                budget
        );
    }

    // GPT에 단일 요청으로 여러 상점 분류를 요청하도록 수정
    public Map<String, String> classifyKeywords(List<StoreInfoDto> stores) {
        String prompt = buildClassificationPrompt(stores);

        WebClient webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1/chat/completions")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openaiApiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-5",
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a helpful assistant that classifies store information into a JSON format with category names."),
                        Map.of("role", "user", "content", prompt)
                )
        );

        Map<String, Object> apiResponse = webClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        try {
            Map<String, Object> choice = ((List<Map<String, Object>>) apiResponse.get("choices")).get(0);
            String content = (String) ((Map<String, Object>) choice.get("message")).get("content");
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(content, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse ChatGPT response for classification", e);
        }
    }

    private String buildClassificationPrompt(List<StoreInfoDto> stores) {
        String storeInfo = stores.stream()
                .map(store -> String.format("{\"name\":\"%s\", \"industry\":\"%s\"}", store.getName(), store.getIndustry()))
                .collect(Collectors.joining(","));

        return String.format(
                "다음 상점 목록의 이름과 업종 정보를 참고하여 가장 적합한 카테고리로 분류해줘. 카테고리는 '육류', '수산물', '채소', '반찬', '기타' 중 하나여야 해. '반찬'은 '유통'이라는 상호명과 식품판매 업종으로도 나올 수 있어, 식사 메뉴를 판매하는 일반 음식점 (예: 김밥집, 국수집, 분식집, 정육점이 아닌 일반 고깃집), 카페, 프랜차이즈, 서비스업 등은 '기타'로 분류해줘. 답변은 상점 이름과 카테고리를 JSON 형식으로 묶어서 제공해줘. 예: {\"엄지농산물 역곡중국식품\":\"채소\", \"금산수산\":\"수산물\"}. 상점 목록: [%s]",
                storeInfo
        );
    }
}