package likelion.traditional_market.controller;

import likelion.traditional_market.dto.MissionRequestDto;
import likelion.traditional_market.dto.StoreInfoDto;
import likelion.traditional_market.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/story")
@RequiredArgsConstructor
public class LocationController {
    private final LocationService locationService;
    @PostMapping("/location")
    public ResponseEntity<Map<String, List<StoreInfoDto>>>

    getMissionStores(@RequestBody MissionRequestDto request){
//        실제 db데이터 호출할때 사용할 코드_Mock데이터 호출시 주석처리해야함
        Map<String, List<StoreInfoDto>> response =
                locationService.getStoreInfo(request.getUserKey());

//      Mock데이터 호출_db연결 시 삭제
//      Map<String, List<StoreInfoDto>> response =
//              locationService.getMockStoreInfo(request.getUserKey());

        return ResponseEntity.ok(response);
        }
}
