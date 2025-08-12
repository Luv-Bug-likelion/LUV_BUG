package likelion.traditional_market.CreateMission.Dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class MissionDetailDto {

    private int missionId;

    @JsonAlias({"missionDetail", "ingredient"})
    private String missionDetail;

    private int expectedPrice;

    @JsonProperty("is_success")
    private boolean isSuccess;
}
