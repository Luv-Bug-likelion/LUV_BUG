package likelion.traditional_market.CreateMission.Dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"code", "message", "storyId", "missionTitle", "missionList", "totalSpent", "missionCompleteCount"})
public class MissionStatusResponse {
    @JsonProperty("code")
    private int code;
    @JsonProperty("message")
    private String message;
    private int storyId;
    private String missionTitle;
    private List<MissionDetailDto> missionList;
    private int totalSpent;
    private int missionCompleteCount;
}
