package likelion.traditional_market.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import likelion.traditional_market.dto.StoreInfoDto;
import likelion.traditional_market.entity.MissionItem;
import likelion.traditional_market.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LocationService {
    @Value("${kakao.api.key}")
    private String kakaoapikey;
    private static final String BASE_URL = "https://dapi.kakao.com/v2/local/search/keyword.json";
    private final MissionRepository missionRepository;

    public Map<String,List<StoreInfoDto>> getStoreInfo(String userKey){
        List<MissionItem> items = missionRepository.findByMission_User_UserKey(userKey);
        Map<String,List<StoreInfoDto>> result = new LinkedHashMap<>();
        for(MissionItem item:items){
            List<StoreInfoDto> stores = searchStores(item.getItemName());
            result.put(item.getItemName(),stores);
        }
        return result;
    }

    public List<StoreInfoDto> searchStores(String keyword){
    WebClient client = WebClient.builder()
            .baseUrl(BASE_URL)
            .defaultHeader("Authorization", "KakaoAK " + kakaoapikey)
            .build();
        String response = client.get()
                .uri(uriBuilder -> uriBuilder.queryParam("query", keyword).build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return parseResponse(response);
    }

    private List<StoreInfoDto> parseResponse(String jsonResponse) {
        ObjectMapper mapper = new ObjectMapper();
        List<StoreInfoDto> result = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(jsonResponse);
            JsonNode documents = root.get("documents");

            for (JsonNode doc : documents) {
                result.add(new StoreInfoDto(
                        doc.get("place_name").asText(),
                        doc.get("address_name").asText(),
                        doc.hasNonNull("phone") ? doc.get("phone").asText() : "정보 없음",
                        doc.get("x").asText(),
                        doc.get("y").asText()
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

//    Mock데이터_DB연동 시 제거_Postman확인용
//    public Map<String, List<StoreInfoDto>> getMockStoreInfo(String userKey) {
//        Map<String, List<StoreInfoDto>> mockData = new LinkedHashMap<>();
//        List<StoreInfoDto> appleStores = List.of(
//                new StoreInfoDto("사과가게1", "서울시 중구", "010-1234-5678", "127.001", "37.501"),
//                new StoreInfoDto("사과가게2", "서울시 종로구", "010-9876-5432", "127.002", "37.502")
//        );
//        List<StoreInfoDto> bananaStores = List.of(
//                new StoreInfoDto("바나나가게1", "서울시 강남구", "010-0000-1111", "127.003", "37.503")
//        );
//        mockData.put("사과", appleStores);
//        mockData.put("바나나", bananaStores);
//        return mockData;
//    }
}
