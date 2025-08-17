package likelion.traditional_market.CreateMission.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_missions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
