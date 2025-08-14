package likelion.traditional_market.CreateMission.Repository;

import likelion.traditional_market.CreateMission.Entity.UserMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserMissionRepository extends JpaRepository<UserMission, Long> {
    List<UserMission> findByUserKey(String userKey);
}
