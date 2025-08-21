// ReceiptService.java

package likelion.traditional_market.Receipt.Service;

import jakarta.transaction.Transactional;
import likelion.traditional_market.CreateMission.Entity.UserMission;
import likelion.traditional_market.CreateMission.Repository.UserMissionRepository;
import likelion.traditional_market.Receipt.Dto.ExtractedFields;
import likelion.traditional_market.Receipt.Dto.ReceiptCheckResponse;
import likelion.traditional_market.UserKeyIssue.Entity.User;
import likelion.traditional_market.UserKeyIssue.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiptService {
    private final ClovaOcrClient clovaOcrClient;
    private final ReceiptParser receiptParser;
    private final ReceiptScorer receiptScorer;
    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");
    private final UserMissionRepository userMissionRepository;
    private final UserRepository userRepository;
    @Transactional
    public ReceiptCheckResponse handleReceiptCheck(MultipartFile image, String userKey, Integer missionId) {
        try {
            if (image == null || image.isEmpty()) {
                return ReceiptCheckResponse.fail("이미지가 비어있습니다.");
            }
            byte[] imgBytes = image.getBytes();
            String filename = image.getOriginalFilename() == null ? "unknown.jpg" : image.getOriginalFilename();
            String ocrJson = clovaOcrClient.recognize(filename, imgBytes);

            ExtractedFields ex = receiptParser.parse(ocrJson);

            List<String> hits = receiptParser.findKeywordHits(ocrJson);
            int score = receiptScorer.score(ex, hits);

            if (ex.getVisitDate() != null && ex.getVisitDate().isAfter(LocalDate.now(ZONE_SEOUL))) {
                return ReceiptCheckResponse.fail("영수증 날짜가 미래입니다. 재촬영해주세요.");
            }
            boolean hasCoreField = (ex.getSpentAmount() != null) || (ex.getVisitDate() != null);
            if (!hasCoreField) {
                return ReceiptCheckResponse.fail("핵심 정보(날짜/금액)를 인식하지 못했습니다. 재촬영해주세요.");
            }
            String status = (score >= 7) ? "SUCCESS" : "FAIL";

            if ("SUCCESS".equals(status)) {
                UserMission userMission = userMissionRepository.findByUserKeyAndMissionId(userKey, missionId)
                        .orElseThrow(() -> new IllegalArgumentException("User mission not found"));
                userMission.setSuccess(true);
                userMissionRepository.save(userMission);

                User user = userRepository.findById(userKey)
                        .orElseThrow(() -> new IllegalArgumentException("User not found"));
                user.setTotalSpent(user.getTotalSpent() + ex.getSpentAmount());
                user.setMissionCompleteCount(user.getMissionCompleteCount() + 1);
                userRepository.save(user);

                return ReceiptCheckResponse.success("영수증 인증 성공", score, ex);
            }

            return ReceiptCheckResponse.fail("영수증 인식이 충분하지 않습니다. 재촬영해주세요.");

        }
        catch (IllegalArgumentException e) {
            log.warn("데이터 조회 실패: {}",e.getMessage());
            return ReceiptCheckResponse.fail("요청 처리 중 오류 발생: "+e.getMessage());
        }
        catch(IOException e){
            log.error("이미지 처리 중 오류 발생",e);
            return ReceiptCheckResponse.fail("이미지 처리 중 오류 발생");
        }
        catch(Exception e){
            log.error("알 수 없는 오류 발생",e);
            return ReceiptCheckResponse.fail("OCR 처리 중 알 수 없는 오류 발생");
        }
    }
    @Transactional
    public ReceiptCheckResponse handleReceiptTest(
            String userKey, int missionId, ExtractedFields fields, List<String> keywordHits) {

        int score = receiptScorer.score(fields, keywordHits);
        String status = (score >= 7) ? "SUCCESS" : "FAIL";

        // 미션 성공 시, isSuccess 값 업데이트
        if ("SUCCESS".equals(status)) {
            UserMission userMission = userMissionRepository.findByUserKeyAndMissionId(userKey, missionId)
                    .orElseThrow(() -> new IllegalArgumentException("User mission not found"));
            userMission.setSuccess(true);
            userMission.setSpentAmount(fields.getSpentAmount()); // 지출 금액 업데이트
            userMissionRepository.save(userMission);

            User user = userRepository.findById(userKey)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            user.setTotalSpent(user.getTotalSpent() + fields.getSpentAmount());
            user.setMissionCompleteCount(user.getMissionCompleteCount() + 1);
            userRepository.save(user);

            return ReceiptCheckResponse.success("영수증 인증 성공", score, fields);
        }

        return ReceiptCheckResponse.fail("테스트 실패");
    }
}