package pl.nclean.crm.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/error").permitAll()
                .anyRequest().hasRole("OWNER"))
            .formLogin(Customizer.withDefaults())
            .logout(logout -> logout.invalidateHttpSession(true).deleteCookies("NCLEAN_CRM_SESSION"))
            .csrf(Customizer.withDefaults())
            .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    UserDetailsService users(@Value("${crm.admin.username}") String username,
                             @Value("${crm.admin.password-hash}") String hash) {
        if (username.isBlank() || !hash.matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}")) {
            throw new IllegalArgumentException("Set a CRM owner username and a valid BCrypt password hash");
        }
        return new InMemoryUserDetailsManager(User.withUsername(username)
            .password(hash).roles("OWNER").build());
    }
}
