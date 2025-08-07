package likelion.traditional_market.UserKeyIssue.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    private String userKey;
    private String market;
    private int budget;
    private int storyId;
    private int totalSpent;
    private int missionCompleteCount;
}
