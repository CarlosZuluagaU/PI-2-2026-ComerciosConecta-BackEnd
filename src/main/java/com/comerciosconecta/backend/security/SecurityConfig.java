package com.comerciosconecta.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtRequestFilter jwtRequestFilter;

    public SecurityConfig(UserDetailsServiceImpl userDetailsService, JwtRequestFilter jwtRequestFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtRequestFilter = jwtRequestFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .authenticationProvider(authenticationProvider())
                .build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // ── Auth público ──
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register", "/api/auth/refresh").permitAll()
                // ── Tienda pública — productos ──
                .requestMatchers(HttpMethod.GET, "/api/productos", "/api/productos/**").permitAll()
                // ── Apariencia del comercio (para cargar la tienda) ──
                .requestMatchers(HttpMethod.GET, "/api/comercios/*/apariencia").permitAll()
                // ── Checkout público ──
                .requestMatchers(HttpMethod.POST, "/api/checkout/create-order").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/checkout/create-payment-link/**").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/checkout/orders/by-uuid/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/checkout/orders/by-uuid/**").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/checkout/orders/buscar").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/checkout/orders/*/cancelar").permitAll()
                // ── Webhook Wompi ──
                .requestMatchers(HttpMethod.POST, "/api/wompi/webhook").permitAll()
                // ── Envíos — solo cálculo (checkout) y seguimiento público ──
                .requestMatchers(HttpMethod.POST, "/api/envios/calcular").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/envios/seguimiento/**").permitAll()
                // ── Herramientas de desarrollo ──
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/h2-console/**").permitAll()
                // ── Todo lo demás requiere JWT ──
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
