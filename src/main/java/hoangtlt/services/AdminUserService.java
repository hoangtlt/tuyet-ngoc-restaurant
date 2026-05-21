package hoangtlt.services;

import hoangtlt.entities.User;
import hoangtlt.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class AdminUserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User saveUser(User user) {
        // Encode password if it's a new user or password changed
        if (user.getId() == null || !user.getPassword().startsWith("$2a$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public List<User> getPendingUsers() {
        return userRepository.findByStatus(User.UserStatus.PENDING);
    }

    public void approveUser(Long id) {
        User user = userRepository.findById(id).orElseThrow();
        user.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);
    }

    public void lockUser(Long id) {
        User user = userRepository.findById(id).orElseThrow();
        user.setStatus(User.UserStatus.LOCKED);
        userRepository.save(user);
    }
}
