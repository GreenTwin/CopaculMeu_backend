package ro.greentween.copaculmeu.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionCorsTest {

    private final CorsConfiguration configuration = SecurityConfig.corsConfiguration();

    @Test
    void productionOrigin_isAllowed() {
        assertThat(configuration.checkOrigin("https://copacul-meu.greentwin.ro"))
                .isEqualTo("https://copacul-meu.greentwin.ro");
    }

    @Test
    void localhostOrigin_isAllowed() {
        assertThat(configuration.checkOrigin("http://localhost:5173"))
                .isEqualTo("http://localhost:5173");
    }

    @Test
    void unknownOrigin_isRejected() {
        assertThat(configuration.checkOrigin("https://evil.example")).isNull();
    }
}
