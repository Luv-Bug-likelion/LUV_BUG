package likelion.traditional_market.CreateMission.Service;

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
                        Map.of("role", "system", "content", "You are a helpful assistant."),
                        Map.of("role", "user", "content", prompt)
                )
        );

        Map<String, Object> response = webClient.post()
                .bodyValue(requetBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        // 응답을 파싱하여 MissionStatusResponse DTO로 변환
        Map<String, Object> choice = ((List<Map<String, Object>>) response.get("choices")).get(0);
        String content = (String) ((Map<String, Object>) choice.get("message")).get("content");

        MissionStatusResponse missionStatusResponse = new MissionStatusResponse();
        missionStatusResponse.setMissionTitle("김치찌개 재료를 사와라!");
        missionStatusResponse.setMissionList(List.of(
                MissionDetailDto.builder().missionDetail("돼지고기 200g").expectedPrice(5000).isSuccess(false).build(),
                MissionDetailDto.builder().missionDetail("신김치 1/4포기").expectedPrice(4500).isSuccess(false).build()
        ));
        return missionStatusResponse;
    }

    private String buildPrompt(int storyId, int budget) {
        return String.format("Generate a mission for storyId %d with a budget of %d. Return a JSON object with 'missionTitle' and a list of 'missionList' containing at least 5 ingredients with their 'missionDetail' and 'expectedPrice'.", storyId, budget);
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
