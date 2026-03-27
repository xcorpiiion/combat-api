package br.com.study.combatapi.config;

import br.com.study.genericauthorization.configuration.AbstractMicroserviceSecurityConfig;
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
        auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll();
    }

    @Override
    protected void configureProtectedEndpoints(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>
                    .AuthorizationManagerRequestMatcherRegistry auth
    ) {
        // Todos os endpoints de batalha exigem autenticação
    }
}