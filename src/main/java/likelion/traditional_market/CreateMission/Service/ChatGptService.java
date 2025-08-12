package likelion.traditional_market.CreateMission.Service;

import likelion.traditional_market.CreateMission.Dto.MissionDetailDto;
import likelion.traditional_market.CreateMission.Dto.MissionStatusResponse;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class ChatGptService {

    // ChatGpt API 호출 후 미션, 가격 생성
    // 하드코딩을 통해 출력 예시 반영
    public MissionStatusResponse generateMission(int storyId, int budget) {
        List<MissionDetailDto> missionList = Arrays.asList(
                MissionDetailDto.builder().missionId(1).missionDetail("돼지고기 200g").expectedPrice(10000).isSuccess(false).build(),
                MissionDetailDto.builder().missionId(2).missionDetail("신김치 1/4포기").expectedPrice(4500).isSuccess(false).build(),
                MissionDetailDto.builder().missionId(3).missionDetail("두부 한 모").expectedPrice(2000).isSuccess(false).build(),
                MissionDetailDto.builder().missionId(4).missionDetail("대파 한 단").expectedPrice(3500).isSuccess(false).build(),
                MissionDetailDto.builder().missionId(5).missionDetail("양파 1개").expectedPrice(1500).isSuccess(false).build()
        );

        return MissionStatusResponse.builder()
                .missionTitle("김치찌개 재료를 사와라!")
                .missionList(missionList)
                .totalSpent(0)
                .missionCompleteCount(0)
                .build();
    }

    public String classifyKeyword(String keyword) {
        if ("깻잎".equals(keyword) || "상추".equals(keyword)) {
            return "농산물";
        } else if ("고등어".equals(keyword)) {
            return "수산물";
        } else if ("바나나".equals(keyword)) {
            return "과일";
        } else {
            return "식품";
        }
    }
}