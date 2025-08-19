package likelion.traditional_market.Reward.Repository;

import likelion.traditional_market.Reward.Entity.RewardToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RewardTokenRepository extends JpaRepository<RewardToken, Long> {
    Optional<RewardToken> findByTokenAndUserKey(String token, String userKey);
}