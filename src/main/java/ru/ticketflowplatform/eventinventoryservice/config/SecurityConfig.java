package ru.ticketflowplatform.eventinventoryservice.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import ru.ticketflowplatform.eventinventoryservice.controller.SecurityExceptionHandler;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String ROLE_PREFIX = "ROLE_%s";
    private static final String ROLES_FIELD = "roles";
    private static final String REALM_ACCESS_CLAIM = "realm_access";

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        @Value("${api.urls.health}") String healthPath,
        @Value("${api.urls.swagger-ui}") String swaggerUiPath,
        @Value("${api.urls.swagger-ui-entry}") String swaggerUiEntryPath,
        @Value("${api.urls.openapi-docs}") String openApiDocsPath,
        SecurityExceptionHandler errorHandlers) {
        return http.csrf(AbstractHttpConfigurer::disable)
            .exceptionHandling(
                exceptions -> exceptions
                    .authenticationEntryPoint(errorHandlers)
                    .accessDeniedHandler(errorHandlers)
            )
            .authorizeHttpRequests(
                auth -> auth
                    .requestMatchers(healthPath, swaggerUiPath, swaggerUiEntryPath, openApiDocsPath).hasRole("DEVELOPER")
                    .anyRequest().authenticated()
            )
            .oauth2ResourceServer(
                oauth2 -> oauth2
                    .authenticationEntryPoint(errorHandlers)
                    .accessDeniedHandler(errorHandlers)
                    .jwt(
                        jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                    )
            )
            .build();
    }

    @Bean
    JwtDecoder jwtDecoder(
        @Value("${security.jwt.jwk-set-uri}") String jwkSetUri,
        @Value("${security.jwt.issuer}") String issuer,
        @Value("${security.jwt.audience}") String audience,
        MessageSource messageSource) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuer);

        OAuth2TokenValidator<Jwt> audienceValidator = getAudienceValidator(
            audience,
            messageSource.getMessage(
                "security.jwt.invalid_audience.error_description",
                new Object[0],
                Locale.ENGLISH
            )
        );

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));
        return decoder;
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtAuthoritiesConverter());
        return converter;
    }

    private OAuth2TokenValidator<Jwt> getAudienceValidator(String audience, String errorDescription) {
        return token -> token.getAudience().contains(audience)
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(
                new OAuth2Error(
                    "invalid_token",
                    errorDescription,
                    null
                )
            );
    }

    private Converter<Jwt, Collection<GrantedAuthority>> jwtAuthoritiesConverter() {
        return jwt -> {
            ArrayList<GrantedAuthority> authorities = new ArrayList<>();
            Map<String, Object> realmAccess = jwt.getClaimAsMap(REALM_ACCESS_CLAIM);
            List<GrantedAuthority> enriched = enrichAuthority(authorities, realmAccess);
            return enriched;
        };
    }

    private List<GrantedAuthority> enrichAuthority(List<GrantedAuthority> authorities, Map<String, Object> realmAccess) {
        if (realmAccess != null && realmAccess.get(ROLES_FIELD) instanceof Collection<?> roles) {
            roles.stream().map(Object::toString)
                .map(role -> new SimpleGrantedAuthority(String.format(ROLE_PREFIX, role)))
                .forEach(authorities::add);
        }

        return authorities;
    }
}
