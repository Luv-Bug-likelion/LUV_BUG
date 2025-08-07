package likelion.traditional_market.UserKeyIssue.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateStoryResponse {

    private int code;
    private String message;
    private String storyId;
}
