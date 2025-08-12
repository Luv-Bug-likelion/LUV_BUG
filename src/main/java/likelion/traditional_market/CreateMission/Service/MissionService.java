package likelion.traditional_market.CreateMission.Service;

import likelion.traditional_market.CreateMission.Dto.MissionDetailDto;
import likelion.traditional_market.CreateMission.Dto.MissionStatusResponse;
import likelion.traditional_market.CreateMission.Entity.Mission;
import likelion.traditional_market.CreateMission.Entity.Story;
import likelion.traditional_market.CreateMission.Entity.UserMission;
import likelion.traditional_market.CreateMission.Reposiitory.MissionRepository;
import likelion.traditional_market.CreateMission.Reposiitory.StoryRepository;
import likelion.traditional_market.CreateMission.Reposiitory.UserMissionRepository;
import likelion.traditional_market.KakaoMap.dto.StoreInfoDto;
import likelion.traditional_market.KakaoMap.service.LocationService;
import likelion.traditional_market.UserKeyIssue.Entity.User;
import likelion.traditional_market.UserKeyIssue.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private final LocationService locationService;

    @Transactional
    public MissionStatusResponse generateMissionForUser(String userKey) {
        User user = userRepository.findById(userKey)
                .orElseThrow(() -> new IllegalArgumentException("User not found with key: " + userKey));

        // 이미 미션이 생성되었는지 확인
        List<UserMission> existingMissions = userMissionRepository.findByUserKey(userKey);
        if (!existingMissions.isEmpty()) {
            return buildExistingMissionResponse(user, existingMissions);
        }

        // ChatGPT API 호출을 통한 미션 생성
        MissionStatusResponse chatGptResponse = chatGptService.generateMission(user.getStoryId(), user.getBudget());

        // 가격 제약 조건 검증 및 재요청 로직
        List<MissionDetailDto> generatedMissions = chatGptResponse.getMissionList();

        // 가격 파일로 가격을 검증하고, 없는 품목은 ChatGPT 가격 사용
        List<MissionDetailDto> finalMissionDetails = generatedMissions.stream()
                .map(dto -> {
                    int priceFromCsv = priceService.getPrice(dto.getMissionDetail(), user.getMarket());
                    if (priceFromCsv > 0) {
                        dto.setExpectedPrice(priceFromCsv);
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        finalMissionDetails.sort(Comparator.comparingInt(MissionDetailDto::getExpectedPrice).reversed());
        int top3PriceSum = finalMissionDetails.stream()
                .limit(3)
                .mapToInt(MissionDetailDto::getExpectedPrice)
                .sum();

        if (top3PriceSum > user.getBudget()) {
            throw new IllegalArgumentException("예상 가격이 예산을 초과하여 미션 생성에 실패했습니다.");
        }

        // 3. 생성된 미션을 DB에 저장
        Story story = storyRepository.findById(user.getStoryId())
                .orElseThrow(() -> new IllegalArgumentException("Story not found"));
        story.setTitle(chatGptResponse.getMissionTitle());
        story.setDescription(chatGptResponse.getMissionTitle()); // 예시로 description도 title과 동일하게 설정
        storyRepository.save(story);

        List<Mission> missions = finalMissionDetails.stream()
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
                .missionList(finalMissionDetails)
                .totalSpent(0)
                .missionCompleteCount(0)
                .build();
    }

    // 기존 미션이 있을 경우 응답 DTO
    private MissionStatusResponse buildExistingMissionResponse(User user, List<UserMission> userMissions) {
        Story story = storyRepository.findById(user.getStoryId())
                .orElseThrow(() -> new IllegalArgumentException("Story not found with key: " + user.getStoryId()));

        List<Mission> missions = missionRepository.findAllById(
                userMissions.stream().map(UserMission::getMissionId).collect(Collectors.toList())
        );

        List<MissionDetailDto> missionDetails = missions.stream()
                .map(m -> MissionDetailDto.builder()
                        .missionId(m.getMissionId())
                        .missionDetail(m.getMissionDetail())
                        .expectedPrice(m.getExpectedPrice())
                        .isSuccess(userMissions.stream()
                                .filter(um -> um.getMissionId() == m.getMissionId())
                                .findFirst()
                                .map(um -> um.isSuccess() ? true : false)
                                .orElse(false))
                        .build())
                .collect(Collectors.toList());

        return MissionStatusResponse.builder()
                .missionTitle(story.getTitle())
                .missionList(missionDetails)
                .totalSpent(user.getTotalSpent())
                .missionCompleteCount(user.getMissionCompleteCount())
                .build();
    }

    @Transactional(readOnly = true)
    public Map<String, List<StoreInfoDto>> getStoresForMissions(String userKey) {
        User user = userRepository.findById(userKey)
                .orElseThrow(() -> new IllegalArgumentException("User not found with key: " + userKey));

        List<Mission> missions = missionRepository.findByStoryId(user.getStoryId());

        List<String> keywords = missions.stream()
                .map(mission -> extractKeyword(mission.getMissionDetail()))
                .distinct() // 중복된 키워드를 제거
                .collect(Collectors.toList());

        return locationService.searchStores(keywords, user.getMarket());
    }

    private String extractKeyword(String missionDetail) {
        Pattern pattern = Pattern.compile("^([가-힣a-zA-Z0-9]+)");
        Matcher matcher = pattern.matcher(missionDetail);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }
}