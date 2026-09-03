package br.ufpb.iago.backend.config;

import br.ufpb.iago.backend.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    // Injetamos o bean do CorsConfig que você criou
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Habilita o CORS usando as regras do seu CorsConfig
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Infraestrutura
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Autenticação: apenas register, login e refresh são públicos
                        .requestMatchers("/api/v1/auth/register").permitAll()
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/auth/refresh").permitAll()

                        // Attractions: leitura pública, escrita restrita a GUIDE
                        .requestMatchers(HttpMethod.GET, "/api/v1/attractions/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/attractions/**").hasRole("GUIDE")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/attractions/**").hasRole("GUIDE")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/attractions/**").hasRole("GUIDE")

                        // Reservations: confirm/complete é do GUIDE, o resto é do TOURIST
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/reservations/*/confirm").hasRole("GUIDE")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/reservations/*/complete").hasRole("GUIDE")
                        .requestMatchers(HttpMethod.GET, "/api/v1/reservations/guide").hasRole("GUIDE")
                        .requestMatchers("/api/v1/reservations/**").hasRole("TOURIST")

                        // Reviews: leitura pública, escrita restrita a TOURIST
                        .requestMatchers(HttpMethod.GET, "/api/v1/reviews/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/reviews/**").hasRole("TOURIST")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/reviews/**").hasRole("TOURIST")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/reviews/**").hasRole("TOURIST")

                        // Users: administração restrita
                        .requestMatchers("/api/v1/users/**").hasRole("ADMIN")

                        // Qualquer outra rota exige autenticação
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}