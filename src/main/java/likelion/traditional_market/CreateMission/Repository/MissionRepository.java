package likelion.traditional_market.CreateMission.Repository;

import likelion.traditional_market.CreateMission.Entity.Mission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MissionRepository extends JpaRepository<Mission, Integer> {
    List<Mission> findByStoryId(int storyId);
}
