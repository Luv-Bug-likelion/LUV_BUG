package likelion.traditional_market.MissionCheck.Dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data

public class MissionCheckRequest {
    private String userKey;
    @JsonProperty("mission_id")
    private int missionId;
}
