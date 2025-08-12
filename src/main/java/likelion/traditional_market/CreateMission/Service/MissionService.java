package likelion.traditional_market.CreateMission.Service;

import jakarta.transaction.Transactional;
import likelion.traditional_market.CreateMission.Dto.MissionDetailDto;
import likelion.traditional_market.CreateMission.Dto.MissionStatusResponse;
import likelion.traditional_market.CreateMission.Entity.Mission;
import likelion.traditional_market.CreateMission.Entity.Story;
import likelion.traditional_market.CreateMission.Entity.UserMission;
import likelion.traditional_market.CreateMission.Reposiitory.MissionRepository;
import likelion.traditional_market.CreateMission.Reposiitory.StoryRepository;
import likelion.traditional_market.CreateMission.Reposiitory.UserMissionRepository;
import likelion.traditional_market.UserKeyIssue.Entity.User;
import likelion.traditional_market.UserKeyIssue.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MissionService {
    private final UserRepository userRepository;
    private final ChatGptService chatGptService;
    private final MissionRepository missionRepository;
    private final StoryRepository storyRepository;
    private final PriceService priceService;
    private final UserMissionRepository userMissionRepository;

    @Transactional
    public MissionStatusResponse generateMissionForUser(String userKey) {
        User user = userRepository.findById(userKey)
                .orElseThrow(() -> new RuntimeException("User not found with key: " + userKey));

        // ChatGPT API를 사용해 미션, 예상 가격 생성
        MissionStatusResponse chatGptResponse = chatGptService.generateMission(user.getStoryId(), user.getBudget());

        // 가격 파일로 가격을 검증, 없는 품목은 ChatGPT에서 가격 생성
        List<MissionDetailDto> missionDetails = chatGptResponse.getMissionList().stream()
                .map(item -> {
                    int priceFromCsv = priceService.getPrice(item.getMissionDetail(), user.getMarket());
                    if (priceFromCsv > 0) {
                        item.setExpectedPrice(priceFromCsv);
                    }
                    return item;
                })
                .collect(Collectors.toList());

        // 가격 제약 검증 (수정 필요)
        missionDetails.sort(Comparator.comparingInt(MissionDetailDto::getExpectedPrice).reversed());
        int top3PriceSum = missionDetails.stream()
                .limit(3)
                .mapToInt(MissionDetailDto::getExpectedPrice)
                .sum();

        if (top3PriceSum > user.getBudget()) {
            throw new IllegalArgumentException("예상 가격이 예산을 초과합니다.");
        }

        // 생성된 미션 DB에 저장
        Story story = storyRepository.findById(user.getStoryId())
                .orElseThrow(() -> new IllegalArgumentException("Story not found with key: " + user.getStoryId()));
        story.setTitle(chatGptResponse.getMissionTitle());
        storyRepository.save(story);

        List<Mission> missions = missionDetails.stream()
                .map(dto -> {
                    Mission mission = new Mission();
                    mission.setStoryId(user.getStoryId());
                    mission.setMissionDetail(dto.getMissionDetail());
                    mission.setExpectedPrice(dto.getExpectedPrice());
                    return mission;
                })
                .collect(Collectors.toList());
        missionRepository.saveAll(missions);

        List<UserMission> userMissions = missions.stream()
                .map(mission -> {
                    UserMission userMission = new UserMission();
                    userMission.setUserKey(userKey);
                    userMission.setMissionId(mission.getMissionId());
                    userMission.setSuccess(false);
                    return userMission;
                })
                .collect(Collectors.toList());
        userMissionRepository.saveAll(userMissions);

        // 응답 DTO 생성
        return MissionStatusResponse.builder()
                .missionTitle(story.getTitle())
                .missionList(missionDetails)
                .totalSpent(0)
                .missionCompleteCount(0)
                .build();
    }
}
