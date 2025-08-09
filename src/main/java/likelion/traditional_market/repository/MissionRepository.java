package likelion.traditional_market.repository;

import likelion.traditional_market.entity.MissionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MissionRepository extends JpaRepository<MissionItem,Long> {
    List<MissionItem> findByMission_User_UserKey(String userkey);
}
