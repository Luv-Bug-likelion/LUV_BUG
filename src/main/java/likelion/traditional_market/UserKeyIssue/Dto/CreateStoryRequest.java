package likelion.traditional_market.UserKeyIssue.Dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateStoryRequest {

    private String market; //추후 확장을 위해 시장도 db에 담고 id로 관리하도록 수정 필요
    private int budget;
    private int storyId;
}
