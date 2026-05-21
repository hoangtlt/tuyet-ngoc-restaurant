package hoangtlt.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import hoangtlt.services.UsersService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
        private final CustomSuccessHandler successHandler;
        private final UsersService usersService;

        public SecurityConfig(CustomSuccessHandler successHandler, UsersService usersService) {
                this.successHandler = successHandler;
                this.usersService = usersService;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
                return httpSecurity
                                .authorizeHttpRequests(authorize -> authorize
                                                .requestMatchers("/assets/**", "/login", "/register", "/css/**",
                                                                "/js/**", "/images/**")
                                                .permitAll()
                                                .requestMatchers("/admin/users/pending", "/admin/users/approve/**",
                                                                "/admin/users/reject/**", "/api/notifications/**")
                                                .hasAnyRole("ADMIN", "MANAGER")
                                                .requestMatchers("/admin/**").hasRole("ADMIN")
                                                .requestMatchers("/manager/**").hasAnyRole("ADMIN", "MANAGER")
                                                .anyRequest().authenticated())
                                .formLogin(form -> form.loginPage("/login")
                                                .usernameParameter("username")
                                                .successHandler(successHandler)
                                                .failureUrl("/login?error=true"))
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login")
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID", "remember-me-cookie")
                                                .clearAuthentication(true)
                                                .permitAll())
                                .exceptionHandling(exception -> exception
                                                .accessDeniedPage("/access-denied"))
                                .rememberMe(remember -> remember
                                                .key("uniqueAndSecret")
                                                .tokenValiditySeconds(86400 * 30) // 30 ngày
                                                .userDetailsService(usersService)
                                                .rememberMeParameter("remember-me")
                                                .rememberMeCookieName("remember-me-cookie") // Đặt tên cookie rõ ràng
                                                .useSecureCookie(false) // Rất quan trọng cho Safari khi dùng HTTP/IP
                                )
                                .build();
        }

}
