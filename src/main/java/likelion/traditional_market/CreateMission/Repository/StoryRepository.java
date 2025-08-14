package likelion.traditional_market.CreateMission.Repository;

import likelion.traditional_market.CreateMission.Entity.Story;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoryRepository extends JpaRepository<Story, Integer> {
}
