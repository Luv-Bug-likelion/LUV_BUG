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
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

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

    private Mono<Optional<Map<String, Object>>> getMarketCoordinatesAsync(WebClient webClient, String marketName) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/keyword.json")
                        .queryParam("query", marketName)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(response -> {
                    List<Map<String, Object>> documents = (List<Map<String, Object>>) response.get("documents");
                    return documents.stream().findFirst();
                });
    }

    public Mono<Map<String,String>> findSubwayAsync(String x, String y){
        return kakaoClient().get()
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
                .map(response -> {
                    List<Map<String,Object>> documents  = (List<Map<String, Object>>) response.get("documents");
                    if(documents.isEmpty()){
                        return Map.of();
                    }
                    Map<String,String> result = new HashMap<>();
                    result.put("name",(String)documents.get(0).get("place_name"));
                    result.put("distance",(String)documents.get(0).get("distance"));
                    return result;
                });
    }

    public Mono<Map<String, List<StoreInfoDto>>> getAllStoresByMarketAndCategorizeAsync(String marketName, int radius) {
        WebClient webClient = kakaoClient();

        List<String> keywords = List.of("시장", "마트", "정육점", "수산물", "농산물", "반찬", "식료품", "식당", "분식");

        return getMarketCoordinatesAsync(webClient, marketName)
                .flatMap(marketLocationOpt -> {
                    if (marketLocationOpt.isEmpty()) {
                        return Mono.just(Map.of("전체", List.of(), "육류", List.of(), "수산물", List.of(), "채소", List.of(), "반찬", List.of()));
                    }

                    String marketX = (String) marketLocationOpt.get().get("x");
                    String marketY = (String) marketLocationOpt.get().get("y");

                    List<Mono<List<StoreInfoDto>>> storeMonos = keywords.stream()
                            .map(keyword -> searchStoresByKeywordAsync(webClient, keyword, marketX, marketY, radius))
                            .collect(Collectors.toList());

                    return Mono.zip(storeMonos, (Object[] results) ->
                                    Arrays.stream(results)
                                            .map(result -> (List<StoreInfoDto>) result)
                                            .collect(Collectors.toList())
                            )
                            .map(allStoresList -> allStoresList.stream()
                                    .flatMap(List::stream)
                                    .collect(Collectors.toSet()))
                            .map(allStores -> {
                                Map<String, List<StoreInfoDto>> categorizedStores = new HashMap<>();
                                categorizedStores.put("육류", new ArrayList<>());
                                categorizedStores.put("수산물", new ArrayList<>());
                                categorizedStores.put("채소", new ArrayList<>());
                                categorizedStores.put("반찬", new ArrayList<>());

                                for (StoreInfoDto store : allStores) {
                                    String category = classifyByStoreInfo(store);
                                    if (categorizedStores.containsKey(category)) {
                                        categorizedStores.get(category).add(store);
                                    }
                                }
                                return categorizedStores;
                            });
                });
    }

    private Mono<List<StoreInfoDto>> searchStoresByKeywordAsync(WebClient webClient, String keyword, String x, String y, int radius) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/keyword.json")
                        .queryParam("query", keyword)
                        .queryParam("x", x)
                        .queryParam("y", y)
                        .queryParam("radius", radius)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .flatMapMany(response -> {
                    List<Map<String, Object>> documents = (List<Map<String, Object>>) response.get("documents");
                    return Flux.fromIterable(documents);
                })
                .flatMap(doc -> {
                    StoreInfoDto store = new StoreInfoDto();
                    store.setName((String) doc.get("place_name"));
                    store.setAddress((String) doc.get("address_name"));
                    store.setPhoneNumber((String) doc.get("phone"));
                    store.setX((String) doc.get("x"));
                    store.setY((String) doc.get("y"));
                    store.setIndustry((String) doc.get("category_name"));

                    return findSubwayAsync(store.getX(), store.getY())
                            .map(subwayInfo -> {
                                if (!subwayInfo.isEmpty()) {
                                    store.setSubwayName(subwayInfo.get("name"));
                                    store.setSubwayDistance(subwayInfo.get("distance")+"m");
                                }
                                return store;
                            });
                })
                .collectList();
    }

    // StoreInfoDto 객체를 받아 상점 이름과 업종 정보를 모두 활용하도록 수정
    private String classifyByStoreInfo(StoreInfoDto store) {
        String name = store.getName() != null ? store.getName().toLowerCase() : "";
        String industry = store.getIndustry() != null ? store.getIndustry().toLowerCase() : "";

        if (name.contains("정육") || name.contains("축산") || industry.contains("정육점") || industry.contains("축산")) {
            return "육류";
        }
        if (name.contains("수산") || industry.contains("수산물")) {
            return "수산물";
        }
        if (name.contains("농산") || name.contains("채소") || name.contains("과일") || industry.contains("농산물") || industry.contains("채소") || industry.contains("과일")) {
            return "채소";
        }
        if (name.contains("반찬") || name.contains("식료") || industry.contains("반찬") || industry.contains("식료품")) {
            return "반찬";
        }
        return "기타";
    }
}