package likelion.traditional_market.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;

@Entity

public class User {
    @Id
    @GeneratedValue
    private Long id;
    @Getter
    private String userKey;
}
