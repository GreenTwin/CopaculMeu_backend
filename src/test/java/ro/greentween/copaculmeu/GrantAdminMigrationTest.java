package ro.greentween.copaculmeu;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class GrantAdminMigrationTest {

    @Test
    void migrationPromotesOnlyTheNamedAccount() throws Exception {
        try (var in = getClass().getResourceAsStream("/db/migration/V12__grant_admin_to_sorin_tamas.sql")) {
            assertThat(in).isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).toLowerCase();

            assertThat(sql).contains("update users");
            assertThat(sql).contains("set role = 'admin'");
            assertThat(sql).contains("where lower(email) = 'tamas.sorin90@gmail.com'");
            assertThat(sql).doesNotContain("status");
        }
    }
}
