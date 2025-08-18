package likelion.traditional_market.Reward.Dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MissionRewardDto {
    private int missionId;
    private String missionDetail;
    private int spentAmount;
    private String receiptImgUrl;
}