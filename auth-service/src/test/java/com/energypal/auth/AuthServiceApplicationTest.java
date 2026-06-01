package com.energypal.auth;

import com.energypal.common.event.EventEnvelope;
import com.energypal.common.event.EventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceApplicationTest {
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final EventPublisher eventPublisher = mock();
    private final AuthController controller = new AuthController(passwordEncoder, eventPublisher);

    @Test
    void registersAndLogsInUser() {
        var register = controller.register(new RegisterRequest("asha@example.com", "secret", "CUSTOMER")).getBody().data();
        var login = controller.login(new LoginRequest("asha@example.com", "secret")).getBody().data();

        assertThat(register.userId()).isNotBlank();
        assertThat(register.role()).isEqualTo("CUSTOMER");
        assertThat(register.accessToken()).isNotBlank();
        assertThat(login.userId()).isEqualTo(register.userId());
        assertThat(login.accessToken()).isNotBlank();
        assertThat(login.role()).isEqualTo("CUSTOMER");
        verify(eventPublisher).publish(eq("customer.events"), argThat(event -> "CustomerCreated".equals(event.eventType())));
    }

    @Test
    void rejectsInvalidCredentials() {
        controller.register(new RegisterRequest("asha@example.com", "secret", "CUSTOMER"));

        assertThatThrownBy(() -> controller.login(new LoginRequest("asha@example.com", "bad")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void createsPasswordEncoder() {
        var encoder = new SecurityConfig().passwordEncoder();

        assertThat(encoder.matches("secret", encoder.encode("secret"))).isTrue();
    }
}
