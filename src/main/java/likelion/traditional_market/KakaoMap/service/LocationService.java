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

    public Mono<List<StoreInfoDto>> getAllFoodStoresInRadiusAsync(String marketName, int radius) {
        WebClient webClient = kakaoClient();

        List<String> broadKeywords = List.of("시장", "상점", "가게", "점포", "음식점", "마트", "정육점", "축산", "수산물", "횟집", "야채", "농산물", "과일", "건어물", "두부", "방앗간");

        return getMarketCoordinatesAsync(webClient, marketName)
                .flatMap(marketLocationOpt -> {
                    if (marketLocationOpt.isEmpty()) {
                        return Mono.just(List.of());
                    }

                    String marketX = (String) marketLocationOpt.get().get("x");
                    String marketY = (String) marketLocationOpt.get().get("y");

                    List<Mono<List<StoreInfoDto>>> storeMonos = broadKeywords.stream()
                            .map(keyword -> searchStoresByKeywordAsync(webClient, keyword, marketX, marketY, radius))
                            .collect(Collectors.toList());

                    return Mono.zip(storeMonos, (Object[] results) ->
                            Arrays.stream(results)
                                    .map(result -> (List<StoreInfoDto>) result)
                                    .flatMap(List::stream)
                                    .distinct()
                                    .collect(Collectors.toList())
                    );
                });
    }
}