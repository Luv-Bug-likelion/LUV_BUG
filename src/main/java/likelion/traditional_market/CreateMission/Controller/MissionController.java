package likelion.traditional_market.CreateMission.Controller;

import jakarta.servlet.http.HttpSession;
import likelion.traditional_market.CreateMission.Dto.MissionStatusResponse;
import likelion.traditional_market.CreateMission.Service.MissionService;
import likelion.traditional_market.KakaoMap.dto.MarketStoresResponse;
import likelion.traditional_market.KakaoMap.dto.StoreInfoDto;
import likelion.traditional_market.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mission")
public class MissionController {

    private final MissionService missionService;

    @GetMapping
    public ResponseEntity<ApiResponse<MissionStatusResponse>> getMissionStatus(HttpSession session) {
        String userKey = (String) session.getAttribute("userKey");
        if (userKey == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "userKey가 없습니다."));
        }
        ApiResponse<MissionStatusResponse> response = missionService.generateMissionForUser(userKey);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stores")
    public ResponseEntity<ApiResponse<Map<String, List<StoreInfoDto>>>> getStores(HttpSession session) {
        String userKey = (String) session.getAttribute("userKey");
        if (userKey == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "userKey가 없습니다."));
        }

        ApiResponse<Map<String, List<StoreInfoDto>>> storeInfoMap = missionService.getStoresForMissions(userKey);
        return ResponseEntity.ok(storeInfoMap);
    }
    @GetMapping("/stores/v2")
    public ResponseEntity<ApiResponse<MarketStoresResponse>> getStoresV2(
            HttpSession session,
            @RequestParam(defaultValue = "") String signPost
    ) {
        String userKey = (String) session.getAttribute("userKey");
        if (userKey == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "userKey가 없습니다."));
        }
        // 내부에서 MissionService가 userKey 기반으로
        // - 시장명(marketName)
        // - 키워드 리스트(keywords)
        // 를 뽑아 LocationService.getMarketStores(...) 호출하도록 구현
        ApiResponse<MarketStoresResponse> resp = missionService.getMarketStoresForMissions(userKey, signPost);
        return ResponseEntity.ok(resp);
    }
}