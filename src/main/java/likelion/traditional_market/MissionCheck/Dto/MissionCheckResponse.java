package likelion.traditional_market.MissionCheck.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MissionCheckResponse {
    private int code;
    private String message;

    private int missionId;
    private String missionDetail;

    @JsonProperty("spent_amount")
    private int spent_amount;

    private boolean is_success;
}
