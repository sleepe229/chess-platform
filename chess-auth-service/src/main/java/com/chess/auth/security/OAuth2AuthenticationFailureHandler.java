package com.chess.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * On OAuth2 login failure (e.g. user denied, provider error), redirect to frontend
 * with error in fragment so the SPA can show a message (per Spring Security docs).
 */
@Slf4j
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        log.warn("OAuth2 login failure: {}", exception.getMessage());
        String errorCode = mapToErrorCode(exception);
        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/auth/callback")
                .fragment("error=" + errorCode)
                .build()
                .toUriString();
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private static String mapToErrorCode(AuthenticationException exception) {
        String msg = exception.getMessage() != null ? exception.getMessage().toLowerCase() : "";
        if (msg.contains("access_denied") || msg.contains("user denied")) return "access_denied";
        if (msg.contains("invalid_token") || msg.contains("token")) return "invalid_token";
        if (msg.contains("redirect")) return "redirect_mismatch";
        return "oauth_failed";
    }
}
