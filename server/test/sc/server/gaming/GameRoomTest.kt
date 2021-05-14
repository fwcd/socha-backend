package sc.server.gaming

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.assertThrows
import sc.protocol.requests.PrepareGameRequest
import sc.server.helpers.StringNetworkInterface
import sc.server.network.Client
import sc.server.plugins.TestPlugin
import sc.shared.PlayerScore
import sc.shared.ScoreCause
import sc.shared.SlotDescriptor

class GameRoomTest: WordSpec({
    isolationMode = IsolationMode.SingleInstance
    val client = Client(StringNetworkInterface("")).apply { start() }
    "A GameRoomManager" should {
        val manager = GameRoomManager().apply { pluginManager.loadPlugin(TestPlugin::class.java) }
        // TODO Replay observing
        // Configuration.set(Configuration.SAVE_REPLAY, "true")
        "create a game when a player joins" {
            manager.joinOrCreateGame(client, TestPlugin.TEST_PLUGIN_UUID).playerCount shouldBe 1
            manager.games shouldHaveSize 1
        }
        val room = manager.games.single()
        "add a second player to the existing game" {
            manager.joinOrCreateGame(client, TestPlugin.TEST_PLUGIN_UUID).playerCount shouldBe 2
        }
        "return correct scores on game over" {
            val playersScores = room.game.players.associateWith { PlayerScore(ScoreCause.REGULAR, "Game terminated", 0) }
            room.onGameOver(playersScores)
            room.result.isRegular shouldBe true
            room.result.scores shouldContainExactly playersScores.values
            room.isOver shouldBe true
        }
    }
    "A GameRoom with prepared reservations" should {
        val manager = GameRoomManager().apply { pluginManager.loadPlugin(TestPlugin::class.java) }
        val player2name = "opponent"
        
        val reservations = manager.prepareGame(PrepareGameRequest(TestPlugin.TEST_PLUGIN_UUID, descriptor2 = SlotDescriptor(player2name))).reservations
        manager.games shouldHaveSize 1
        val room = manager.games.single()
        room.clients shouldHaveSize 0
        "reject a client with wrong or no reservation" {
            assertThrows<UnknownReservationException> {
                ReservationManager.redeemReservationCode(client, "nope")
            }
            room.join(client) shouldBe false
            room.clients shouldHaveSize 0
        }
        "join a client with reservation" {
            ReservationManager.redeemReservationCode(client, reservations[0])
            room.clients shouldHaveSize 1
        }
        "not accept a reservation twice" {
            assertThrows<UnknownReservationException> {
                ReservationManager.redeemReservationCode(client, reservations[0])
            }
            room.clients shouldHaveSize 1
        }
        "accept a second client and create Players" {
            ReservationManager.redeemReservationCode(client, reservations[1])
            room.clients shouldHaveSize 2
        }
        "reject a third client" {
            room.join(client) shouldBe false
            room.clients shouldHaveSize 2
        }
        "have properly named players" {
            room.game.players[0].displayName shouldBe "Player1"
            room.game.players[1].displayName shouldBe player2name
        }
    }
})
