package likelion.traditional_market.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class MissionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="mission_id")
    private Mission mission;
    @Column(name="item_name",nullable=false)
    private String itemName;
}
