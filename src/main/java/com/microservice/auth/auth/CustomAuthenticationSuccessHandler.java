package com.microservice.auth.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@NoArgsConstructor
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
    @Value("${application.security.jwt.access-token-header}")
    private String accessTokenHeader;
    @Value("${application.security.jwt.refresh-token-header}")
    private String refreshTokenHeader;
    @Autowired
    private AuthenticationService service;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {
        super.onAuthenticationSuccess(request, response, authentication);

        SecurityContextHolder.getContext().getAuthentication();
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2AuthenticatedPrincipal principal = oauthToken.getPrincipal();
        String email = principal.getAttribute("email");


        AuthenticationResponse authResponse = service.authenticateWithOauth(email);


        response.addHeader(accessTokenHeader, "Bearer " + authResponse.getAccessToken());
        response.addHeader(refreshTokenHeader, authResponse.getRefreshToken());
    }
}
