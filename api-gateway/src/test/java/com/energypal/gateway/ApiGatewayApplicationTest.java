package com.energypal.gateway;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiGatewayApplicationTest {
    @Test
    void applicationClassExists() {
        assertThat(ApiGatewayApplication.class.getPackageName()).isEqualTo("com.energypal.gateway");
    }
}
