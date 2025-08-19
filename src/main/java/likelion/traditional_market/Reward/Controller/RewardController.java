package likelion.traditional_market.Reward.Controller;

import likelion.traditional_market.Reward.Dto.RewardClaimRequest;
import likelion.traditional_market.Reward.Service.RewardClaimService;
import likelion.traditional_market.common.ApiResponse;
import likelion.traditional_market.Reward.Service.RewardService;
import com.google.zxing.WriterException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reward")
public class RewardController {

    private final RewardService rewardService;
    private final RewardClaimService rewardClaimService;

    @GetMapping
    public ResponseEntity<ApiResponse<String>> getRewardQrCode(HttpSession session) {
        String userKey = (String) session.getAttribute("userKey");
        if (userKey == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(401, "userKey가 없습니다."));
        }

        try {
            String qrCodeBase64 = rewardService.generateRewardQrCode(userKey);
            return ResponseEntity.ok(ApiResponse.success("QR 코드 생성 성공", qrCodeBase64));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(400, e.getMessage()));
        } catch (IOException | WriterException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(500, "QR 코드 생성 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/claim")
    public ResponseEntity<ApiResponse<String>> claimReward(@RequestBody RewardClaimRequest request) {
        if (rewardClaimService.claimReward(request.getUserKey(), request.getToken())) {
            return ResponseEntity.ok(ApiResponse.success("보상이 성공적으로 지급되었습니다.", null));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "유효하지 않거나 이미 사용된 QR 코드입니다."));
        }
    }
}