package likelion.traditional_market.CreateMission.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "missions")
@Getter
@Setter
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int missionId;
    private int storyId;
    private String missionDetail;
    private int expectedPrice;
}
