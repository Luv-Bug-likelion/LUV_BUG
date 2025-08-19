package likelion.traditional_market.Reward.Repository;

import likelion.traditional_market.Reward.Entity.RewardTokens;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RewardTokenRepository extends JpaRepository<RewardTokens, Long> {
    Optional<RewardTokens> findByTokenAndUserKey(String token, String userKey);
    boolean existsByUserKey(String userKey);
}