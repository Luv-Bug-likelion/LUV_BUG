package likelion.traditional_market.CreateMission.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import likelion.traditional_market.CreateMission.Dto.MissionStatusResponse;
import likelion.traditional_market.KakaoMap.dto.StoreInfoDto;
import likelion.traditional_market.common.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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
                "model", "gpt-4o",
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

        Map<String, Object> choice = ((List<Map<String, Object>>) apiResponse.get("choices")).get(0);
        String content = (String) ((Map<String, Object>) choice.get("message")).get("content");

        String cleanedContent = content.replaceAll("```json|```", "").trim();

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            MissionStatusResponse response = objectMapper.readValue(cleanedContent, MissionStatusResponse.class);
            return ApiResponse.success("ChatGPT 응답 성공", response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse ChatGPT response", e);
        }
    }

    private String buildPrompt(int storyId, int budget) {
        return String.format(
                "스토리 ID %d와 예산 %d원을 활용하여, 시장에서 구입할 음식 재료 미션 5개 이상 12개 미만으로 생성해줘. JSON은 'missionTitle', 'missionList' 키를 포함해야 해. 'missionList'의 각 항목은 'missionDetail', 'expectedPrice', 'is_success' 키를 반드시 포함해야 해. 'missionTitle'은 시장 재료로 만들 수 있는 음식 이름(한글)으로만 작성하고, 'missionDetail'은 '~을 구매한다' 형식으로 작성해줘. 'is_success' 값은 false로 고정해줘. 응답은 오직 JSON 형식으로만 제공해줘.",
                storyId,
                budget
        );
    }

    // 상점 목록을 받아 일괄 분류하는 새로운 메서드 추가
    public Mono<Map<String, String>> classifyStoresInBatch(List<StoreInfoDto> stores) {
        WebClient webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1/chat/completions")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openaiApiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        String storeListText = stores.stream()
                .map(s -> String.format("상호명: '%s', 업종: '%s'", s.getName(), s.getIndustry()))
                .collect(Collectors.joining("\n- ", "- ", ""));

        String prompt = String.format(
                "다음 상점 목록을 가장 적합한 카테고리로 분류해줘. 각 상점에 대해 상호명과 카테고리를 JSON 형태로 반환해줘. 카테고리는 '육류', '수산물', '채소', '반찬', '기타' 중 하나여야 해. '반찬'에는 시장 내에서 파는 조리된 음식 (예: 족발, 닭강정, 전, 만두)을 포함하고, 식사 메뉴를 판매하는 일반 음식점, 카페, 프랜차이즈, 서비스업 등은 '기타'로 분류해줘. \n\n%s",
                storeListText
        );

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o",
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a helpful assistant that only responds in JSON format where keys are store names and values are their categories."),
                        Map.of("role", "user", "content", prompt)
                )
        );

        return webClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(apiResponse -> {
                    try {
                        Map<String, Object> choice = (Map<String, Object>) ((List<Map<String, Object>>) apiResponse.get("choices")).get(0);
                        String content = (String) ((Map<String, Object>) choice.get("message")).get("content");

                        ObjectMapper objectMapper = new ObjectMapper();
                        if (content.startsWith("```json")) {
                            content = content.substring(7, content.lastIndexOf("```")).trim();
                        }
                        return objectMapper.readValue(content, new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse ChatGPT response for classification", e);
                    }
                });
    }
}