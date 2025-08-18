package likelion.traditional_market.Reward.Controller;

import likelion.traditional_market.common.ApiResponse;
import likelion.traditional_market.Reward.Service.RewardService;
import com.google.zxing.WriterException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reward")
public class RewardController {

    private final RewardService rewardService;

    @GetMapping
    public ResponseEntity<?> getRewardQrCode(HttpSession session) {
        String userKey = (String) session.getAttribute("userKey");
        if (userKey == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(401, "userKey가 없습니다."));
        }

        try {
            byte[] qrCodeImage = rewardService.generateRewardQrCode(userKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(qrCodeImage.length);

            return new ResponseEntity<>(qrCodeImage, headers, HttpStatus.OK);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(400, e.getMessage()));
        } catch (IOException | WriterException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(500, "QR 코드 생성 중 오류가 발생했습니다."));
        }
    }
}