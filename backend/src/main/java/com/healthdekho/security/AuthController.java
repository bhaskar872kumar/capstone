package com.healthdekho.security;

import com.healthdekho.common.error.UnauthorizedException;
import com.healthdekho.customer.CustomerRepository;
import com.healthdekho.security.dto.LoginRequest;
import com.healthdekho.security.dto.SessionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-1 A: customer login. Logout is handled by Spring Security at POST /api/auth/logout (see SecurityConfig).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final String INVALID_CREDENTIALS = "Invalid email or password";

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final CustomerRepository customerRepository;
    private final CurrentCustomer currentCustomer;

    public AuthController(AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository,
                          CustomerRepository customerRepository,
                          CurrentCustomer currentCustomer) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.customerRepository = customerRepository;
        this.currentCustomer = currentCustomer;
    }

    @PostMapping("/login")
    public SessionResponse login(@Valid @RequestBody LoginRequest body,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(body.email().trim(), body.password()));
        } catch (AuthenticationException ex) {
            // Same message for unknown email and wrong password, so accounts can't be discovered.
            log.warn("Login failed: {}", ex.getClass().getSimpleName());
            throw new UnauthorizedException("INVALID_CREDENTIALS", INVALID_CREDENTIALS);
        }

        // New session id on login, so a session id known before login can't be reused (session fixation).
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        return customerRepository.findByEmailIgnoreCase(authentication.getName())
                .map(customer -> {
                    log.info("Login succeeded customerId={}", customer.getId());
                    return SessionResponse.loggedIn(customer);
                })
                .orElseThrow(() -> new UnauthorizedException("INVALID_CREDENTIALS", INVALID_CREDENTIALS));
    }

    /** Lets the React app check for an existing session on page load. Also sets the CSRF cookie. */
    @GetMapping("/session")
    public SessionResponse session() {
        return currentCustomer.find()
                .map(SessionResponse::loggedIn)
                .orElseGet(SessionResponse::loggedOut);
    }
}
