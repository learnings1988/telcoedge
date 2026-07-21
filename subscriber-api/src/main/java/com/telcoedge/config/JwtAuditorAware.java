package com.telcoedge.config;


import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("auditorAware")
public class JwtAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor(){
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication :: isAuthenticated)
                .map(Authentication :: getPrincipal)
                .filter(Jwt.class :: isInstance)
                .map(Jwt.class :: cast)
                .map(Jwt :: getSubject);
    }
}
