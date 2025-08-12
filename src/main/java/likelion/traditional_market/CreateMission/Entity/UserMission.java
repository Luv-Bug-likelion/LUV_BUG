package likelion.traditional_market.CreateMission.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_missions")
@Getter
@Setter
public class UserMission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String userKey;
    private int missionId;
    private boolean isSuccess = false;
    private String receiptImgUrl;
    private int spentAmount = 0;
}
