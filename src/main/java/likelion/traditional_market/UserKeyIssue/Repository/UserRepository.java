package likelion.traditional_market.UserKeyIssue.Repository;

import likelion.traditional_market.UserKeyIssue.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
}
