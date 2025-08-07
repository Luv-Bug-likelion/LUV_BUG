package likelion.traditional_market.UserKeyIssue.Controller;

import jakarta.servlet.http.HttpSession;
import likelion.traditional_market.UserKeyIssue.Dto.CreateStoryRequest;
import likelion.traditional_market.UserKeyIssue.Dto.CreateStoryResponse;
import likelion.traditional_market.UserKeyIssue.Service.StoryService;
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
    public ResponseEntity<CreateStoryResponse> createStory(@RequestBody CreateStoryRequest request, HttpSession session) {
        String userKey = storyService.createUserAndSave(
                request.getMarket(), request.getBudget(), request.getStoryId()
        );

        session.setAttribute("userKey", userKey);
        return ResponseEntity.ok(new CreateStoryResponse(200, "예산 입력 및 스토리 생성 완료", userKey));
    }
}
