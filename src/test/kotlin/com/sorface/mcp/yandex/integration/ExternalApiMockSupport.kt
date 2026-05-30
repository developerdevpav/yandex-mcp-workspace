package com.sorface.mcp.yandex.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.sorface.mcp.yandex.auth.domain.TokenSet
import org.springframework.test.context.DynamicPropertyRegistry
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

/**
 * Общая инфраструктура интеграционных тестов: WireMock для внешних API и файл токенов.
 *
 * Внешние интеграции (Tracker, Wiki) изолированы локальными WireMock-серверами. Реальные
 * HTTP-запросы к api.tracker.yandex.net и api.wiki.yandex.net не выполняются.
 */
object ExternalApiMockSupport {

    const val ACCESS_TOKEN = "test-access-token"
    const val ORG_ID = "12345"
    const val ORG_HEADER = "X-Org-ID"

    val trackerServer: WireMockServer = WireMockServer(options().dynamicPort())
    val wikiServer: WireMockServer = WireMockServer(options().dynamicPort())

    private val objectMapper = ObjectMapper().registerModule(JavaTimeModule())
    private lateinit var tokenFile: Path

    init {
        trackerServer.start()
        wikiServer.start()
        tokenFile = Files.createTempFile("yandex-mcp-integration-tokens", ".json")
        seedTokenFile(tokenFile)
        Runtime.getRuntime().addShutdownHook(Thread { shutdown() })
    }

    fun registerProperties(registry: DynamicPropertyRegistry) {
        registry.add("yandex.tracker.base-url") { trackerServer.baseUrl() }
        registry.add("yandex.wiki.base-url") { wikiServer.baseUrl() }
        registry.add("yandex.oauth.base-url") { trackerServer.baseUrl() }
        registry.add("yandex.client-id") { "integration-client-id" }
        registry.add("yandex.client-secret") { "integration-client-secret" }
        registry.add("yandex.org-id") { ORG_ID }
        registry.add("yandex.org-type") { "YANDEX_360" }
        registry.add("yandex.token-store-path") { tokenFile.toString() }
        registry.add("yandex.read-only") { "false" }
        registry.add("yandex.retry.enabled") { "false" }
    }

    fun resetServers() {
        trackerServer.resetAll()
        wikiServer.resetAll()
    }

    fun shutdown() {
        trackerServer.stop()
        wikiServer.stop()
        Files.deleteIfExists(tokenFile)
    }

    private fun seedTokenFile(path: Path) {
        val tokenSet = TokenSet(
            accessToken = ACCESS_TOKEN,
            refreshToken = "integration-refresh-token",
            tokenType = "OAuth",
            expiresAt = Instant.now().plusSeconds(3600),
        )
        Files.createDirectories(path.parent)
        objectMapper.writeValue(path.toFile(), tokenSet)
    }
}
