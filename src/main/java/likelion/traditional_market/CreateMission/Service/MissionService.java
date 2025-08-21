package likelion.traditional_market.CreateMission.Service;

import likelion.traditional_market.CreateMission.Dto.MissionDetailDto;
import likelion.traditional_market.CreateMission.Dto.MissionStatusResponse;
import likelion.traditional_market.CreateMission.Entity.Mission;
import likelion.traditional_market.CreateMission.Entity.Story;
import likelion.traditional_market.CreateMission.Entity.UserMission;
import likelion.traditional_market.CreateMission.Repository.MissionRepository;
import likelion.traditional_market.CreateMission.Repository.StoryRepository;
import likelion.traditional_market.CreateMission.Repository.UserMissionRepository;
import likelion.traditional_market.KakaoMap.dto.StoreInfoDto;
import likelion.traditional_market.KakaoMap.service.LocationService;
import likelion.traditional_market.UserKeyIssue.Entity.User;
import likelion.traditional_market.UserKeyIssue.Repository.UserRepository;
import likelion.traditional_market.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

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
    private final StoryRepository storyRepository;
    private final MissionRepository missionRepository;
    private final UserMissionRepository userMissionRepository;
    private final ChatGptService chatGptService;
    private final PriceService priceService;
    private final LocationService locationService;

    @Transactional
    public ApiResponse<MissionStatusResponse> generateMissionForUser(String userKey) {
        User user = userRepository.findById(userKey)
                .orElseThrow(() -> new IllegalArgumentException("User not found with key: " + userKey));

        List<UserMission> existingMissions = userMissionRepository.findByUserKey(userKey);
        if (!existingMissions.isEmpty()) {
            return buildExistingMissionResponse(user, existingMissions);
        }

        Story story = storyRepository.findById(user.getStoryId())
                .orElseThrow(() -> new IllegalArgumentException("Story not found"));

        ApiResponse<MissionStatusResponse> chatGptApiResponse = chatGptService.generateMission(user.getStoryId(), user.getBudget());
        MissionStatusResponse chatGptResponse = chatGptApiResponse.getData();
        String chatGptMissionTitle = chatGptResponse.getMissionTitle();

        if (chatGptMissionTitle == null || chatGptMissionTitle.isEmpty()) {
            throw new IllegalArgumentException("ChatGPT 응답에 missionTitle이 포함되어 있지 않습니다.");
        }

        String newMissionTitle;
        if (story.getTitle().equals("엄마의 심부름")) {
            newMissionTitle = "엄마가 " + chatGptMissionTitle + " 재료를 사오라고 했어요.";
        } else if (story.getTitle().equals("임꺽정의 여정")) {
            newMissionTitle = "임꺽정의 부천 시장 탐방: 주린 배를 채울 " + chatGptMissionTitle + " 재료를 찾아라!";
        } else {
            newMissionTitle = chatGptMissionTitle;
        }

        user.setMissionTitle(newMissionTitle);
        userRepository.save(user);

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

        List<Mission> missionsToSave = finalMissionDetails.stream()
                .map(dto -> {
                    Mission mission = new Mission();
                    mission.setStoryId(user.getStoryId());
                    mission.setMissionDetail(dto.getMissionDetail());
                    mission.setExpectedPrice(dto.getExpectedPrice());
                    return mission;
                })
                .collect(Collectors.toList());

        List<Mission> savedMissions = missionRepository.saveAll(missionsToSave);

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

        List<MissionDetailDto> responseMissionList = savedMissions.stream()
                .map(mission -> MissionDetailDto.builder()
                        .missionId(mission.getMissionId())
                        .missionDetail(mission.getMissionDetail())
                        .expectedPrice(mission.getExpectedPrice())
                        .isSuccess(false)
                        .build())
                .collect(Collectors.toList());

        MissionStatusResponse response = MissionStatusResponse.builder()
                .storyId(user.getStoryId())
                .missionTitle(newMissionTitle)
                .missionList(responseMissionList)
                .totalSpent(0)
                .missionCompleteCount(0)
                .build();

        return ApiResponse.success("미션 목록 조회 성공", response);
    }

    private ApiResponse<MissionStatusResponse> buildExistingMissionResponse(User user, List<UserMission> userMissions) {
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

        MissionStatusResponse response = MissionStatusResponse.builder()
                .storyId(user.getStoryId())
                .missionTitle(user.getMissionTitle())
                .missionList(missionDetails)
                .totalSpent(user.getTotalSpent())
                .missionCompleteCount(user.getMissionCompleteCount())
                .build();

        return ApiResponse.success("기존 미션 목록 조회 성공", response);
    }

    private String extractKeyword(String missionDetail) {
        Pattern pattern = Pattern.compile("(.+?)( [0-9]+.*)?(을|를)? 구매한다");
        Matcher matcher = pattern.matcher(missionDetail);

        if (matcher.find()) {
            String fullItem = matcher.group(1).trim();
            String[] parts = fullItem.split(" ");

            if (parts.length > 1 && (parts[0].equals("돼지고기") || parts[0].equals("소고기"))) {
                return fullItem;
            }

            return parts[0];
        }

        return "";
    }

    // getAllCategorizedStores 메서드를 삭제하고, 모든 식자재 상점을 반환하는 메서드로 대체
    @Transactional(readOnly = true)
    public Mono<ApiResponse<List<StoreInfoDto>>> getAllFoodStores(String userKey) {
        User user = userRepository.findById(userKey)
                .orElseThrow(() -> new IllegalArgumentException("User not found with key: " + userKey));

        String marketName = (user.getMarket() != null && !user.getMarket().isBlank())
                ? user.getMarket()
                : "역곡남부시장";

        return locationService.getAllFoodStoresInRadiusAsync(marketName, 300)
                .map(allStores -> ApiResponse.success("상점 목록 조회 성공", allStores));
    }
}