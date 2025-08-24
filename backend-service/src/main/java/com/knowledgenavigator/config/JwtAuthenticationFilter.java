package com.knowledgenavigator.config;

import com.knowledgenavigator.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            try {
                if (jwtService.isTokenValid(token)) {
                    String username = jwtService.extractUsername(token);
                    
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        username, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    );
                    
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception e) {
                // Invalid token, continue without authentication
            }
        }
        
        filterChain.doFilter(request, response);
    }
}