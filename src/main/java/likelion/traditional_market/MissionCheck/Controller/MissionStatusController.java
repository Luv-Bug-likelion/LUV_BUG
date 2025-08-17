package likelion.traditional_market.MissionCheck.Controller;

import likelion.traditional_market.MissionCheck.Dto.MissionCheckRequest;
import likelion.traditional_market.MissionCheck.Dto.MissionCheckResponse;
import likelion.traditional_market.MissionCheck.Service.MissionStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mission")
public class MissionStatusController {
    private final MissionStatusService missionStatusService;

    @PostMapping("/status")
    public ResponseEntity<MissionCheckResponse> getSingleMissionStatus(
            @RequestBody MissionCheckRequest req)
    {
        return ResponseEntity.ok(missionStatusService.getSingleMissionStatus(req.getUserKey(),req.getMissionId()));
    }
}
