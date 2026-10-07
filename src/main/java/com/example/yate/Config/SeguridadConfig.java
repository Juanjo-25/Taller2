package com.example.yate.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SeguridadConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(permisos -> permisos
                .requestMatchers("/css/**", "/js/**", "/img/**", "/error",
                        "/login/ingresar", "/login/registro", "/login/inicializar").permitAll()
                .requestMatchers("/login/pendientes", "/login/activar/**").hasRole("ADMIN")
                .requestMatchers("/clientes/perfil").hasRole("CLIENTE")
                .requestMatchers("/productos", "/productos/**").hasRole("ADMIN")
                .requestMatchers("/clientes", "/clientes/**").hasRole("ADMIN")
                .requestMatchers("/", "/home").authenticated()
                .anyRequest().denyAll())
            .formLogin(login -> login.loginPage("/login/ingresar")
                .loginProcessingUrl("/login/ingresar")
                .usernameParameter("email").passwordParameter("contrasena")
                .defaultSuccessUrl("/", true)
                .failureHandler((solicitud, respuesta, error) -> respuesta.sendRedirect(
                        solicitud.getContextPath() + "/login/ingresar?error="
                        + (error instanceof DisabledException ? "inactiva" : "credenciales")))
                .permitAll())
            .logout(logout -> logout.logoutUrl("/login/salir")
                .logoutSuccessUrl("/login/ingresar?salida").permitAll());
        return http.build();
    }
}
