package likelion.traditional_market.CreateMission.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "stories")
@Getter
@Setter
public class Story {

    @Id
    private int storyId;
    private String title;
    private String description;
}
