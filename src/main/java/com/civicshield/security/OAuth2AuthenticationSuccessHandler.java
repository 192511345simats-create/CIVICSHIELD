package com.civicshield.security;

import com.civicshield.entity.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res,
                                        Authentication auth) throws IOException, ServletException {
        if (auth.getPrincipal() instanceof CustomOAuth2User u) {
            String redirect = u.getRole() == User.Role.ADMIN ? "/admin/overview" : "/citizen/home";
            log.info("OAuth2 success: {} → {}", u.getEmail(), redirect);
            getRedirectStrategy().sendRedirect(req, res, redirect);
        } else {
            super.onAuthenticationSuccess(req, res, auth);
        }
    }
}
