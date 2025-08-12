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
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MissionService {

    private final UserRepository userRepository;
    private final StoryRepository storyRepository;
    private final MissionRepository missionRepository;
    private final UserMissionRepository userMissionRepository;
    private final ChatGptService chatGptService;
    private final PriceService priceService;
    private final LocationService locationService;

    @Transactional
    public String createUserAndSave(String market, int budget, int storyId) {
        String userKey = UUID.randomUUID().toString();
        User user = new User();
        user.setUserKey(userKey);
        user.setMarket(market);
        user.setBudget(budget);
        user.setStoryId(storyId);
        userRepository.save(user);
        return userKey;
    }

    @Transactional
    public MissionStatusResponse generateMissionForUser(String userKey) {
        User user = userRepository.findById(userKey)
                .orElseThrow(() -> new IllegalArgumentException("User not found with key: " + userKey));

        List<UserMission> existingMissions = userMissionRepository.findByUserKey(userKey);
        if (!existingMissions.isEmpty()) {
            return buildExistingMissionResponse(user, existingMissions);
        }

        MissionStatusResponse chatGptResponse = chatGptService.generateMission(user.getStoryId(), user.getBudget());
        String missionTitle = chatGptResponse.getMissionTitle();
        if (missionTitle == null || missionTitle.isEmpty()) {
            throw new IllegalArgumentException("ChatGPT 응답에 missionTitle이 포함되어 있지 않습니다.");
        }

        List<MissionDetailDto> processedMissionDetails = chatGptResponse.getMissionList().stream()
                .map(dto -> {
                    String detail = dto.getMissionDetail();
                    if (detail != null) {
                        dto.setMissionDetail(detail.replaceAll("시장 [^ ]+에서 ", ""));
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        List<MissionDetailDto> finalMissionDetails = processedMissionDetails.stream()
                .map(dto -> {
                    int priceFromCsv = priceService.getPrice(extractKeyword(dto.getMissionDetail()), user.getMarket());
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

        Story story = storyRepository.findById(user.getStoryId())
                .orElseThrow(() -> new IllegalArgumentException("Story not found"));
        story.setTitle(missionTitle);
        story.setDescription(missionTitle);
        storyRepository.save(story);

        List<Mission> missionsToSave = finalMissionDetails.stream()
                .map(dto -> {
                    Mission mission = new Mission();
                    mission.setStoryId(user.getStoryId());
                    mission.setMissionDetail(dto.getMissionDetail());
                    mission.setExpectedPrice(dto.getExpectedPrice());
                    return mission;
                })
                .collect(Collectors.toList());

        List<Mission> savedMissions = missionRepository.saveAll(missionsToSave); // DB에 저장하고, ID가 부여된 엔티티 목록을 받음

        List<UserMission> userMissions = savedMissions.stream()
                .map(mission -> {
                    UserMission userMission = new UserMission();
                    userMission.setUserKey(userKey);
                    userMission.setMissionId(mission.getMissionId());
                    userMission.setSuccess(false);
                    return userMission;
                })
                .collect(Collectors.toList());
        userMissionRepository.saveAll(userMissions);

        // DB에서 생성된 ID를 포함한 DTO 리스트를 새로 만듭니다.
        List<MissionDetailDto> responseMissionList = savedMissions.stream()
                .map(mission -> MissionDetailDto.builder()
                        .missionId(mission.getMissionId())
                        .missionDetail(mission.getMissionDetail())
                        .expectedPrice(mission.getExpectedPrice())
                        .isSuccess(false)
                        .build())
                .collect(Collectors.toList());

        return MissionStatusResponse.builder()
                .code(200)
                .message("미션 목록 조회 성공")
                .storyId(user.getStoryId()) // storyId 추가
                .missionTitle(story.getTitle())
                .missionList(responseMissionList) // ID가 포함된 리스트로 교체
                .totalSpent(0)
                .missionCompleteCount(0)
                .build();
    }
    private MissionStatusResponse buildExistingMissionResponse(User user, List<UserMission> userMissions) {
        Story story = storyRepository.findById(user.getStoryId())
                .orElseThrow(() -> new IllegalArgumentException("Story not found"));

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
                                .map(UserMission::isSuccess)
                                .orElse(false))
                        .build())
                .collect(Collectors.toList());

        return MissionStatusResponse.builder()
                .code(200)
                .message("기존 미션 목록 조회 성공")
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
                .distinct()
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