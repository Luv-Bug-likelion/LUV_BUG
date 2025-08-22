package likelion.traditional_market.CreateMission.Controller;

import jakarta.servlet.http.HttpSession;
import likelion.traditional_market.CreateMission.Dto.MissionStatusResponse;
import likelion.traditional_market.CreateMission.Service.MissionService;
import likelion.traditional_market.KakaoMap.dto.MarketStoreListResponse;
import likelion.traditional_market.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

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
    public Mono<ResponseEntity<ApiResponse<MarketStoreListResponse>>> getStores(HttpSession session) {
        String userKey = (String) session.getAttribute("userKey");
        if (userKey == null) {
            return Mono.just(ResponseEntity.badRequest().body(ApiResponse.error(400, "userKey가 없습니다.")));
        }
        return missionService.getAllFoodStores(userKey)
                .map(ResponseEntity::ok);
    }
}