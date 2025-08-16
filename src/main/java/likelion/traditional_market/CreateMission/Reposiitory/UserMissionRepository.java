package likelion.traditional_market.CreateMission.Reposiitory;

import likelion.traditional_market.CreateMission.Entity.UserMission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserMissionRepository extends JpaRepository<UserMission, Long> {
    List<UserMission> findByUserKey(String userKey);
    Optional<UserMission> findByUserKeyAndMissionId(String userKey, Integer missionId);
}

