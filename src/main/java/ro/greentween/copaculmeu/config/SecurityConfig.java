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
                        // Species endpoints: reading is public, creating species requires ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/species/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/species/**").permitAll()
                        // Tree direct creation, proposals management, and review actions require ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/trees").hasRole("ADMIN")
                        .requestMatchers("/api/trees/proposals/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/trees/*/approve").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/trees/*/reject").hasRole("ADMIN")
                        // Proposing trees requires authenticated user
                        .requestMatchers(HttpMethod.POST, "/api/trees/propose").authenticated()
                        // Public tree reading endpoints
                        .requestMatchers(HttpMethod.GET, "/api/trees/**").permitAll()
                        // Public auth, user registration, and documentation endpoints
                        .requestMatchers("/api/auth/**", "/api/users/**").permitAll()
                        .requestMatchers("/scalar/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
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
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
