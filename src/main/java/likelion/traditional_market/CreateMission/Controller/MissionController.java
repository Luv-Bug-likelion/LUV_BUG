package likelion.traditional_market.CreateMission.Controller;

import jakarta.servlet.http.HttpSession;
import likelion.traditional_market.CreateMission.Dto.MissionStatusResponse;
import likelion.traditional_market.CreateMission.Service.MissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mission")
public class MissionController {

    private final MissionService missionService;

    @GetMapping
    public ResponseEntity<MissionStatusResponse> getMissionStatus(HttpSession session) {
        String userKey = (String) session.getAttribute("userKey");
        if (userKey == null) {
            return ResponseEntity.badRequest().body(null);
        }

        MissionStatusResponse response = missionService.generateMissionForUser(userKey);
        return ResponseEntity.ok(response);
    }
}
