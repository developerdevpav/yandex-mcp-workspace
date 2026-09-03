package com.sorface.mcp.yandex.client.api

import com.sorface.mcp.yandex.auth.api.AuthSetupPrompter
import com.sorface.mcp.yandex.client.application.ClientAvailability
import com.sorface.mcp.yandex.client.application.ClientConnectionResult
import com.sorface.mcp.yandex.client.application.ClientConnector
import com.sorface.mcp.yandex.client.application.ClientScope
import com.sorface.mcp.yandex.client.application.ClientTarget
import com.sorface.mcp.yandex.client.application.McpServerCommand
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.ApplicationArguments

@DisplayName("Интерактивная команда подключения MCP-клиентов")
class ClientConnectCommandRunnerTest {

    @Test
    @DisplayName("Enter выбирает всех найденных клиентов и пользовательский уровень по умолчанию")
    fun `empty answers select all available clients and user scope`() {
        val connector = mockk<ClientConnector>()
        val arguments = mockk<ApplicationArguments>()
        val answers = ArrayDeque(listOf("", "", ""))
        val prompter = object : AuthSetupPrompter {
            override fun read(prompt: String, secret: Boolean): String = answers.removeFirst()
        }
        val availability = listOf(
            ClientAvailability(ClientTarget.CODEX, true, "обнаружен"),
            ClientAvailability(ClientTarget.CLAUDE_CODE, false, "не найден"),
            ClientAvailability(ClientTarget.CLAUDE_DESKTOP, true, "обнаружен"),
            ClientAvailability(ClientTarget.CURSOR, true, "обнаружен"),
        )
        every { arguments.nonOptionArgs } returns listOf("connect")
        every { arguments.getOptionValues("tracker-command") } returns listOf("/stable/tracker")
        every { arguments.getOptionValues("wiki-command") } returns null
        every { connector.detect() } returns availability
        every { connector.connect(any(), any(), any(), any()) } answers {
            ClientConnectionResult(firstArg(), true, "готово")
        }

        ClientConnectCommandRunner(connector, prompter).run(arguments)

        verify(exactly = 1) { connector.connect(ClientTarget.CODEX, ClientScope.USER, any(), any()) }
        verify(exactly = 1) { connector.connect(ClientTarget.CLAUDE_DESKTOP, ClientScope.USER, any(), any()) }
        verify(exactly = 1) { connector.connect(ClientTarget.CURSOR, ClientScope.USER, any(), any()) }
        verify(exactly = 0) { connector.connect(ClientTarget.CLAUDE_CODE, any(), any(), any()) }
    }
}
