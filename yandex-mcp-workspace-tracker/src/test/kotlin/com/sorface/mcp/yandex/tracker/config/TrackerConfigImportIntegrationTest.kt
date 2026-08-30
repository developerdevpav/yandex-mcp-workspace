package com.sorface.mcp.yandex.tracker.config

import com.sorface.mcp.yandex.config.OrgType
import com.sorface.mcp.yandex.config.YandexProperties
import com.sorface.mcp.yandex.tracker.YandexTrackerMcpApplication
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.boot.builder.SpringApplicationBuilder
import java.nio.file.Files
import java.nio.file.Path

@DisplayName("Импорт сохранённых настроек Tracker")
class TrackerConfigImportIntegrationTest {

    @Test
    @DisplayName("config.properties имеет приоритет над встроенными значениями application.yml")
    fun `saved settings override packaged defaults`(@TempDir temporaryDirectory: Path) {
        val configPath = temporaryDirectory.resolve("config.properties")
        Files.writeString(
            configPath,
            """
            yandex.client-id=profile-client
            yandex.client-secret=profile-secret
            yandex.org-id=profile-org
            yandex.org-type=YANDEX_CLOUD
            yandex.read-only=true
            """.trimIndent(),
        )

        SpringApplicationBuilder(YandexTrackerMcpApplication::class.java)
            .run(
                "--YANDEX_CONFIG_PATH=${configPath.toAbsolutePath()}",
                "--spring.ai.mcp.server.enabled=false",
                "--spring.ai.mcp.server.stdio=false",
                "--logging.level.root=OFF",
            ).use { context ->
                val properties = context.getBean(YandexProperties::class.java)

                assertThat(properties.clientId).isEqualTo("profile-client")
                assertThat(properties.clientSecret).isEqualTo("profile-secret")
                assertThat(properties.orgId).isEqualTo("profile-org")
                assertThat(properties.orgType).isEqualTo(OrgType.YANDEX_CLOUD)
                assertThat(properties.readOnly).isTrue()
            }
    }
}
