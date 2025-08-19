package likelion.traditional_market.Reward.Dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardClaimRequest {
    private String userKey;
    private String token;
}