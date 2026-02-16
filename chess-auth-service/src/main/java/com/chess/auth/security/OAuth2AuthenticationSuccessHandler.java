package com.chess.auth.security;

import com.chess.auth.constants.AuthProvider;
import com.chess.auth.domain.User;
import com.chess.auth.service.AuthService;
import com.chess.auth.service.RefreshTokenService;
import com.chess.common.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * After successful OAuth2 login: find or create user, issue JWT + refresh token,
 * redirect to frontend with tokens in URL fragment (so they are not sent to server logs).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String registrationId = getRegistrationId(authentication);
        String provider = mapRegistrationIdToProvider(registrationId);
        String providerUserId = getProviderUserId(oauth2User, registrationId);
        String email = getEmail(oauth2User);

        if (providerUserId == null || email == null || email.isBlank()) {
            log.warn("OAuth2 user missing sub or email: attributes={}", oauth2User.getAttributes());
            redirectToFrontendError(request, response, "missing_attributes");
            return;
        }

        User user = authService.findOrCreateFromOAuth2(provider, providerUserId, email);
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRoles());
        String refreshTokenValue = jwtTokenProvider.generateRefreshToken(user.getId());
        refreshTokenService.createRefreshToken(user, refreshTokenValue);
        long expiresInSeconds = jwtTokenProvider.getAccessTokenValidityMs() / 1000;

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/auth/callback")
                .fragment("access_token=" + encode(accessToken)
                        + "&refresh_token=" + encode(refreshTokenValue)
                        + "&expires_in=" + expiresInSeconds)
                .build()
                .toUriString();

        log.info("OAuth2 login success: userId={}, provider={}, redirecting to frontend", user.getId(), provider);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    /** OpenID Connect uses "sub", GitHub uses "id". */
    private String getProviderUserId(OAuth2User oauth2User, String registrationId) {
        String sub = oauth2User.getAttribute("sub");
        if (sub != null && !sub.isBlank()) return sub;
        Object id = oauth2User.getAttribute("id");
        if (id != null) return id.toString();
        return null;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String getRegistrationId(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken token) {
            return token.getAuthorizedClientRegistrationId();
        }
        return "google";
    }

    private String mapRegistrationIdToProvider(String registrationId) {
        return switch (registrationId != null ? registrationId.toLowerCase() : "") {
            case "google" -> AuthProvider.GOOGLE;
            case "github" -> AuthProvider.GITHUB;
            default -> registrationId != null ? registrationId.toUpperCase() : AuthProvider.GOOGLE;
        };
    }

    private String getEmail(OAuth2User oauth2User) {
        String email = oauth2User.getAttribute("email");
        if (email != null && !email.isBlank()) return email;
        Map<String, Object> attrs = oauth2User.getAttributes();
        if (attrs != null && attrs.containsKey("email")) {
            Object e = attrs.get("email");
            return e != null ? e.toString() : null;
        }
        return null;
    }

    private void redirectToFrontendError(HttpServletRequest request, HttpServletResponse response, String error) throws IOException {
        String url = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/auth/callback")
                .fragment("error=" + encode(error))
                .build()
                .toUriString();
        getRedirectStrategy().sendRedirect(request, response, url);
    }
}
