package hoangtlt.services;

import hoangtlt.entities.User;
import hoangtlt.repositories.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UsersService implements UserDetailsService {
    private final UserRepository userRepository;
    private final hoangtlt.repositories.RoleRepository roleRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public UsersService(UserRepository userRepository, 
                        hoangtlt.repositories.RoleRepository roleRepository,
                        org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public void register(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(User.UserStatus.PENDING); // Đợi duyệt
        // Gán Role STAFF mặc định
        hoangtlt.entities.Role staffRole = roleRepository.findByName("STAFF")
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy Role STAFF hệ thống"));
        user.setRole(staffRole);
        userRepository.save(user);
    }

    public long getPendingCount() {
        return userRepository.countByStatus(hoangtlt.entities.User.UserStatus.PENDING);
    }

    public java.util.List<User> getPendingUsers() {
        return userRepository.findByStatus(hoangtlt.entities.User.UserStatus.PENDING);
    }

    public void approveUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);
    }

    public void updateProfile(String username, String fullName, String email, String phone, String newPassword) {
        User user = userRepository.findByUsername(username).orElseThrow();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(newPassword));
        }
        
        userRepository.save(user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> userOptional = userRepository.findByUsername(username);
        
        if (userOptional.isEmpty()) {
            throw new UsernameNotFoundException("Không tìm thấy người dùng: " + username);
        }

        User user = userOptional.get();
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            String msg = user.getStatus() == User.UserStatus.PENDING ? "Tài khoản đang chờ phê duyệt" : "Tài khoản đã bị khóa";
            throw new UsernameNotFoundException(msg + ": " + username);
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().getName()) // Spring prefixes ROLE_ automatically if we use .roles()
                .build();
    }
}
