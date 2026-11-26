package ro.greentween.copaculmeu.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import ro.greentween.copaculmeu.security.JwtAuthenticationFilter;

import java.time.Instant;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"timestamp\":\"" + Instant.now() + "\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Authentication is required to access this resource\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"timestamp\":\"" + Instant.now() + "\",\"status\":403,\"error\":\"Forbidden\",\"message\":\"Access denied: Administrator privileges required\"}");
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        // All admin endpoints under /api/admin/** require ADMIN role
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Species endpoints: reading is public, creating species requires ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/species/**", "/api/admin/species/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/species/**").permitAll()
                        // Tree direct creation, proposals management, and review actions require ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/trees", "/api/admin/trees").hasRole("ADMIN")
                        .requestMatchers("/api/trees/proposals/**", "/api/admin/trees/proposals/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/trees/*/approve", "/api/admin/trees/*/approve").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/trees/*/reject", "/api/admin/trees/*/reject").hasRole("ADMIN")
                        // Proposing trees requires authenticated user
                        .requestMatchers(HttpMethod.POST, "/api/trees/propose").authenticated()
                        // Public tree reading endpoints
                        .requestMatchers(HttpMethod.GET, "/api/trees/**").permitAll()
                        // Admin user management endpoints
                        .requestMatchers(HttpMethod.GET, "/api/users", "/api/users/", "/api/users/paged", "/api/admin/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/admin/users/*", "/api/users/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/admin/users/*", "/api/users/*").hasRole("ADMIN")
                        .requestMatchers("/api/users/*/approve", "/api/users/*/confirm", "/api/admin/users/*/approve", "/api/admin/users/*/confirm", "/api/users/*/status", "/api/admin/users/*/status").hasRole("ADMIN")
                        // Admin email template management endpoints
                        .requestMatchers("/api/admin/email-templates/**", "/api/email-templates/**").hasRole("ADMIN")
                        // Public auth, user registration, and documentation endpoints
                        .requestMatchers("/api/auth/**", "/api/users/**").permitAll()
                        .requestMatchers("/scalar/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public org.springframework.boot.web.servlet.FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(
            JwtAuthenticationFilter filter) {
        org.springframework.boot.web.servlet.FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration());
        return source;
    }

    static CorsConfiguration corsConfiguration() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:8080",
                "http://127.0.0.1:8080",
                "https://copacul-meu.greentwin.ro"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "Location", "Set-Cookie"));
        configuration.setAllowCredentials(true);
        return configuration;
    }
}
