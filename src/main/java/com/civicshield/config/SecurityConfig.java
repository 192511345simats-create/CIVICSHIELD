package com.civicshield.config;

import com.civicshield.security.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    @Autowired(required = false)
    private OAuth2UserService oAuth2UserService;

    @Autowired(required = false)
    private OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;

    // Check if Google OAuth is configured
    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userDetailsService);
        p.setPasswordEncoder(passwordEncoder());
        return p;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration c) throws Exception {
        return c.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(a -> a
                .requestMatchers("/", "/about", "/how-it-works", "/login", "/register",
                        "/forgot-password", "/reset-password",
                        "/css/**", "/js/**", "/images/**", "/favicon.ico",
                        "/api/public/**", "/error", "/access-denied").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/citizen/**").hasRole("CITIZEN")
                .requestMatchers("/worker/**").hasAnyRole("WORKER", "ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(f -> f
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler((req, res, auth) -> {
                    boolean isAdmin = auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                    boolean isWorker = auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_WORKER"));
                    if (isAdmin) {
                        res.sendRedirect("/admin/overview");
                    } else if (isWorker) {
                        res.sendRedirect("/worker/dashboard");
                    } else {
                        res.sendRedirect("/citizen/home");
                    }
                })
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(l -> l
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/login?logged_out=true")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(e -> e.accessDeniedPage("/access-denied"));

        // Only enable Google OAuth2 if credentials are configured
        boolean googleConfigured = googleClientId != null && !googleClientId.isBlank()
                && !googleClientId.equals("YOUR_GOOGLE_CLIENT_ID");
        if (googleConfigured && oAuth2UserService != null && oAuth2SuccessHandler != null) {
            http.oauth2Login(o -> o
                .loginPage("/login")
                .userInfoEndpoint(i -> i.userService(oAuth2UserService))
                .successHandler(oAuth2SuccessHandler)
                .failureUrl("/login?oauth_error=true")
            );
        }

        return http.build();
    }
}
