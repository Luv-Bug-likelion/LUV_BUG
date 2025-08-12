package likelion.traditional_market.CreateMission.Reposiitory;

import likelion.traditional_market.CreateMission.Entity.Story;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoryRepository extends JpaRepository<Story, Integer> {
}
