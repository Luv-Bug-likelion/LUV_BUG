package likelion.traditional_market.Reward.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class RewardTokens {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String token;

    private String userKey;

    @Enumerated(EnumType.STRING)
    private RewardStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime usedAt;

    public enum RewardStatus {
        UNUSED, USED
    }

    public RewardTokens(String token, String userKey) {
        this.token = token;
        this.userKey = userKey;
        this.status = RewardStatus.UNUSED;
        this.createdAt = LocalDateTime.now();
    }
}