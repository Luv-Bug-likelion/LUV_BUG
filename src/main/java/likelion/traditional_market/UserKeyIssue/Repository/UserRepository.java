package likelion.traditional_market.UserKeyIssue.Repository;

import likelion.traditional_market.UserKeyIssue.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {

}
