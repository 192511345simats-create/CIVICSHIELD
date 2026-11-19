package com.civicshield.security;

import com.civicshield.entity.User;
import com.civicshield.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String email     = oAuth2User.getAttribute("email");
        String name      = oAuth2User.getAttribute("name");
        String googleId  = oAuth2User.getAttribute("sub");

        if (email == null) throw new OAuth2AuthenticationException("Email not provided by Google");

        User user = userRepository.findByEmail(email).map(existing -> {
            if (existing.getProvider() == User.AuthProvider.LOCAL) {
                existing.setProviderId(googleId);
                existing.setProvider(User.AuthProvider.GOOGLE);
            }
            return userRepository.save(existing);
        }).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFullName(name != null ? name : email);
            newUser.setProvider(User.AuthProvider.GOOGLE);
            newUser.setProviderId(googleId);
            newUser.setRole(User.Role.CITIZEN);
            newUser.setEnabled(true);
            log.info("New user via Google OAuth: {}", email);
            return userRepository.save(newUser);
        });

        return new CustomOAuth2User(user, oAuth2User.getAttributes());
    }
}
