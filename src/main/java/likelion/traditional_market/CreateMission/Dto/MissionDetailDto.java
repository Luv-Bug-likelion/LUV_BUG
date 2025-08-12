package likelion.traditional_market.CreateMission.Dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MissionDetailDto {

    private int missionId;
    private String missionDetail;
    private int expectedPrice;
    private boolean isSuccess;
}
