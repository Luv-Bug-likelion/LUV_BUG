package likelion.traditional_market.UserKeyIssue.Service;

import jakarta.transaction.Transactional;
import likelion.traditional_market.UserKeyIssue.Entity.User;
import likelion.traditional_market.UserKeyIssue.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoryService {

    private final UserRepository userRepository;

    @Transactional
    public String createUserAndSave(String market, int budget, int storyId) {
        // 고유한 userKey 생성
        String userKey = UUID.randomUUID().toString();

        // User 엔티티 생성
        User user = new User();
        user.setUserKey(userKey);
        user.setMarket(market);
        user.setBudget(budget);
        user.setStoryId(storyId);
        user.setTotalSpent(0);
        user.setMissionCompleteCount(0);

        userRepository.save(user);

        return userKey;
    }
}
