package com.energypal.auth;

import com.energypal.common.event.EventEnvelope;
import com.energypal.common.event.EventPublisher;
import com.energypal.common.event.TopicNames;
import com.energypal.common.web.ApiResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import static org.springframework.security.config.Customizer.withDefaults;

@org.springframework.boot.autoconfigure.SpringBootApplication(scanBasePackages = "com.energypal")
public class AuthServiceApplication {
    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(AuthServiceApplication.class, args);
    }
}

@EnableWebSecurity
@org.springframework.context.annotation.Configuration
class SecurityConfig {
    @Bean
    WebSecurityCustomizer authApiWebSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(ant("/api/auth/**"));
    }

    @Bean
    @Order(0)
    SecurityFilterChain publicAuthSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(ant("/api/auth/**"))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    @Bean
    @Order(1)
    SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        var authorizationServerConfigurer = new OAuth2AuthorizationServerConfigurer();
        var endpointsMatcher = authorizationServerConfigurer.getEndpointsMatcher();
        http
                .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                ant("/api/auth/**"),
                                ant("/actuator/health"),
                                ant("/v3/api-docs/**"),
                                ant("/swagger-ui/**"),
                                ant("/swagger-ui.html")
                        ).permitAll()
                        .anyRequest().authenticated())
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()));
        http.apply(authorizationServerConfigurer);
        authorizationServerConfigurer.oidc(withDefaults());
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable).cors(withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                ant("/api/auth/**"),
                                ant("/actuator/health"),
                                ant("/v3/api-docs/**"),
                                ant("/swagger-ui/**"),
                                ant("/swagger-ui.html")
                        ).permitAll()
                        .anyRequest().authenticated())
                .formLogin(withDefaults())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()))
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    RegisteredClientRepository registeredClientRepository(PasswordEncoder passwordEncoder) {
        var client = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("energypal-client")
                .clientSecret(passwordEncoder.encode("energypal-secret"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://127.0.0.1:8080/login/oauth2/code/energypal-client")
                .scope(OidcScopes.OPENID)
                .scope("energypal.read")
                .scope("energypal.write")
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .refreshTokenTimeToLive(Duration.ofDays(1))
                        .build())
                .build();
        return new InMemoryRegisteredClientRepository(client);
    }

    @Bean
    JWKSource<SecurityContext> jwkSource() {
        var keyPair = generateRsaKey();
        var publicKey = (RSAPublicKey) keyPair.getPublic();
        var privateKey = (RSAPrivateKey) keyPair.getPrivate();
        var rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    @Bean
    JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    AuthorizationServerSettings authorizationServerSettings(@Value("${energypal.oauth2.issuer:http://localhost:8081}") String issuer) {
        return AuthorizationServerSettings.builder()
                .issuer(issuer)
                .build();
    }

    private KeyPair generateRsaKey() {
        try {
            var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate OAuth2 signing key", exception);
        }
    }

    private AntPathRequestMatcher ant(String pattern) {
        return new AntPathRequestMatcher(pattern);
    }
}

@RestController
@RequestMapping("/api/auth")
class AuthController {
    private final Map<String, UserAccount> users = new ConcurrentHashMap<>();
    private final PasswordEncoder passwordEncoder;
    private final EventPublisher eventPublisher;
    private final SecretKey jwtKey = Keys.hmacShaKeyFor("EnergyPalJwtSecretKeyForDemoOnlyChangeMe".getBytes(StandardCharsets.UTF_8));

    AuthController(PasswordEncoder passwordEncoder, EventPublisher eventPublisher) {
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping("/register")
    ResponseEntity<ApiResponse<AuthResult>> register(@RequestBody RegisterRequest request) {
        var id = UUID.randomUUID().toString();
        var account = new UserAccount(id, request.email(), passwordEncoder.encode(request.password()), request.role());
        users.put(request.email(), account);
        eventPublisher.publish(TopicNames.CUSTOMER_EVENTS, EventEnvelope.of("CustomerCreated", "auth-service", id,
                Map.of("userId", id, "email", request.email(), "role", request.role())));
        return ResponseEntity.ok(ApiResponse.ok(tokenFor(account)));
    }

    @PostMapping("/login")
    ResponseEntity<ApiResponse<AuthResult>> login(@RequestBody LoginRequest request) {
        var account = Optional.ofNullable(users.get(request.email()))
                .filter(user -> passwordEncoder.matches(request.password(), user.passwordHash()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        return ResponseEntity.ok(ApiResponse.ok(tokenFor(account)));
    }

    private AuthResult tokenFor(UserAccount account) {
        var token = Jwts.builder()
                .subject(account.email())
                .claim("userId", account.id())
                .claim("role", account.role())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(jwtKey)
                .compact();
        return new AuthResult(account.id(), token, account.role());
    }
}

record RegisterRequest(String email, String password, String role) {}
record LoginRequest(String email, String password) {}
record AuthResult(String userId, String accessToken, String role) {}
record UserAccount(String id, String email, String passwordHash, String role) {}
