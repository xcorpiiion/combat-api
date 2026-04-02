package br.com.study.combatapi.config;

import br.com.study.genericauthorization.configuration.AbstractMicroserviceSecurityConfig;
import br.com.study.genericauthorization.configuration.SecurityRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

@Configuration
@EnableMethodSecurity
public class SecurityConfig extends AbstractMicroserviceSecurityConfig {

    @Override
    protected void configurePublicEndpoints(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>
                    .AuthorizationManagerRequestMatcherRegistry auth
    ) {
        auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll();
    }

    @Override
    protected void configureProtectedEndpoints(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>
                    .AuthorizationManagerRequestMatcherRegistry auth
    ) {
    }

    @Bean
    @Override
    public SecurityRegistryCustomizer securityRegistryCustomizer() {
        return auth -> {
            configurePublicEndpoints(auth);
            configureProtectedEndpoints(auth);
        };
    }
}