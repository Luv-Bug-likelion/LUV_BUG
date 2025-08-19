package likelion.traditional_market.Reward.Service;

import likelion.traditional_market.Reward.Entity.RewardTokens;
import likelion.traditional_market.Reward.Repository.RewardTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RewardClaimService {

    private final RewardTokenRepository rewardTokenRepository;

    @Transactional
    public boolean claimReward(String userKey, String token) {
        RewardTokens rewardToken = rewardTokenRepository.findByTokenAndUserKey(token, userKey)
                .orElse(null);

        // 1. 토큰이 존재하지 않거나
        // 2. 이미 사용된 경우
        if (rewardToken == null || rewardToken.getStatus() == RewardTokens.RewardStatus.USED) {
            return false; // 보상 청구 실패
        }

        // 3. 토큰 상태를 'USED'로 변경
        rewardToken.setStatus(RewardTokens.RewardStatus.USED);
        rewardToken.setUsedAt(LocalDateTime.now());
        rewardTokenRepository.save(rewardToken);

        // TODO: 사용자에게 실제 보상(쿠폰 등)을 지급하는 로직 추가

        return true; // 보상 청구 성공
    }
}