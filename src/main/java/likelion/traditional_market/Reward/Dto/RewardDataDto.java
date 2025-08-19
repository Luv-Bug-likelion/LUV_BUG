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
    private int missionCompleteCount;
    private String rewardToken;
    private List<MissionRewardDto> missions;
}