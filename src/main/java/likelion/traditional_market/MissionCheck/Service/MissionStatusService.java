package likelion.traditional_market.MissionCheck.Service;

import likelion.traditional_market.CreateMission.Entity.Mission;
import likelion.traditional_market.CreateMission.Entity.Story;
import likelion.traditional_market.CreateMission.Entity.UserMission;
import likelion.traditional_market.CreateMission.Repository.MissionRepository;
import likelion.traditional_market.CreateMission.Repository.UserMissionRepository;
import likelion.traditional_market.MissionCheck.Dto.MissionCheckResponse;
import likelion.traditional_market.UserKeyIssue.Entity.User;
import likelion.traditional_market.UserKeyIssue.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MissionStatusService {
    private final UserRepository userRepository;
    private final likelion.traditional_market.CreateMission.Repository.StoryRepository storyRepository;
    private final UserMissionRepository userMissionRepository;
    private final MissionRepository missionRepository;

    public MissionCheckResponse getSingleMissionStatus(String userKey, int missionId) {

        User user = userRepository.findById(userKey)
                .orElseThrow(() -> new RuntimeException("해당 유저를 찾을 수 없습니다: " + userKey));

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new RuntimeException("미션을 찾을 수 없습니다: " + missionId));

        Story story = storyRepository.findById(user.getStoryId())
                .orElseThrow(() -> new RuntimeException("스토리를 찾을 수 없습니다."));

        if (!Objects.equals(mission.getStoryId(), story.getStoryId())) {
            throw new RuntimeException("해당 스토리의 미션이 아닙니다.");
        }

        Optional<UserMission> um = userMissionRepository.findByUserKeyAndMissionId(userKey, missionId);

        boolean success = um.isPresent(); // 기록 있으면 완료로 간주
        int spent = um.map(UserMission::getSpentAmount).orElse(0);

        return MissionCheckResponse.builder()
                .code(200)
                .message("미션 성공 여부 조회 성공")
                .missionId(missionId)
                .missionDetail(mission.getMissionDetail())
                .spent_amount(spent)
                .is_success(success)
                .build();
    }
}
