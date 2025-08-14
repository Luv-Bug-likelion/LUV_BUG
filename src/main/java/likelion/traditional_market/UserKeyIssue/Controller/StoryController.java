package likelion.traditional_market.UserKeyIssue.Controller;

import jakarta.servlet.http.HttpSession;
import likelion.traditional_market.UserKeyIssue.Dto.CreateStoryRequest;
import likelion.traditional_market.UserKeyIssue.Service.StoryService;
import likelion.traditional_market.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/home")
public class StoryController {

    private final StoryService storyService;

    @PostMapping
    public ResponseEntity<ApiResponse<String>> createStory(@RequestBody CreateStoryRequest request, HttpSession session) {
        String userKey = storyService.createUserAndSave(
                request.getMarket(), request.getBudget(), request.getStoryId()
        );

        session.setAttribute("userKey", userKey);

        return ResponseEntity.ok(ApiResponse.success("예산 입력 및 스토리 생성 완료", userKey));
    }
}
