package ro.greentween.copaculmeu.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import ro.greentween.copaculmeu.data.dao.UserDao;
import ro.greentween.copaculmeu.data.enums.UserStatus;
import ro.greentween.copaculmeu.data.model.User;
import ro.greentween.copaculmeu.service.JwtService;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDao userDao;

    public JwtAuthenticationFilter(@org.springframework.context.annotation.Lazy JwtService jwtService,
                                   @org.springframework.context.annotation.Lazy UserDao userDao) {
        this.jwtService = jwtService;
        this.userDao = userDao;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = jwtService.extractToken(request);

        if (StringUtils.hasText(token) && jwtService.isTokenValid(token)) {
            Optional<Claims> claimsOpt = jwtService.parseTokenClaims(token);
            if (claimsOpt.isPresent()) {
                Claims claims = claimsOpt.get();
                try {
                    UUID userId = UUID.fromString(claims.getSubject());
                    User user = userDao.find(userId);
                    if (user != null && user.getStatus() != UserStatus.SUSPENDED) {
                        UserPrincipal principal = UserPrincipal.fromEntity(user);
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } catch (Exception e) {
                    log.error("Could not set user authentication in security context: {}", e.getMessage());
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
