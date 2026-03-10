package com.example.mailsearch.controller;

import com.example.mailsearch.dto.AuthUserResponse;
import com.example.mailsearch.dto.ChangePasswordRequest;
import com.example.mailsearch.dto.LoginRequest;
import com.example.mailsearch.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final AuthService authService;

    public AuthController(AuthenticationManager authenticationManager, AuthService authService) {
        this.authenticationManager = authenticationManager;
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthUserResponse login(@RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        if (request == null || request.getUsername() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("아이디와 비밀번호를 입력하세요.");
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            throw new IllegalArgumentException("로그인 정보가 올바르지 않습니다.");
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        HttpSession session = httpServletRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        return authService.getUserProfile(authentication.getName());
    }

    @GetMapping("/me")
    public AuthUserResponse me(Authentication authentication) {
        return authService.getUserProfile(authentication.getName());
    }

    @PostMapping("/change-password")
    public AuthUserResponse changePassword(@RequestBody ChangePasswordRequest request, Authentication authentication) {
        if (request == null || request.getNewPassword() == null || request.getNewPassword().trim().length() < 4) {
            throw new IllegalArgumentException("새 비밀번호는 4자 이상이어야 합니다.");
        }

        authService.changePassword(authentication.getName(), request.getNewPassword().trim());
        return authService.getUserProfile(authentication.getName());
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "로그아웃되었습니다."));
    }
}
