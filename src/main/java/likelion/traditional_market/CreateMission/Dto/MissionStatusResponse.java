package likelion.traditional_market.CreateMission.Dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissionStatusResponse {
    private int code;
    private String message;
    private String missionTitle;
    private List<MissionDetailDto> missionList;
    private int totalSpent;
    private int missionCompleteCount;
}
