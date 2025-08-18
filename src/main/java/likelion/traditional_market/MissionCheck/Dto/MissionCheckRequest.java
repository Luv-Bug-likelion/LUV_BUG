package likelion.traditional_market.MissionCheck.Dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MissionCheckRequest {
    private String userKey;
    @JsonProperty("mission_id")
    private int missionId;
}
