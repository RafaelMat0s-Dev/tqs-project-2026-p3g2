package pt.marazul.identity.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import pt.marazul.identity.application.JwtService;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {
    static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/login", "/error",
            "/actuator/health", "/actuator/health/**", "/actuator/prometheus",
            "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**",
            "/gbfs/**"
    };

    static final String[] GUEST_ENDPOINTS_UNTIL_GUEST_LOGIN = {
            "/api/bookings", "/api/bookings/**", "/api/trips", "/api/trips/**", "/api/telemetry/**"
    };

    @Bean
    PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    UserDetailsService emptyDefaultUserDetailsService() {
        return new InMemoryUserDetailsManager();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwt, ObjectMapper objectMapper) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(GUEST_ENDPOINTS_UNTIL_GUEST_LOGIN).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> writeProblem(
                                objectMapper, response, HttpStatus.UNAUTHORIZED, "Não autenticado",
                                "É necessário autenticar-se para aceder a este recurso.",
                                "urn:marazul:erro:nao-autenticado", request.getRequestURI()))
                        .accessDeniedHandler((request, response, exception) -> writeProblem(
                                objectMapper, response, HttpStatus.FORBIDDEN, "Acesso negado",
                                "O utilizador autenticado não tem permissão para aceder a este recurso.",
                                "urn:marazul:erro:acesso-negado", request.getRequestURI())))
                .addFilterBefore(new JwtAuthenticationFilter(jwt), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static void writeProblem(ObjectMapper objectMapper, jakarta.servlet.http.HttpServletResponse response,
                                     HttpStatus status, String title, String detail, String type, String instance)
            throws java.io.IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(type));
        problem.setTitle(title);
        problem.setInstance(URI.create(instance));
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
