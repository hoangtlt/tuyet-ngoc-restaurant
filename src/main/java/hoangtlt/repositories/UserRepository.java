package hoangtlt.repositories;

import hoangtlt.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    long countByStatus(hoangtlt.entities.User.UserStatus status);
    java.util.List<hoangtlt.entities.User> findByStatus(hoangtlt.entities.User.UserStatus status);
}
