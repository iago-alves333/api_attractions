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

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Rotas públicas de infraestrutura
                        .requestMatchers("/error").permitAll()

                        // Autenticação
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/auth/register").permitAll()

                        // Atrações: leitura pública, escrita restrita a GUIDE
                        .requestMatchers(HttpMethod.GET, "/api/v1/attractions/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/attractions").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/attractions/**").hasRole("GUIDE")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/attractions/**").hasRole("GUIDE")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/attractions/**").hasRole("GUIDE")
                        .requestMatchers(HttpMethod.POST, "/api/v1/create/attraction").hasRole("GUIDE")

                        // Reservas: só turistas
                        .requestMatchers("/api/v1/reservations/**").hasRole("TOURIST")
                        .requestMatchers("/api/v1/reservations").hasRole("TOURIST")

                        // Reviews: só turistas podem criar/editar; leitura pode ser pública
                        .requestMatchers(HttpMethod.GET, "/api/v1/reviews/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/reviews/**").hasRole("TOURIST")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/reviews/**").hasRole("TOURIST")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/reviews/**").hasRole("TOURIST")

                        // Usuários: administração restrita
                        .requestMatchers("/api/v1/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/users").hasRole("ADMIN")

                        // Qualquer outra rota exige autenticação
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}