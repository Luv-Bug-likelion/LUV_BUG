package likelion.traditional_market.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class Mission {
    @Id
    @GeneratedValue
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) // User와 N:1 관계
    @JoinColumn(name = "user_id", nullable = false) // FK 컬럼명 지정
    private User user;
}
