package likelion.traditional_market.KakaoMap.service;

import likelion.traditional_market.CreateMission.Service.ChatGptService;
import likelion.traditional_market.KakaoMap.dto.MarketStoresResponse;
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

    private WebClient kakaoClient() {
        return WebClient.builder()
                .baseUrl("https://dapi.kakao.com/v2/local")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoApiKey)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Map<String, List<StoreInfoDto>> searchStores(List<String> keywords, String market) {
        WebClient webClient = kakaoClient();

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
    
    //프론트Mock데이터 구조로 포장
    public MarketStoresResponse getMarketStores(String marketName, String signPost, List<String> keywords) {
        WebClient webClient = kakaoClient();

        // 시장 좌표
        Optional<Map<String, Object>> marketLocation = getMarketCoordinates(webClient, marketName);
        if (marketLocation.isEmpty()) {
            return MarketStoresResponse.builder()
                    .marketName(marketName)
                    .signPost(signPost)
                    .meat(List.of())
                    .fish(List.of())
                    .vegetable(List.of())
                    .fruit(List.of())
                    .build();
        }
        String marketX = (String) marketLocation.get().get("x");
        String marketY = (String) marketLocation.get().get("y");

        // 버킷 (중복 제거 위해 Set 사용 권장)
        Set<StoreInfoDto> meat = new HashSet<>();
        Set<StoreInfoDto> fish = new HashSet<>();
        Set<StoreInfoDto> vegetable = new HashSet<>();
        Set<StoreInfoDto> fruit = new HashSet<>();

        // 기존 “카테고리 매핑 로직” 그대로 활용
        for (String keyword : Optional.ofNullable(keywords).orElseGet(List::of)) {
            List<String> categories = mapKeywordToCategory(keyword); // ← 기존 메서드 그대로 사용
            for (String category : categories) {
                Bucket bucket = toBucket(category);     // ↓ 헬퍼
                if (bucket == Bucket.NONE) continue;

                List<StoreInfoDto> found = searchStoresByKeyword(webClient, category, marketX, marketY); // ← 기존 메서드 그대로 사용
                String label = industryLabel(bucket);   // ↓ 헬퍼

                for (StoreInfoDto s : found) {
                    StoreInfoDto copy = new StoreInfoDto();
                    copy.setName(s.getName());
                    copy.setAddress(s.getAddress());
                    copy.setPhoneNumber(s.getPhoneNumber());
                    copy.setX(s.getX());
                    copy.setY(s.getY());
                    copy.setIndustry(label); // 업종 라벨 통일
                    switch (bucket) {
                        case MEAT      -> meat.add(copy);
                        case FISH      -> fish.add(copy);
                        case VEGETABLE -> vegetable.add(copy);
                        case FRUIT     -> fruit.add(copy);
                        default -> {}
                    }
                }
            }
        }

        return MarketStoresResponse.builder()
                .marketName(marketName)
                .signPost(signPost)
                .meat(new ArrayList<>(meat))
                .fish(new ArrayList<>(fish))
                .vegetable(new ArrayList<>(vegetable))
                .fruit(new ArrayList<>(fruit))
                .build();
    }

    private enum Bucket { MEAT, FISH, VEGETABLE, FRUIT, NONE }

    private Bucket toBucket(String category) {
        return switch (category) {
            case "축산", "정육점" -> Bucket.MEAT;
            case "수산물"        -> Bucket.FISH;
            case "농산물", "야채" -> Bucket.VEGETABLE;
            case "과일"          -> Bucket.FRUIT;
            default              -> Bucket.NONE; // "식품" 등은 프론트 버킷 제외
        };
    }

    private String industryLabel(Bucket b) {
        return switch (b) {
            case MEAT      -> "정육점";
            case FISH      -> "수산물 가게";
            case VEGETABLE -> "채소 가게";
            case FRUIT     -> "과일 가게";
            default        -> "";
        };
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
                    Optional<Map<String,String>> subwayInfo = findSubway(store.getX(), store.getY());
                    subwayInfo.ifPresent(info->{
                            store.setSubwayName(info.get("name"));
                            store.setSubwayDistance(info.get("distance")+"m");
                });
                    return store;
                })
                .collect(Collectors.toList());
    }
    public Optional<Map<String,String>> findSubway(String x, String y){
        Map<String,Object> response = kakaoClient().get()
                .uri(uriBuilder -> uriBuilder
                .path("/search/category.json")
                .queryParam("category_group_code","SW8")
                .queryParam("x",x)
                .queryParam("y",y)
                .queryParam("radius",1000)
                .queryParam("sort","distance")
                .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
        List<Map<String,Object>> documents  = (List<Map<String, Object>>) response.get("documents");
        if(documents.isEmpty()){
            return Optional.empty();
        }
        Map<String,String> result = new HashMap<>();
        result.put("name",(String)documents.get(0).get("place_name"));
        result.put("distance",(String)documents.get(0).get("distance"));
        return Optional.of(result);
    }
}