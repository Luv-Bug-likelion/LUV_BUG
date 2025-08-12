package likelion.traditional_market.CreateMission.Reposiitory;

import likelion.traditional_market.CreateMission.Entity.Mission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MissionRepository extends JpaRepository<Mission, Integer> {
    List<Mission> findByStoryId(int storyId);
}
