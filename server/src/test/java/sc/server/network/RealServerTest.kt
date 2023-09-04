package sc.server.network

import io.kotest.assertions.timing.eventually
import io.kotest.assertions.until.fibonacci
import io.kotest.assertions.withClue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import sc.api.plugins.IGamePlugin
import sc.networking.clients.LobbyClient
import sc.server.Configuration
import sc.server.Lobby
import sc.server.gaming.GameRoomManager
import sc.server.helpers.TestHelper
import sc.server.plugins.TestPlugin
import java.io.IOException
import java.net.Socket
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

abstract class RealServerTest {
    protected lateinit var lobby: Lobby
    protected lateinit var clientMgr: ClientManager
    protected lateinit var gameMgr: GameRoomManager
    
    protected val serverPort: Int
        get() = NewClientListener.lastUsedPort
    
    fun connectPlayer() =
            LobbyClient("localhost", serverPort).apply { start() }
    
    fun await(clue: String? = null, time: Duration = 1.seconds, f: () -> Boolean) = runBlocking {
        withClue(clue) {
            eventually(time, 20.milliseconds.fibonacci(), predicate = { f() }) {}
        }
    }
    
    @BeforeEach
    fun setup() {
        // Random PortAllocation
        Configuration.set(Configuration.PORT_KEY, "0")
        Configuration.set(Configuration.PASSWORD_KEY, "TEST_PASSWORD")
        this.lobby = Lobby()
        this.clientMgr = this.lobby.clientManager
        this.gameMgr = this.lobby
        
        IGamePlugin.loadPlugin(TestPlugin.TEST_PLUGIN_UUID)
        
        NewClientListener.lastUsedPort = 0
        this.lobby.start()
        waitForServer()
    }
    
    @AfterEach
    fun tearDown() {
        this.lobby.close()
    }
    
    private fun waitForServer() {
        while (NewClientListener.lastUsedPort == 0) {
            Thread.yield()
        }
    }
    
    protected fun waitForConnect(count: Int) =
            TestHelper.assertEqualsWithTimeout(count, { lobby.clientManager.clients.size }, 1, TimeUnit.SECONDS)
    
    protected fun connectClient(): TestTcpClient {
        return try {
            if (serverPort == 0)
                throw RuntimeException("Could not find an open port to connect to.")
            val mySocket = Socket("localhost", NewClientListener.lastUsedPort)
            TestTcpClient(mySocket).apply { start() }
        } catch (e: IOException) {
            e.printStackTrace()
            Assertions.fail("Could not connect to server.", e)
        }
    }
    
}
