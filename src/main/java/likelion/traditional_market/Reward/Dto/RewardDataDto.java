package likelion.traditional_market.Reward.Dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class RewardDataDto {
    private String userKey;
    private String market;
    private int totalSpent;
    private int rewardAmount;
    private String rewardToken; // 일회용 만료 기능을 위해 유지
}