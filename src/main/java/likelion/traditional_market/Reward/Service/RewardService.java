package likelion.traditional_market.Reward.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.fasterxml.jackson.databind.ObjectMapper;
import likelion.traditional_market.CreateMission.Entity.Mission;
import likelion.traditional_market.CreateMission.Entity.UserMission;
import likelion.traditional_market.CreateMission.Repository.MissionRepository;
import likelion.traditional_market.CreateMission.Repository.UserMissionRepository;
import likelion.traditional_market.Reward.Dto.MissionRewardDto;
import likelion.traditional_market.Reward.Dto.RewardDataDto;
import likelion.traditional_market.Reward.Entity.RewardTokens;
import likelion.traditional_market.Reward.Repository.RewardTokenRepository;
import likelion.traditional_market.UserKeyIssue.Entity.User;
import likelion.traditional_market.UserKeyIssue.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RewardService {

    private final UserRepository userRepository;
    private final UserMissionRepository userMissionRepository;
    private final MissionRepository missionRepository;
    private final RewardTokenRepository rewardTokenRepository;

    @Transactional
    public String generateRewardQrCode(String userKey) throws WriterException, IOException {
        User user = userRepository.findById(userKey)
                .orElseThrow(() -> new IllegalArgumentException("User not found with key: " + userKey));

        // 발급받은 사용자 검증
        if (rewardTokenRepository.existsByUserKey(userKey)) {
            throw new IllegalStateException("이미 리워드 QR 코드를 발급받았습니다. 하나의 계정으로 한 번만 발급할 수 있습니다.");
        }

        // 미션 완료 횟수가 3회 미만일 경우 예외 발생
        if (user.getMissionCompleteCount() < 3) {
            throw new IllegalStateException("미션 완료 횟수가 3회 미만입니다. 현재: " + user.getMissionCompleteCount() + "회");
        }

        // 고유 토큰 생성 및 DB 저장
        String rewardToken = UUID.randomUUID().toString();
        rewardTokenRepository.save(new RewardTokens(rewardToken, userKey));

        // 사용자의 모든 미션 정보 조회
        List<UserMission> userMissions = userMissionRepository.findByUserKey(userKey);
        List<Integer> completedMissionIds = userMissions.stream()
                .filter(UserMission::isSuccess)
                .map(UserMission::getMissionId)
                .collect(Collectors.toList());

        List<Mission> missions = missionRepository.findAllById(completedMissionIds);

        // QR 코드에 담을 미션 상세 정보 DTO 리스트 생성
        List<MissionRewardDto> missionDataList = missions.stream()
                .map(mission -> {
                    UserMission userMission = userMissions.stream()
                            .filter(um -> um.getMissionId() == mission.getMissionId())
                            .findFirst()
                            .orElse(null);

                    int spentAmount = (userMission != null) ? userMission.getSpentAmount() : 0;
                    String receiptImgUrl = (userMission != null) ? userMission.getReceiptImgUrl() : null;

                    return MissionRewardDto.builder()
                            .missionId(mission.getMissionId())
                            .missionDetail(mission.getMissionDetail())
                            .spentAmount(spentAmount)
                            .receiptImgUrl(receiptImgUrl)
                            .build();
                })
                .collect(Collectors.toList());

        // 리워드 금액 계산 (총 사용 금액의 10%)
        int rewardAmount = (int) (user.getTotalSpent() * 0.1);

        // QR 코드에 인코딩할 최종 데이터 DTO 생성
        RewardDataDto qrData = RewardDataDto.builder()
                .userKey(user.getUserKey())
                .market(user.getMarket())
                .totalSpent(user.getTotalSpent())
                .rewardAmount(rewardAmount) // 새로 추가된 필드에 값 할당
                .missionCompleteCount(user.getMissionCompleteCount())
                .rewardToken(rewardToken)
                .missions(missionDataList)
                .build();

        // DTO를 JSON 문자열로 변환
        String jsonString = new ObjectMapper().writeValueAsString(qrData);

        // 한글 인코딩을 위한 힌트 추가
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8"); // UTF-8 힌트 추가

        BitMatrix bitMatrix = new MultiFormatWriter().encode(
                jsonString,
                BarcodeFormat.QR_CODE,
                200,
                200,
                hints // 힌트 맵 전달
        );

        // QR 코드 이미지를 byte 배열로 변환
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        byte[] qrCodeImage = pngOutputStream.toByteArray();

        return Base64.getEncoder().encodeToString(qrCodeImage);
    }
}