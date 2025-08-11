package likelion.traditional_market.KakaoMap.service;

import likelion.traditional_market.CreateMission.Service.ChatGptService;
import likelion.traditional_market.KakaoMap.dto.StoreInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocationService {
    @Value("${kakao.api.key}")
    private String kakaoApiKey;

    private final ChatGptService chatGptService;

    public Map<String, List<StoreInfoDto>> searchStores(List<String> keywords, String market) {
        WebClient webClient = WebClient.builder()
                .baseUrl("https://dapi.kakao.com/v2/local")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoApiKey)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

        // 시장 좌표 추출
        Optional<Map<String, Object>> marketLocation = getMarketCoordinates(webClient, market);
        if (marketLocation.isEmpty()) {
            return keywords.stream().collect(Collectors.toMap(
                    keyword -> keyword,
                    keyword -> List.of(),
                    (existing, replacement) -> existing // 중복 키가 발생해도 기존 값을 유지
            ));
        }
        String marketX = (String) marketLocation.get().get("x");
        String marketY = (String) marketLocation.get().get("y");

        // 키워드별 상점 검색
        return keywords.stream().collect(Collectors.toMap(
                keyword -> keyword,
                keyword -> {
                    List<String> searchCategories = mapKeywordToCategory(keyword);
                    Set<StoreInfoDto> uniqueStores = new HashSet<>();
                    for (String category : searchCategories) {
                        uniqueStores.addAll(searchStoresByKeyword(webClient, category, marketX, marketY));
                    }
                    return new ArrayList<>(uniqueStores);
                }
        ));
    }

    private List<String> mapKeywordToCategory(String keyword) {
        List<String> categories = switch (keyword) {
            case "돼지고기", "소고기" -> List.of("축산", "정육점");
            case "오징어", "새우", "고등어", "명태" -> List.of("수산물");
            case "양파", "대파", "무", "배추" -> List.of("농산물", "야채");
            case "사과", "배" -> List.of("과일");
            case "김치", "두부" -> List.of("식품");
            default -> {
                // GPT로 분류한 키워드를 사용
                String gptCategory = chatGptService.classifyKeyword(keyword);
                yield switch (gptCategory) {
                    case "축산" -> List.of("축산", "정육점");
                    case "수산물" -> List.of("수산물");
                    case "농산물" -> List.of("농산물", "야채");
                    case "과일" -> List.of("과일");
                    case "식품" -> List.of("식품");
                    default -> List.of(keyword);
                };
            }
        };
        return categories;
    }

    private Optional<Map<String, Object>> getMarketCoordinates(WebClient webClient, String marketName) {
        Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/keyword.json")
                        .queryParam("query", marketName)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        List<Map<String, Object>> documents = (List<Map<String, Object>>) response.get("documents");
        return documents.stream().findFirst();
    }

    private List<StoreInfoDto> searchStoresByKeyword(WebClient webClient, String keyword, String x, String y) {
        Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/keyword.json")
                        .queryParam("query", keyword)
                        .queryParam("x", x)
                        .queryParam("y", y)
                        .queryParam("radius", 300) // 300m 반경 내 검색
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        List<Map<String, Object>> documents = (List<Map<String, Object>>) response.get("documents");
        return documents.stream()
                .map(doc -> {
                    StoreInfoDto store = new StoreInfoDto();
                    store.setName((String) doc.get("place_name"));
                    store.setAddress((String) doc.get("address_name"));
                    store.setPhoneNumber((String) doc.get("phone"));
                    store.setX((String) doc.get("x"));
                    store.setY((String) doc.get("y"));
                    return store;
                })
                .collect(Collectors.toList());
    }
}