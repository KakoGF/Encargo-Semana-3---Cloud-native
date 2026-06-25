package cl.duoc.guias.config;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Configuration
    @ConditionalOnProperty(name = "app.security.enabled", havingValue = "false")
    static class SeguridadDesactivadaConfig {

        @Bean
        SecurityFilterChain cadenaAbierta(HttpSecurity http) throws Exception {
            http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @Configuration
    @ConditionalOnProperty(name = "app.security.enabled", havingValue = "true", matchIfMissing = true)
    static class SeguridadJwtConfig {

        @Value("${app.security.jwks-uri}")
        private String jwksUri;

        @Value("${app.security.issuer}")
        private String issuer;

        @Value("${app.security.roles-claim}")
        private String rolesClaim;

        @Value("${app.security.role-descarga}")
        private String roleDescarga;

        @Value("${app.security.role-gestor}")
        private String roleGestor;

        @Bean
        SecurityFilterChain cadenaSegura(HttpSecurity http) throws Exception {
            http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.GET, "/guias/descargar").hasRole(roleDescarga)
                    .requestMatchers("/guias", "/guias/**").hasRole(roleGestor)
                    .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(convertidorJwt())));
            return http.build();
        }

        @Bean
        JwtDecoder jwtDecoder() {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
            OAuth2TokenValidator<Jwt> validadorIssuer = JwtValidators.createDefaultWithIssuer(issuer);
            OAuth2TokenValidator<Jwt> validador = new DelegatingOAuth2TokenValidator<>(validadorIssuer);
            decoder.setJwtValidator(validador);
            return decoder;
        }

        private JwtAuthenticationConverter convertidorJwt() {
            JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
            convertidor.setJwtGrantedAuthoritiesConverter(this::extraerRoles);
            return convertidor;
        }

        private Collection<GrantedAuthority> extraerRoles(Jwt jwt) {
            Collection<GrantedAuthority> autoridades = new ArrayList<>();
            Object valor = jwt.getClaim(rolesClaim);
            if (valor instanceof String texto) {
                agregarRol(autoridades, texto);
            } else if (valor instanceof Collection<?> lista) {
                for (Object elemento : lista) {
                    if (elemento != null) {
                        agregarRol(autoridades, elemento.toString());
                    }
                }
            }
            return autoridades;
        }

        private void agregarRol(Collection<GrantedAuthority> autoridades, String rol) {
            if (rol != null && !rol.isBlank()) {
                autoridades.add(new SimpleGrantedAuthority("ROLE_" + rol.trim()));
            }
        }
    }
}
